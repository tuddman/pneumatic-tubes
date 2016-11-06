(ns group-chat.reactions
  (:require [clojure.tools.logging :as log]
            [pneumatic-tubes.core :refer [transmitter dispatch find-tubes]]
            [group-chat.datomic :as db :refer [conn]]
            [datomic.api :as d]
            [clojure.core.async :refer [go-loop thread]]))

(def tx (transmitter #(log/info "Dispatching " %2 "to" %1)))

(def dispatch-to (partial dispatch tx))
(defn users-in-room [chat-room-name]
  (fn [tube] (= chat-room-name (:chat-room-name tube))))

(defn push-users-online [room-name]
  (let [all-in-room (users-in-room room-name)
        names-in-room (map :name (find-tubes all-in-room))]
    (dispatch-to all-in-room [:users-online-changed names-in-room])))

(defn push-current-chat-messages [db room-name tube]
  (let [messages (db/fetch-chat-messages db room-name)]
    (dispatch-to tube [:clean-messages])
    (dispatch-to tube [:new-messages messages])))

(defn push-new-chat-messages [txn]
  (let [chat-room-msgs (db/extract-new-chat-messages-from-txn txn)]
    (doseq [[room msg] chat-room-msgs]
      (dispatch-to (users-in-room (:chat-room/name room))
                   [:new-messages [msg]]))))

(def tx-queue (d/tx-report-queue conn))
(thread
  (while true
    (let [txn (.take tx-queue)]
      (try
        (when (:tx-data txn)
          (push-new-chat-messages txn))
        (catch Exception e
          (log/error e "There was an error diring processing of datomic transaction"))))))
