(ns group-chat.handlers
  (:require [re-frame.core :as re-frame]
            [group-chat.db :as db]
            [pneumatic-tubes.core :as tubes]))

(defn on-receive [event-v]
  (.log js/console "received from server:" (str event-v))
  (re-frame/dispatch event-v))

(defn on-disconnect []
      (re-frame/dispatch [:backend-connected false]))

(defn on-connect []
      (.log js/console "Connection with server lost.")
      (re-frame/dispatch [:backend-connected true]))

(def tube (tubes/tube (str "ws://localhost:3449/chat") on-receive on-connect on-disconnect))
(def send-to-server (tubes/send-to-tube-middleware tube))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/register-handler
  :enter-chat-room
  (fn [db [_ name room]]
    (tubes/create! tube {:name name :room room})
    (-> db
        (assoc :name name)
        (assoc :chat-room {:name  room
                           :users []
                           :messages {}}))))

(re-frame/register-handler
  :post-message
  send-to-server
  (fn [db _] db))

(re-frame/register-handler
  :users-online-changed
  (fn [db [_ names]]
    (assoc-in db [:chat-room :users] (-> names distinct sort vec))))

(re-frame/register-handler
  :new-messages
  (fn [db [_ messages]]
    (update-in db [:chat-room :messages] into (map (fn [m] [(:db/id m) m]) messages))))

(re-frame/register-handler
  :backend-connected
  (fn [db [_ state]]
      (assoc db :backend-connected state)))