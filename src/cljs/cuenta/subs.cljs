(ns cuenta.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :people
  (fn [db _]
    (:people db)))

;(re-frame/reg-sub
;  :person
;  (fn [db [_ id]]
;    (get (:people db) id)))

(re-frame/reg-sub
  :ap-modal
  (fn [db _]
    (get db :ap-modal?)))
