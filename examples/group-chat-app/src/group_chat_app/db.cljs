(ns group-chat-app.db
  (:require [schema.core :as s :include-macros true]))

;; schema of app-db
(def Message {:db/id               s/Int
              :chat-message/at     s/Inst
              :chat-message/text   s/Str
              :chat-message/author s/Str})

(def schema {:name              s/Str
             :chat-room         (s/maybe {:name     s/Str
                                          :users    [s/Str]
                                          :messages {s/Int Message}})
             :backend-connected s/Bool
             :active-tab        :s/Keyword})

;; initial state of app-db
(def app-db
  {:name              ""
   :chat-room         nil
   :backend-connected false
   :active-tab        :tab/chat})
