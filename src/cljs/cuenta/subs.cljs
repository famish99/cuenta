(ns cuenta.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :people
  (fn [db _]
    (map vector (range) (:people db))))

(re-frame/reg-sub
  :person
  (fn [db [_ id]]
    (get (:people db) id)))
