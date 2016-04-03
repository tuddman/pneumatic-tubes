(ns group-chat-app.ios.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [group-chat-app.ios.ui :as ui]
            [group-chat-app.handlers]
            [group-chat-app.subs]))

(set! js/window.React (js/require "react-native"))
(def app-registry (.-AppRegistry js/React))

(def app-root ui/main-view)

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "GroupChatApp" #(r/reactify-component app-root)))
