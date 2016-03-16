(ns average-number.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [average-number.handlers]
            [average-number.subs]))

(defn main-view-inner [my-number avg]
  [:div
   [:input {:type :number
            :on-change #(re-frame/dispatch [:number-changed (-> % .-target .-value)])
            :value my-number}]
   [:h3 "Overall avg: " avg]])

(defn main-view []
  (let [my-number (re-frame/subscribe [:my-number])
        avg (re-frame/subscribe [:average])]
    (fn []
      [main-view-inner @my-number @avg])))

(defn mount-root []
  (reagent/render [main-view]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
