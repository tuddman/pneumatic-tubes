(ns pneumatic-tubes.core
  (:use [clojure.tools.logging :only [info warn error]]
        [clojure.set])
  (:require [clojure.core.async
             :refer [>! <! >!! <!! go go-loop chan]]))

;; -------- tube-registry-------------------------------------------------------------------

(def ^:private tube-registry (atom {:tubes    {}
                                    :send-fns {}}))

(defn- assoc-tube-data
  "Associate data with the tube on server side, this data can be used to select particular tubes for sending data"
  [registry tube-id new-data]
  (if (map? new-data)
    (assoc-in registry [:tubes tube-id] (merge {:tube/id tube-id} new-data))
    (do
      (error "pneumatic-tubes: Expected map containing new tube data, but got " new-data " . Tube data was not changed")
      registry)))

(defn- add-tube [registry tube-id send-fn initial-data]
  (-> registry
      (assoc-tube-data tube-id initial-data)
      (assoc-in [:send-fns tube-id] send-fn)))

(defn- rm-tube [registry tube-id]
  (-> registry
      (update :tubes dissoc tube-id)
      (update :send-fns dissoc tube-id)))

(defn add-tube!
  "Registers a new tube in a global registry,
  the send-fn is a function which sends a message via implementation-specific channel like a WebSocket"
  ([send-fn]
   (add-tube! send-fn {}))
  ([send-fn client-data]
   (let [tube-id (java.util.UUID/randomUUID)]
     (swap! tube-registry #(add-tube % tube-id send-fn client-data))
     tube-id)))

(defn assoc-tube-data!
  "Associates the some data with the tube.
  This is like putting a sticker with a label on a tube,
  so that you can select the tube by label to send messages to particular destination"
  [tube-id new-data]
  (swap! tube-registry #(assoc-tube-data % tube-id new-data)))

(defn get-tube [id]
  "Returns current tube data from rergistry"
  (get-in @tube-registry [:tubes id]))

(defn rm-tube!
  "Removes tube from the registry"
  [tube-id]
  (swap! tube-registry #(rm-tube % tube-id))
  nil)


;; -------- receiver ----------------------------------------------------------------------

(defn- lookup-handler
  [receiver event-id]
  (get (:event-handlers receiver) event-id))

(defn- handle-incoming
  [receiver {from :from event-v :event}]
  (let [event-id (first event-v)
        tube-id (:tube/id from)
        handler-fn (lookup-handler receiver event-id)]
    (if (nil? handler-fn)
      (error "pneumatic-tubes: no event handler registered for: \"" event-id "\". Ignoring.")
      (try
        (assoc-tube-data! tube-id (handler-fn from event-v))
        (catch Exception e (error "pneumatic-tubes: Exception while processing event:"
                                  event-v "received from tube" from e))))))

(defn- noop-handler [tube _] tube)
(def ^:private noop-handlers {:tube/on-create  noop-handler
                              :tube/on-destroy noop-handler})

(defn receiver
  "Receiver processes all the messages coming to him using the provided handler map.
  Handler map key is an event-id and valye is function to process the event."
  ([handler-map] (receiver (chan 100) handler-map))
  ([in-queue handler-map]
   (let [this {:in-queue       in-queue
               :event-handlers (into noop-handlers handler-map)}]
     (go-loop [event (<! in-queue)]
       (when event
         (handle-incoming this event)
         (recur (<! in-queue))))
     this)))

(defn receive-sync
  "Synchroinously process the incoming event"
  ([receiver from event-v]
   (handle-incoming receiver {:from from :event event-v})))

(defn receive
  "Asynchronously process the incoming event"
  ([receiver from event-v]
   (>!! (:in-queue receiver) {:from from :event event-v})))

(defn wrap-handlers
  "Wraps a map of handlers with one or more middlewares"
  ([handler-map middleware & middlewares]
   (let [kv-pairs (seq handler-map)
         wrapped-map (into {} (map (fn [[k v]] [k (middleware v)]) kv-pairs))]
     (if (empty? middlewares)
       wrapped-map
       (apply wrap-handlers wrapped-map (first middlewares) (rest middlewares))))))

;; -------- transmitter ----------------------------------------------------------------------

(defn dispatch
  "Send event vector to one or more tubes.
  Destination (parameter 'to') can be a map, a predicate function or :all keyword "
  ([transmitter to event-v]
   (>!! (:out-queue transmitter) {:to to :event event-v}) to))

(defn- send-to-tube [tube-registry tube-id event-v]
  (let [send! (get-in tube-registry [:send-fns tube-id])]
    (send! (str event-v))))

(defn- resolve-target-tube-ids [clients crit]
  (let [all-clients (vals clients)]
    (if (= crit :all)
      (map :tube/id all-clients)
      (if (fn? crit)
        (map :tube/id (filter crit all-clients))
        (if-let [tube-id (:tube/id crit)]
          [tube-id]
          [])))))

(defn- handle-outgoing
  [tube-registry {to :to event-v :event}]
  (let [tube-ids (resolve-target-tube-ids (:tubes tube-registry) to)]
    (doseq [tube-id tube-ids]
      (send-to-tube tube-registry tube-id event-v))))

(defn- call-listeners [on-send {to :to event-v :event}]
  (when on-send
    (if (coll? on-send)
      (doseq [on-send-fn on-send]
        (on-send-fn to event-v))
      (on-send to event-v))))

(defn transmitter
  "Transmitter is responsible for sending events."
  ([] (transmitter (chan 100) nil))
  ([on-send] (transmitter (chan 100) on-send))
  ([out-queue on-send]
   (let [this {:out-queue      out-queue
               :send-listeners on-send}]
     (go-loop [event (<! out-queue)]
       (when event
         (handle-outgoing @tube-registry event)
         (call-listeners on-send event)
         (recur (<! out-queue))))
     this)))
