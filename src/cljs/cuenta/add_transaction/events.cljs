(ns cuenta.add-transaction.events
  (:require [ajax.core :as ajax]
            [bidi.bidi :as bidi]
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.db :as db]
            [cuenta.routes :as rt]
            [cuenta.util :as util]))

;; -- utility functions ------------------------------------------------------

(defn adjust-item-owners
  [{:keys [people] :as db}]
  (->> people
       (update db :owner-matrix select-keys)))

(defn adjust-items-owned
  [{:keys [owner-matrix items] :as db}]
  (let [item-keys (keys items)]
    (->> (for [[owner items-owned] owner-matrix]
           {owner (select-keys items-owned item-keys)})
         (into {})
         (assoc db :owner-matrix))))

;; -- events section ---------------------------------------------------------

(rf/reg-event-fx
  ::add-transaction
  (fn [world _]
    {:db (-> world
             :db
             (assoc :route :add-transaction)
             (merge db/transaction-defaults))
     :api
         {:action :load-vendors
          :on-success [::update-vendor-map]
          :on-failure [:dump-error]}}))

(rf/reg-event-db
  ::cast-item
  (fn [db [_ cast-type default-value & path]]
    (->> (update-in (:items db)
                    path
                    (case cast-type
                      :int util/format-int
                      :float util/format-float
                      :money util/format-money
                      :string util/trim-string)
                    default-value)
         (filter (fn [[_ v]] (util/not-blank? (:item-name v))))
         (into {})
         (assoc db :items)
         adjust-items-owned)))

(rf/reg-event-db
  ::cast-tax-rate
  (fn [db _]
    (update db :tax-rate util/format-float const/default-tax-rate)))

(rf/reg-event-db
  ::cast-tip-amount
  (fn [db _]
    (update db :tip-amount util/format-money const/default-tip)))

(rf/reg-event-fx
  ::save-transaction
  (fn [world _]
    {:api {:action :save-transaction
           :params (-> world
                       :db
                       (select-keys (-> db/transaction-defaults
                                        (dissoc :vendor-map :item-map)
                                        keys)))
           :on-success [:load-home]
           :on-failure [:dump-error]}}))

(rf/reg-event-db
  ::update-credit-to
  (fn [db [_ user-value]]
    (assoc db :credit-to {:user-id user-value
                          :existing (contains? (:user-map db) user-value)})))

(rf/reg-event-db
  ::update-item
  (fn [db [_ new-value & path]]
    (->> new-value
         (assoc-in (:items db) path)
         (assoc db :items))))

(rf/reg-event-db
  ::update-item-map
  (fn [db [_ results]]
    (assoc db :item-map results)))

(rf/reg-event-db
  ::update-item-name
  (fn [db [_ item-value i-pos]]
    (->> (when-let [item (get (:item-map db) item-value)]
           (-> item
               (select-keys [:item-price :item-taxable])
               (update :item-price util/format-money)))
         (merge {:item-name {:item-id item-value
                             :existing (contains? (:item-map db) item-value)}})
         (assoc (:items db) i-pos)
         (filter (fn [[_ v]] (some? (get-in v [:item-name :item-id]))))
         (into {})
         (assoc db :items)
         adjust-items-owned)))

(rf/reg-event-db
  ::update-owner
  (fn [db [_ new-value person-name i-pos]]
    (assoc-in db [:owner-matrix person-name i-pos] new-value)))

(rf/reg-event-db
  ::update-person
  (fn [db [_ pos user-value]]
    (->> {:user-id user-value
          :existing (contains? (:user-map db) user-value)}
         (assoc-in db [:people pos])
         :people
         (filter some?)
         distinct
         vec
         (assoc db :people)
         adjust-item-owners)))

(rf/reg-event-db
  ::update-tax-rate
  (fn [db [_ new-value]]
    (assoc db :tax-rate new-value)))

(rf/reg-event-db
  ::update-tip-amount
  (fn [db [_ new-value]]
    (assoc db :tip-amount new-value)))

(rf/reg-event-db
  ::update-vendor-map
  (fn [db [_ results]]
    (assoc db :vendor-map results)))

(rf/reg-event-fx
  ::update-vendor-name
  (fn [{:keys [db]} [_ vendor-value]]
    (let [existing (contains? (:vendor-map db) vendor-value)]
      (->
        (when existing
          {:api {:action :load-items
                 :params {:vendor-id vendor-value}
                 :on-success [::update-item-map]
                 :on-failure [:dump-error]}})
        (merge {:db (->> {:vendor-id vendor-value :existing existing}
                         (assoc db :vendor-name))})))))
