(ns group-chat-app.db)

;; initial state of app-db
(def app-db
  {:name              ""
   :chat-room         nil
   :backend-connected false
   :active-tab        :tab/chat})
