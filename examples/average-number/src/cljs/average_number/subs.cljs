(ns average-number.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :my-number
 (fn [db]
   (reaction (:my-number @db))))

(re-frame/register-sub
  :average
  (fn [db]
    (reaction (:avg @db))))
