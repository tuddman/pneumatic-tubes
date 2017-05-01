(ns group-chat-app.events
  (:require
    [re-frame.core :refer [reg-event-db after]]
    [pneumatic-tubes.core :as tubes]
    [group-chat-app.tubes :refer [tube]]
    [group-chat-app.db :refer [app-db]]))

;; -- Interceptors ------------------------------------------------------------

(def send-to-server (after (fn [_ v] (tubes/dispatch tube v))))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  (fn [_ _]
      app-db))

(reg-event-db
  :enter-chat-room
  (fn [db [_ name room]]
      (tubes/create! tube {:name name :room room})
      (-> db
          (assoc :name name)
          (assoc :chat-room {:name     room
                             :users    []
                             :messages {}}))))

(reg-event-db
  :exit-chat-room
  (fn [_ _]
      (tubes/destroy! tube)
      app-db))

(reg-event-db
  :post-message
  send-to-server
  (fn [db _] db))

(reg-event-db
  :users-online-changed
  (fn [db [_ names]]
      (assoc-in db [:chat-room :users] (-> names distinct sort vec))))

(reg-event-db
  :clean-messages
  (fn [db _]
      (assoc-in db [:chat-room :messages] {})))

(reg-event-db
  :new-messages
  (fn [db [_ messages]]
      (update-in db [:chat-room :messages] into (map (fn [m] [(:db/id m) m]) messages))))

(reg-event-db
  :backend-connected
  (fn [db [_ state]]
      (assoc db :backend-connected state)))

(reg-event-db
  :activate-tab
  (fn [db [_ tab]]
      (assoc db :active-tab tab)))
