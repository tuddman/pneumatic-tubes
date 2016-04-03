(ns group-chat-app.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :name
  (fn [db]
    (reaction (:name @db))))

(register-sub
  :active-tab
  (fn [db]
    (reaction (:active-tab @db))))

(register-sub
  :backend-connected
  (fn [db]
    (reaction (:backend-connected @db))))

(register-sub
  :chat-room/name
  (fn [db]
    (reaction (get-in @db [:chat-room :name]))))

(register-sub
  :chat-room/users
  (fn [db]
    (reaction (get-in @db [:chat-room :users]))))

(register-sub
  :chat-room/messages
  (fn [db]
    (reaction
      (sort-by :chat-message/at (vals (get-in @db [:chat-room :messages]))))))