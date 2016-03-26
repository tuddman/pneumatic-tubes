(ns group-chat.core
  (:use org.httpkit.server)
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes routes]]
            [compojure.handler :refer [api]]
            [ring.util.response :refer [file-response]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [pneumatic-tubes.core :refer [receiver wrap-handlers]]
            [pneumatic-tubes.httpkit :refer [websocket-handler]]
            [datomic.api :as d]
            [group-chat.datomic :as db :refer [conn]]
            [group-chat.reactions :as r]))

(defn datomic-transaction
  "Middleware to apply a Datomic transaction.
  The handler takes DB and is expected to return transaction data to be applied."
  [handler]
  (fn [tube event-v]
    (let [[tube txn] (handler tube (d/db conn) event-v)]
      (when txn
        (log/debug "Executing Datomic transaction" txn)
        (d/transact conn txn))
      tube)))

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
       (r/push-current-chat-messages room-name tube db)
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
            datomic-transaction
            debug-middleware)))

(defroutes handler
           (GET "/" [] (file-response "index.html" {:root "resources/public"}))
           (GET "/chat" [name room] (websocket-handler rx {:name name :chat-room-name room})))

(def app (wrap-defaults handler {:params {:urlencoded true
                                          :keywordize true}}))