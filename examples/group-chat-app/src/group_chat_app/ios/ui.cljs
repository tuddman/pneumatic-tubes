(ns group-chat-app.ios.ui
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [group-chat-app.ios.styles :as s]))

(def React (js/require "react-native"))

(def text (r/adapt-react-class (.-Text React)))
(def button (r/adapt-react-class (js/require "react-native-button")))
(def text-input (r/adapt-react-class (.-TextInput React)))
(def view (r/adapt-react-class (.-View React)))
(def tab-bar (r/adapt-react-class (.-TabBarIOS React)))
(def tab-bar-item (r/adapt-react-class (.-TabBarIOS.Item React)))
(def image (r/adapt-react-class (.-Image React)))

(defn alert [title]
  (.alert (.-Alert React) title))

(def unknown-user (js/require "./images/user.png"))

(defn login-view []
  (let [name (atom "")
        chat-room (atom "")]
    (fn []
      [view {:style s/view}
       [text {:style s/headng} "Enter chat room"]
       [text-input {:style          s/input
                    :placeholder    "Your name"
                    :on-change-text #(reset! name %)
                    :value          @name}]
       [text-input {:style          s/input
                    :placeholder    "Chat room name"
                    :on-change-text #(reset! chat-room %)
                    :value          @chat-room}]
       [button {:style          s/btn-primary
                :style-disabled s/btn-disabled
                :disabled       (or (empty? @name) (empty? @chat-room))
                :on-press       #(dispatch [:enter-chat-room @name @chat-room])} "Enter"]])))

(defn message-item [author at msg]
  (if (= author "SYSTEM")
    [view {:style {:flex-direction "row"}}
     [text {:style s/author-name} msg]
     [text {:style s/message-time} (-> at .toISOString)]]
    [view {:style {:flex-direction "column" :margin-bottom 10}}
     [view {:style {:flex-direction "row"}}
      [image {:style s/message-author-avatar :source unknown-user}]
      [text {:style s/author-name} author]
      [text {:style s/message-time} (-> at .toISOString)]]
     [view {:style s/message-text-box}
      [text msg]]]))

(defn message-list [messages]
  [view {:style s/chat-view}
   (for [{id :db/id at :chat-message/at author :chat-message/author msg :chat-message/text} messages]
     ^{:key id} [message-item author at msg])])

(defn message-list-component []
  (let [messages (subscribe [:chat-room/messages])]
    (fn []
      [message-list @messages])))

(defn user-list [users]
  [view {:style s/chat-view}
   (for [user users]
     ^{:key user}
     [view {:style {:flex-direction "row" :background-color "#eeeeee" :margin-bottom 5}}
      [image {:style s/user-avatar :source unknown-user}]
      [text {:style s/user-name} user]])])

(defn user-list-component []
  (let [users (subscribe [:chat-room/users])]
    (fn []
      [user-list @users])))

(defn chat-view [active-tab]
  [tab-bar
   [tab-bar-item {:icon     (js/require "./images/chat.png")
                  :selected (= :tab/chat active-tab)
                  :on-press #(dispatch [:activate-tab :tab/chat])
                  :title    "Chat"}
    [message-list-component]]
   [tab-bar-item {:icon     (js/require "./images/users.png")
                  :selected (= :tab/users active-tab)
                  :on-press #(dispatch [:activate-tab :tab/users])
                  :title    "Users online"}
    [user-list-component]]
   [tab-bar-item {:icon     (js/require "./images/exit.png")
                  :selected false
                  :on-press #(dispatch [:exit-chat-room])
                  :title    "Exit"}]])

(defn main-view []
  (let [room-name (subscribe [:chat-room/name])
        users-online (subscribe [:chat-room/users])
        active-tab (subscribe [:active-tab])]
    (fn []
      (if @room-name
        [chat-view @active-tab @room-name @users-online]
        [login-view]))))