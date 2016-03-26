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

;; ----- queries ------

(def message-pull-expression '(pull ?m [:db/id
                                        :chat-message/at
                                        :chat-message/author
                                        :chat-message/text]))

(defn fetch-chat-messages [db room-name]
  (d/q [:find [message-pull-expression '...]
        :in '$ '?room-name
        :where
        ['?r :chat-room/name '?room-name]
        ['?r :chat-room/messages '?m]] db room-name))

(defn extract-new-chat-messages-from-txn
  "Returns [[chat-room new-message]] for new messages created in Datomic transaction."
  [{db :db-after data :tx-data}]
  (d/q [:find '(pull ?r [:chat-room/name]) message-pull-expression
        :in '$ [['?r '?a '?m '_ '?added]]
        :where
        ['?r '?a '?m '_ true]
        ['?a :db/ident :chat-room/messages]] db data))

;; ----- transactions ------

(defn new-message-txn [room-name author text]
  [{:db/id               #db/id [:db.part/user -1]
    :chat-message/author author
    :chat-message/text   text
    :chat-message/at     (new java.util.Date)}
   {:db/id              #db/id [:db.part/user]
    :chat-room/name     room-name
    :chat-room/messages [#db/id[:db.part/user -1]]}])