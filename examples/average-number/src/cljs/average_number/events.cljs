(ns average-number.events
  (:require [re-frame.core :refer [reg-event-db dispatch after]]
            [pneumatic-tubes.core :as tubes]))

(def default-db {:my-number 0
                 :avg       0})

(defn on-receive [event-v]
      (.log js/console "received from server:" (str event-v))
      (dispatch event-v))

(def tube (tubes/tube (str "ws://localhost:3449/ws") on-receive))
(def send-to-server (after (fn [_ v] (tubes/dispatch tube v))))

(reg-event-db
  :initialize-db
  (fn [_ _]
      default-db))

(reg-event-db
  :number-changed
  send-to-server
  (fn [db [_ num]]
      (assoc db :my-number num)))

(reg-event-db
  :average-changed
  (fn [db [_ avg]]
      (assoc db :avg avg)))

(tubes/create! tube)



