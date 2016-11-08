(ns group-chat.core
  (:use org.httpkit.server)
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [resources not-found]]
            [compojure.handler :refer [api]]
            [ring.util.response :refer [file-response]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [pneumatic-tubes.core :refer [receiver wrap-handlers]]
            [pneumatic-tubes.httpkit :refer [websocket-handler]]
            [datomic.api :as d]
            [group-chat.datomic :as db]
            [group-chat.reactions :as r]))

(def db-uri "datomic:mem://test-db")

(defn datomic-transaction-mw [conn]
  (fn [handler]
    (fn [tube event-v]
      (let [[tube txn] (handler tube (d/db conn) event-v)]
        (when txn
          (log/debug "Executing Datomic transaction" txn)
          (d/transact conn txn))
        tube))))

(defn debug-middleware
  "Middleware to log incoming events"
  [handler]
  (fn [tube event-v]
    (log/debug "Received event" event-v "from" tube)
    (handler tube event-v)))


(def handlers
  {:tube/on-create
   (fn [tube db _]
     (let [room-name (:chat-room-name tube)
           user-name (:name tube)]
       (r/push-users-online room-name)
       (r/push-current-chat-messages db room-name tube)
       [tube (db/new-message-txn room-name "SYSTEM" (str "Joined: " user-name))]))

   :tube/on-destroy
   (fn [tube _ _]
     (let [room-name (:chat-room-name tube)
           user-name (:name tube)]
       (r/push-users-online room-name)
       [tube (db/new-message-txn room-name "SYSTEM" (str "Left: " user-name))]))

   :post-message
   (fn [tube _ [_ text]]
     (let [room-name (:chat-room-name tube)
           user-name (:name tube)]
       [tube (db/new-message-txn room-name user-name text)]))})

(def rx (receiver
          (wrap-handlers
            handlers
            (datomic-transaction-mw (db/ensure-db-conn db-uri))
            debug-middleware)))

(defroutes handler
           (GET "/" [] (file-response "index.html" {:root "resources/public"}))
           (GET "/chat" [name room] (websocket-handler rx {:name name :chat-room-name room}))
           (resources "/")
           (not-found "Not Found"))


(def app (wrap-defaults handler {:params {:urlencoded true
                                          :keywordize true}}))

(def app-with-reload (wrap-reload #'app))

(r/react-on-transactions! (db/ensure-db-conn db-uri))