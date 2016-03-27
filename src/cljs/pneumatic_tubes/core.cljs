(ns pneumatic-tubes.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [close! chan <! put!]]
            [cljs.reader :as reader]
            [clojure.string :as str]))

(def ^:private instances (atom {}))

(defn log [& msg] (.log js/console (apply str msg)))
(defn error [& msg] (.error js/console (apply str msg)))
(defn warn [& msg] (.warn js/console (apply str msg)))

(def default-config
  {:web-socket-impl js/WebSocket
   :out-queue-size  10})
(defn- noop [])

(defn tube
  "Creates the spec of a tube."
  ([url on-receive]
   (tube url on-receive noop noop default-config))
  ([url on-receive config]
   (tube url on-receive noop noop config))
  ([url on-receive on-connect on-disconnect]
   (tube url on-receive on-connect on-disconnect default-config))
  ([url on-receive on-connect on-disconnect config]
   {:url           url
    :on-receive    on-receive
    :on-disconnect on-disconnect
    :on-connect    on-connect
    :config        config}))

(defn- tube-id [tube-spec]
  (:url tube-spec))

(defn- get-tube-instance [tube-spec]
  (get @instances (tube-id tube-spec)))

(defn- new-tube-instance! [tube-spec socket out-queue]
  (swap! instances assoc (tube-id tube-spec)
         {:socket    socket
          :out-queue out-queue
          :destroyed false}))

(defn- rm-tube-instance! [tube-spec]
  (swap! instances dissoc (tube-id tube-spec)))

(defn- mark-tube-destroyed! [tube-spec]
  (swap! instances assoc-in [(tube-id tube-spec) :destroyed] true))

(defn dispatch [tube event-v]
  "Sends the event to some tube"
  (let [ch (:out-queue (get-tube-instance tube))]
    (if ch
      (put! ch (str event-v))
      (throw (js/Error. (str "Tube for " (:url tube) " is not started!"))))))

(defn- start-send-loop [socket out-queue]
  (go-loop
    [event (<! out-queue)]
    (when event
      (.send socket (str event))
      (recur (<! out-queue)))))

(defn create!
  "Creates a tube. On server side this will trigger :tube/on-create event"
  ([tube]
   (create! tube nil))
  ([tube params]
   (let [param-str (str/join "&" (for [[k v] params] (str (name k) "=" v)))
         {base-url :url rcv-fn :on-receive config :config} tube
         {:keys [on-disconnect on-connect]} tube
         ws-impl (:web-socket-impl config)
         url (if (empty? param-str) base-url (str base-url "?" param-str))]
     (if-let [socket (ws-impl. url)]
       (let [out-queue (chan (:out-queue-size config))]
         (set! (.-onopen socket) #(do
                                   (start-send-loop socket out-queue)
                                   (on-connect)))
         (set! (.-onerror socket) #(error "WebSocket error!"))
         (set! (.-onclose socket) #(let [instance (get-tube-instance tube)]
                                    (close! (:out-queue instance))
                                    (rm-tube-instance! tube)
                                    (if (:destroyed instance)
                                      (log "Destroyed tune on " url)
                                      (on-disconnect))))
         (set! (.-onmessage socket)
               #(let [event-v (-> % .-data reader/read-string)]
                 (rcv-fn event-v)))
         (new-tube-instance! tube socket out-queue)
         (log "Created tube on " url))
       (throw (js/Error. "WebSocket connection failed. url: " url))))))

(defn destroy! [tube]
  "Destroys tube. On server this will trigger :tube/on-destroy event."
  (let [socket (:socket (get-tube-instance tube))]
    (mark-tube-destroyed! tube)
    (.close socket)))

(defn send-to-tube-middleware [tube]
  "Middleware to send an event to server 'as-is'"
  (fn [handler]
    (fn [db v]
      (let [new-db (handler db v)]
        (dispatch tube v)
        new-db))))