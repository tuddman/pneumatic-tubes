(ns pneumatic-tubes.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [close! chan <! put!]]
            [cljs.reader :as reader]
            [clojure.string :as str]))

(def ^:private sockets (atom {}))

(defn tube
  "Creates the spec of a tube."
  [url on-receive]
  {:url        url
   :on-receive on-receive})

(defn dispatch [tube event-v]
  "Sends the event to some tube"
  (let [url (:url tube)
        ch (get-in @sockets [url :out-queue])]
    (if ch
      (put! ch (str event-v))
      (throw (js/Error. (str "Replicator for " url " is not started!"))))))

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
         {base-url :url rcv-fn :on-receive} tube
         url (if (empty? param-str) base-url (str base-url "?" param-str))]
     (.log js/console "Creating tube on " url)
     (if-let [socket (js/WebSocket. url)]
       (let [out-queue (chan 10)]
         (set! (.-onopen socket) #(start-send-loop socket out-queue))
         (set! (.-onerror socket) #(.error js/console "WebSocket error!"))
         (set! (.-onmessage socket)
               #(let [event-v (-> % .-data reader/read-string)]
                 (rcv-fn event-v)))
         (swap! sockets assoc base-url {:socket    socket
                                        :out-queue out-queue}))
       (throw (js/Error. "WebSocket connection failed!"))))))

(defn destroy! [tube]
  "Destroys tube. On server this will trigger :tube/on-destroy event."
  (let [url (:url tube)
        instance (get @sockets url)]
    (.log js/console "Destroying tube on " url)
    (close! (:out-queue instance))
    (.close (:socket instance))
    (swap! sockets dissoc url)))

(defn send-to-tube-middleware [tube]
  "Middleware to send an event to server 'as-is'"
  (fn [handler]
    (fn [db v]
      (let [new-db (handler db v)]
        (dispatch tube v)
        new-db))))