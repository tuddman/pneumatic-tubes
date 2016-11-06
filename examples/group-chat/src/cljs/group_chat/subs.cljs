(ns group-chat.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

(reg-sub
  :backend-connected
  (fn [db]
    (:backend-connected db)))

(reg-sub
  :chat-room/name
  (fn [db]
    (get-in db [:chat-room :name])))

(reg-sub
  :chat-room/users
  (fn [db]
    (get-in db [:chat-room :users])))

(reg-sub
  :chat-room/messages
  (fn [db]
    (sort-by :chat-message/at (vals (get-in db [:chat-room :messages])))))

