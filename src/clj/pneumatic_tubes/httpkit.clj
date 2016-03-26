(ns pneumatic-tubes.httpkit
  (:use pneumatic-tubes.core
        org.httpkit.server))

(defn- send-fn [ch]
  (fn [data]
    (when (open? ch)
      (send! ch data))))

(defn websocket-handler
  "Creates WebSocket request handler, use it in your compojure routes"
  ([receiver]
   (websocket-handler receiver {}))
  ([receiver tube-data]
   (fn [request]
     (with-channel
       request ch
       (let [tube-id (add-tube! (send-fn ch) tube-data)]
         (on-close ch (fn [_]
                        (let [destroyed-tube (get-tube tube-id)]
                          (rm-tube! tube-id)
                          (receive receiver destroyed-tube [:tube/on-destroy]))))
         (on-receive ch (fn [message]
                          (receive receiver (get-tube tube-id) (read-string message))))
         (receive receiver (get-tube tube-id) [:tube/on-create]))))))