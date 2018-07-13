(ns cuenta.add-transaction.subs
  (:require [clojure.string :as cs]
            [re-frame.core :as rf]
            [cuenta.calc :as calc]
            [cuenta.constants :as const]
            [cuenta.util :as util]))

;; -- utility functions -------------------------------------------------------

(defn compare-labels
  [val_1 val_2]
  (compare (:label val_1) (:label val_2)))

(defn map-suggest
  [input-map & _]
  (->> (for [[key name] input-map]
         {:value key :label name})
       (into (sorted-set-by compare-labels))))

;; -- first level subs -------------------------------------------------------

(rf/reg-sub
  ::credit-to
  (fn [db _]
    (:credit-to db)))

(rf/reg-sub
  ::items
  (fn [db _]
    (:items db)))

(rf/reg-sub
  ::item-map
  (fn [db _]
    (:item-map db)))

(rf/reg-sub
  ::owners
  (fn [db _]
    (:owner-matrix db)))

(rf/reg-sub
  ::people
  (fn [db _]
    (:people db)))

(rf/reg-sub
  ::tax-rate
  (fn [db _]
    (:tax-rate db)))

(rf/reg-sub
  ::tip-amount-store
  (fn [db _]
    (:tip-amount db)))

(rf/reg-sub
  ::vendor-map
  (fn [db _]
    (:vendor-map db)))

(rf/reg-sub
  ::vendor-name
  (fn [db _]
    (:vendor-name db)))

;; -- second level subs ------------------------------------------------------

;; --- intermediate calculation subs

(rf/reg-sub
  ::item-cost-map
  :<- [::items]
  (fn [items _]
    (map (fn [[k v]] [k (dissoc v :item-name)]) items)))

(rf/reg-sub
  ::item-costs
  :<- [::item-cost-map]
  :<- [::owners]
  :<- [::tax-rate]
  calc/calc-item-cost)

(rf/reg-sub
  ::tip-amount
  :<- [::tip-amount-store]
  (fn [tip-amount _]
    (calc/cast-float tip-amount)))

;; --- intermediate suggestion subs

(rf/reg-sub
  ::items-suggest
  :<- [::item-map]
  (fn [item-map _]
    (->> (for [[key {:keys [item-name]}] item-map]
           {:value key :label item-name})
         (into (sorted-set-by compare-labels)))))

(rf/reg-sub
  ::people-suggest
  :<- [:user-map]
  map-suggest)

;; --- input field related subs

(rf/reg-sub
  ::count-existing-items
  :<- [::items]
  (fn [items _]
    (range (count items))))

(rf/reg-sub
  ::count-existing-people
  :<- [::people]
  (fn [people _]
    (range (count people))))

(rf/reg-sub
  ::count-new-items
  :<- [::items]
  (fn [items _]
    (range (inc (count items)))))

(rf/reg-sub
  ::count-new-people
  :<- [::people]
  (fn [people _]
    (range (inc (count people)))))

(rf/reg-sub
  ::credit-to-disabled
  :<- [::people]
  (fn [people _]
    (< (count people) 1)))

(rf/reg-sub
  ::credit-to-value
  :<- [::credit-to]
  :<- [::people]
  (fn [[credit-to people] _]
    (-> (or credit-to (first people))
        :user-id)))

(rf/reg-sub
  ::item-name
  :<- [::items]
  (fn [items [_ id]]
    (get-in items [id :item-name :item-id])))

(rf/reg-sub
  ::item-price
  :<- [::items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-price]
           :or {item-price const/default-price}} (get items id)]
      {:valid-state (when (and (js/isNaN (js/parseFloat item-price))
                               (util/not-blank? item-price))
                      :error)
       :disabled? (cs/blank? item-name)
       :value item-price})))

(rf/reg-sub
  ::item-quantity
  :<- [::items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-quantity]
           :or {item-quantity const/default-quantity}} (get items id)]
      {:valid-state (when (and (js/isNaN (js/parseInt item-quantity))
                               (util/not-blank? item-quantity))
                      :error)
       :disabled? (cs/blank? item-name)
       :value item-quantity})))

