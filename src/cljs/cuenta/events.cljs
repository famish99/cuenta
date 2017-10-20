(ns cuenta.events
  (:require [ajax.core :as ajax]
            [cljs.pprint :refer [pprint]]
            [day8.re-frame.http-fx]
            [goog.string :as g-string]
            [re-frame.core :as rf]
            [cuenta.calc :as calc]
            [cuenta.constants :as const]
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

(rf/reg-event-db
  :add-transaction
  (fn [db _]
    (-> db
        (assoc :route :transaction)
        (merge db/transaction-defaults))))

(rf/reg-event-db
  :update-creditor
  (fn [db [_ new-value]]
    (assoc db :transaction-owner new-value)))

(defn adjust-item-owners
  [{:keys [people owner-matrix] :as db}]
  (update db :owner-matrix select-keys people))

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
  :cast-item
  (fn [db [_ cast-type default-value & path]]
    (update-in db
               (into [:items] path)
               (condp = cast-type
                 :int util/format-int
                 :float util/format-float
                 :money util/format-money)
               default-value)))

(rf/reg-event-db
  :update-owner
  (fn [db [_ new-value person-name i-pos]]
    (assoc-in db [:owner-matrix person-name i-pos] new-value)))

(rf/reg-event-db
  :update-tax-rate
  (fn [db [_ new-value]]
    (assoc db :tax-rate new-value)))

(rf/reg-event-db
  :cast-tax-rate
  (fn [db _]
    (update db :tax-rate util/format-float const/default-tax-rate)))

(rf/reg-event-db
  :update-credit-to
  (fn [db [_ new-value]]
    (assoc db :credit-to new-value)))

(rf/reg-event-fx
  :dump-result
  (fn [world [_ result]]
    (pprint result)))

(rf/reg-event-fx
  :dump-backend
  (fn [world _]
    {:http-xhrio {:method :post
                  :uri "/api"
                  :params (-> world
                              :db
                              (assoc :action :dump-db))
                  :timeout 5000
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:dump-result]
                  :on-failure [:dump-result]}}))

(rf/reg-event-fx
  :save-transaction
  (fn [world _]
    {:http-xhrio {:method :post
                  :uri "/api"
                  :params (-> world
                              :db
                              (select-keys [:items :people :tax-rate :owner-matrix :credit-to])
                              (assoc :action (-> world :event first)))
                  :timeout 5000
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:dump-result]
                  :on-failure [:dump-result]}}))
