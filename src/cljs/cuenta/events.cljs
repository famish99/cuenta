(ns cuenta.events
  (:require [re-frame.core :as re-frame]
            [cuenta.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 :update-name
 (fn [db [_ new-name]]
   (assoc db :name new-name)))

(re-frame/reg-event-db
 :update-person
 (fn [db [_ new-name]]
   (println new-name)
   (assoc db :person-text new-name)))

(re-frame/reg-event-db
  :add-person
  (fn [db _]
    (update db :people conj (:person-text db))))

(re-frame/reg-event-db
  :ap-modal
  (fn [db [_ bool]]
    (assoc db :ap-modal? bool)))