(rf/reg-sub
  ::item-taxable
  :<- [::items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-taxable] :or {item-taxable true}} (get items id)]
      {:disabled? (cs/blank? item-name)
       :value item-taxable})))

(rf/reg-sub
  ::item-owned?
  :<- [::owners]
  (fn [owners [_ person-name item-id]]
    (get-in owners [person-name item-id] false)))

(rf/reg-sub
  ::person
  :<- [::people]
  (fn [people [_ pos]]
    (-> people
        (get pos)
        :user-id)))

(rf/reg-sub
  ::person-field
  :<- [:user-map]
  (fn [user-map [_ {:keys [existing user-id]}]]
    (if existing
      (get user-map user-id)
      user-id)))

(rf/reg-sub
  ::tax-rate-field
  :<- [::tax-rate]
  (fn [tax-rate _]
    {:valid-state (if (and (js/isNaN (js/parseFloat tax-rate))
                           (not (cs/blank? tax-rate)))
                    :error
                    nil)
     :value tax-rate}))

(rf/reg-sub
  ::tip-amount-field
  :<- [::tip-amount-store]
  (fn [tip-amount _]
    {:valid-state (if (and (js/isNaN (js/parseFloat tip-amount))
                           (not (cs/blank? tip-amount)))
                    :error
                    nil)
     :value tip-amount}))

(rf/reg-sub
  ::vendor-name-field
  :<- [::vendor-name]
  (fn [{:keys [vendor-id]}]
    vendor-id))

;; ---- input suggestion subs

(rf/reg-sub
  ::credit-to-suggest
  :<- [::people]
  :<- [:user-map]
  (fn [[people user-map] _]
    (for [{:keys [user-id existing]} people]
      {:value user-id :label (if existing (get user-map user-id) user-id)})))

(rf/reg-sub
  ::item-suggest
  :<- [::items]
  :<- [::items-suggest]
  (fn [[items suggest] [_ pos]]
    (let [{:keys [existing item-id]} (get-in items [pos :item-name])]
      (if (or existing (nil? item-id))
        suggest
        (conj suggest {:value item-id :label item-id})))))

(rf/reg-sub
  ::person-suggest
  :<- [::people]
  :<- [::people-suggest]
  (fn [[people suggest] [_ pos]]
    (let [{:keys [existing user-id]} (get people pos)]
      (if (or existing (nil? user-id))
        suggest
        (conj suggest {:value user-id :label user-id})))))

(rf/reg-sub
  ::vendor-suggest
  :<- [::vendor-name]
  :<- [::vendor-map]
  (fn [[{:keys [vendor-id existing]} v-map] _]
    (as-> v-map r
          (if-not (or existing (nil? existing))
            (merge r {vendor-id vendor-id})
            r)
          (into
            (sorted-map-by
              (fn [f-v s-v]
                (compare (get r f-v) (get r s-v)))) r)
          (map-suggest r))))

;; --- output related subs

(rf/reg-sub
  ::claimed-total
  :<- [::item-costs]
  :<- [::tip-amount]
  (partial calc/total-cost 0))

(rf/reg-sub
  ::tax-amount
  :<- [::item-cost-map]
  :<- [::tax-rate]
  (fn [[items tax-rate] _]
    (->> (for [[_ {:keys [item-price item-quantity item-taxable]
                   :or {item-quantity 1 item-taxable true}}]
               items
               :when (-> item-taxable false? not)]
           (* item-price (int (or item-quantity 1))))
         (apply +)
         (* (float tax-rate) 0.01))))

(rf/reg-sub
  ::total-cost
  :<- [::item-costs]
  :<- [::tip-amount]
  (partial calc/total-cost 1))

(rf/reg-sub
  ::owed
  :<- [::people]
  :<- [::item-costs]
  :<- [::owners]
  :<- [::tip-amount]
  :<- [::total-cost]
  (fn [[people & args] [_ pos]]
    (->> (get people pos)
         (calc/calc-owed args))))
