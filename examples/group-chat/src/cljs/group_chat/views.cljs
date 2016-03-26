(ns group-chat.views
  (:require
    [reagent.ratom :refer [atom]]
    [re-frame.core :as re-frame]))

(defn login-view []
  (let [name (atom "")
        chat-room (atom "")]
    (fn []
      [:div.container
       [:div.form-signin
        [:h2.form-signin-heading "Enter Chat Room"]
        [:input#input-nickname.form-control {:type      "text" :placeholder "Enter your nickname" :value @name
                                             :on-change #(reset! name (-> % .-target .-value))}]
        [:input#input-chatroom.form-control {:type      "text" :placeholder "Enter room name" :value @chat-room
                                             :on-change #(reset! chat-room (-> % .-target .-value))}]
        [:button.btn.btn-lg.btn-primary.btn-block
         {:class    (when (or (empty? @name) (empty? @chat-room)) "disabled")
          :on-click #(re-frame/dispatch [:enter-chat-room @name @chat-room])} "Enter"]]])))

(defn message-input []
  (let [text (atom "")]
    (fn []
    [:div.input-group
     [:input.form-control {:type "text" :placeholder "Enter Message"
                           :value @text
                           :on-change #(reset! text (-> % .-target .-value))}]
     [:span.input-group-btn
      [:button.btn.btn-info {:type     "button"
                             :class (when (empty? @text) "disabled")
                             :on-click #(do
                                         (re-frame/dispatch [:post-message @text])
                                         (reset! text ""))} "SEND"]]])))

(defn chat-view [name users messages]
  [:div.container
   [:div.row
    [:h3.text-center name] [:br] [:br]
    [:div.col-md-8
     [:div.panel.panel-info
      [:div.panel-heading "RECENT CHAT HISTORY"]
      [:div.panel-body]
      [:ul.media-list
       (for [{id :db/id at :chat-message/at author :chat-message/author text :chat-message/text} messages]
         ^{:key id}
         [:li.media
          [:div.media-body
           [:div.media
            [:div.pull-left
             [:img.media-object.img-circle.user-icon {:src "assets/img/unknown.png"}]]
            [:div.media-body
             text
             [:br]
             [:small.text-muted author (str " | " at)]]]]])]]
     [:div.panel-footer
      [message-input]]]
    [:div.col-md-4
     [:div.panel.panel-primary
      [:div.panel-heading "ONLINE USERS"]
      [:div.panel-body
       [:ul.media-list
        (for [user users]
          ^{:key user}
          [:li.media
           [:div.media-body
            [:div.media
             [:div.pull-left
              [:img.media-object.img-circle.user-icon {:src "assets/img/unknown.png"}]]
             [:div.media-body
              [:h5 user]]]]])]]]]]])

(defn main-panel []
  (let [room-name (re-frame/subscribe [:chat-room/name])
        users-online (re-frame/subscribe [:chat-room/users])
        messages (re-frame/subscribe [:chat-room/messages])]
    (fn []
      (if @room-name
        [chat-view @room-name @users-online @messages]
        [login-view]))))
