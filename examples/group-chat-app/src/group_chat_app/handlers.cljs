(ns group-chat-app.handlers
  (:require
    [re-frame.core :refer [register-handler after]]
    [schema.core :as s :include-macros true]
    [pneumatic-tubes.core :as tubes]
    [group-chat-app.tubes :refer [tube]]
    [group-chat-app.db :refer [app-db schema]]))

;; -- Middleware ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/wiki/Using-Handler-Middleware
;;
(defn check-and-throw
  "throw an exception if db doesn't match the schema."
  [a-schema db]
  (if-let [problems (s/check a-schema db)]
    (throw (js/Error. (str "schema check failed: " problems)))))

(def validate-schema-mw
  (after (partial check-and-throw schema)))

(def send-to-server (tubes/send-to-tube-middleware tube))

;; -- Handlers --------------------------------------------------------------

(register-handler
  :initialize-db
  (fn [_ _]
    app-db))

(register-handler
  :enter-chat-room
  (fn [db [_ name room]]
    (tubes/create! tube {:name name :room room})
    (-> db
        (assoc :name name)
        (assoc :chat-room {:name     room
                           :users    []
                           :messages {}}))))

(register-handler
  :exit-chat-room
  (fn [_ _]
    (tubes/destroy! tube)
    app-db))

(register-handler
  :post-message
  send-to-server
  (fn [db _] db))

(register-handler
  :users-online-changed
  (fn [db [_ names]]
    (assoc-in db [:chat-room :users] (-> names distinct sort vec))))

(register-handler
  :clean-messages
  (fn [db _]
    (assoc-in db [:chat-room :messages] {})))

(register-handler
  :new-messages
  (fn [db [_ messages]]
    (update-in db [:chat-room :messages] into (map (fn [m] [(:db/id m) m]) messages))))

(register-handler
  :backend-connected
  (fn [db [_ state]]
    (assoc db :backend-connected state)))

(register-handler
  :activate-tab
  (fn [db [_ tab]]
    (assoc db :active-tab tab)))