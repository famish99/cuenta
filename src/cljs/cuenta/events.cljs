(ns cuenta.events
  (:require [clojure.string :refer [blank?]]
            [re-frame.core :as rf]
            [cuenta.calc :as calc]
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
  :update-creditor
  (fn [db [_ new-value]]
    (assoc db :transaction-owner new-value)))

(rf/reg-event-db
  :update-person
  (fn [db [_ pos new-name]]
    (->> (assoc (:people db) pos new-name)
         (filter (complement blank?))
         (distinct)
         (vec)
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
  (fn [db [_ new-value person-name i-pos]]
    (assoc-in db [:owner-matrix person-name i-pos] new-value)))

(rf/reg-event-db
  :update-tax-rate
  (fn [db [_ new-value]]
    (assoc db :tax-rate new-value)))

(rf/reg-event-db
  :update-credit-to
  (fn [db [_ new-value]]
    (assoc db :credit-to new-value)))

(rf/reg-event-db
  :save-transaction
  (fn [db _]
    (let [credit-to (or (:credit-to db) (-> db (get :people) (first)))
          owners (:owner-matrix db)
          item-costs (calc/calc-item-cost [(:items db) owners (:tax-rate db)])
          result
          (->> db
               (:people)
               (remove #{credit-to})
               (map #(vector % (calc/calc-owed [item-costs owners] [_ %])))
               )]
      (print result credit-to)
      db)))
