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
 (fn [db [_ id new-name]]
   (println new-name)
   (assoc-in db [:people id] new-name)))
