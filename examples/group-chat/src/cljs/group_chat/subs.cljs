(ns group-chat.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
  :name
  (fn [db]
    (reaction (:name @db))))

(re-frame/register-sub
  :backend-connected
  (fn [db]
      (reaction (:backend-connected @db))))

(re-frame/register-sub
  :chat-room/name
  (fn [db]
    (reaction (get-in @db [:chat-room :name]))))

(re-frame/register-sub
  :chat-room/users
  (fn [db]
    (reaction (get-in @db [:chat-room :users]))))

(re-frame/register-sub
  :chat-room/messages
  (fn [db]
    (reaction
      (sort-by :chat-message/at (vals (get-in @db [:chat-room :messages]))))))

