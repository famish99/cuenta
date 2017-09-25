(ns cuenta.events
  (:require [clojure.string :refer [blank?]]
            [re-frame.core :as rf]
            [cuenta.db :as db]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-db
  :update-route
  (fn [db [_ new-route]]
    (assoc db :route new-route)))

(defn adjust-item-owners
  [{:keys [people owner-matrix] :as db}]
  (update db :owner-matrix select-keys people))

(rf/reg-event-db
 :update-person
 (fn [db [_ pos new-name]]
   (->> (assoc (:people db) pos new-name)
        (filterv (complement blank?))
        (assoc db :people)
        (adjust-item-owners))))

(rf/reg-event-db
  :update-item
  (fn [db [_ new-value & path]]
    (let [result
          (->> new-value
               (assoc-in (:items db) path)
               (filter (fn [[k v]] (not (blank? (:item-name v)))))
               (into {})
               (assoc db :items))]
      result)))

(rf/reg-event-db
  :update-owner
  (fn [db [_ new-value p-pos i-pos]]
    (assoc-in db [:owner-matrix (get-in db [:people p-pos]) i-pos] new-value)))

(rf/reg-event-db
  :update-tax-rate
  (fn [db [_ new-value]]
    (assoc db :tax-rate new-value)))
