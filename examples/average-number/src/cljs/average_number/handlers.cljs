(ns average-number.handlers
  (:require [re-frame.core :as re-frame]
            [pneumatic-tubes.core :as tubes]))

(def default-db {:my-number 0
                 :avg       0})

(defn on-receive [event-v]
  (.log js/console "received from server:" (str event-v))
  (re-frame/dispatch event-v))

(def host (.-host js/document.location))
(def tube (tubes/tube (str "ws://" host "/ws") on-receive))
(def send-to-server (tubes/send-to-tube-middleware tube))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    default-db))

(re-frame/register-handler
  :number-changed
  send-to-server
  (fn [db [_ num]]
    (assoc db :my-number num)))

(re-frame/register-handler
  :average-changed
  (fn [db [_ avg]]
    (assoc db :avg avg)))

(tubes/create! tube)



