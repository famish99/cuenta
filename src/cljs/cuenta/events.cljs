(ns cuenta.events
  (:require [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [cuenta.calc :as calc]
            [cuenta.db :as db]
            [cuenta.util :as util]))

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
         (filter util/not-blank?)
         (distinct)
         (vec)
         (assoc db :people)
         (adjust-item-owners))))

(rf/reg-event-db
  :update-item
  (fn [db [_ new-value & path]]
    (->> new-value
         (assoc-in (:items db) path)
         (filter (fn [[k v]] (util/not-blank? (:item-name v))))
         (into {})
         (assoc db :items))))

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

(rf/reg-event-fx
  :save-success
  (fn [world result]
    (print result)))

(rf/reg-event-fx
  :save-failed
  (fn [world result]
    (print result)))

(rf/reg-event-fx
  :save-transaction
  (fn [world _]
    (print world)
    {:http-xhrio {:method :post
                  :uri "http://localhost:3460/api"
                  :params world
                  :timeout 5000
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:save-success]
                  :on-failure [:save-failed]}}))
    ;(-> db
        ;calc/process-transaction
        ;(conj db/transaction-defaults)
        ;(assoc :route :home)]))
