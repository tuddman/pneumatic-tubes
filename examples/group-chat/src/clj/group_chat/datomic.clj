(ns group-chat.datomic
  (:require [datomic.api :as d]))

(def uri "datomic:mem://test-db")
(d/create-database uri)
(def conn (d/connect uri))

;; ---- schema ------

(def chat-message
  [{:db/id                 #db/id [:db.part/db]
    :db/ident              :chat-message/text
    :db/valueType          :db.type/string
    :db/cardinality        :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id                 #db/id [:db.part/db]
    :db/ident              :chat-message/at
    :db/valueType          :db.type/instant
    :db/cardinality        :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id                 #db/id [:db.part/db]
    :db/ident              :chat-message/author
    :db/valueType          :db.type/string
    :db/cardinality        :db.cardinality/one
    :db.install/_attribute :db.part/db}])


(def chat-room
  [{:db/id                 #db/id [:db.part/db]
    :db/ident              :chat-room/name
    :db/valueType          :db.type/string
    :db/cardinality        :db.cardinality/one
    :db/unique             :db.unique/identity
    :db.install/_attribute :db.part/db}
   {:db/id                 #db/id [:db.part/db]
    :db/ident              :chat-room/messages
    :db/valueType          :db.type/ref
    :db/cardinality        :db.cardinality/many
    :db.install/_attribute :db.part/db}])

;; init schema
(d/transact
  conn
  (vec (concat chat-room chat-message)))

(defn- find-and-pull [db find-fn pull-fn & args]
  (let [find (partial find-fn db)
        pull (partial pull-fn db)]
    (map pull (apply find args))))

;; ----- queries ------

(defn pull-chat-message [db id]
  (d/pull db [:db/id
              :chat-message/at
              :chat-message/author
              :chat-message/text] id))

(defn find-chat-messages [db room-name]
  (d/q '[:find [?m ...]
         :in $ ?room-name
         :where
         [?r :chat-room/name ?room-name]
         [?r :chat-room/messages ?m]] db room-name))

(defn fetch-chat-messages [db room-name]
  (find-and-pull db find-chat-messages pull-chat-message room-name))

(defn extract-new-chat-messages-from-txn
  "Returns [[chat-room new-message]] for new messages created in Datomic transaction."
  [{db :db-after data :tx-data}]
  (map
    (fn [[room-id msg-id]]
      [(d/pull db [:chat-room/name] room-id)
       (pull-chat-message db msg-id)])
    (d/q '[:find ?r ?m
           :in $ [[?r ?a ?m _ ?added]]
           :where
           [?r ?a ?m _ true]
           [?a :db/ident :chat-room/messages]] db data)))

;; ----- transactions ------

(defn new-message-txn [room-name author text]
  [{:db/id               #db/id [:db.part/user -1]
    :chat-message/author author
    :chat-message/text   text
    :chat-message/at     (new java.util.Date)}
   {:db/id              #db/id [:db.part/user]
    :chat-room/name     room-name
    :chat-room/messages [#db/id[:db.part/user -1]]}])