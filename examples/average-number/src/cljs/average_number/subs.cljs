(ns average-number.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :my-number
  (fn [db]
      (:my-number db)))

(reg-sub
  :average
  (fn [db]
      (:avg db)))
