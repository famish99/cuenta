(ns cuenta.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.string :refer [blank?]]
            [re-frame.core :as rf]
            [cuenta.calc :as calc]
            [cuenta.constants :as const]
            [cuenta.util :as util]))

(rf/reg-sub
  :route
  (fn [db _]
    (:route db)))

(rf/reg-sub
  :people
  (fn [db _]
    (:people db)))

(rf/reg-sub
  :owners
  (fn [db _]
    (:owner-matrix db)))

(rf/reg-sub
  :tax-rate
  (fn [db _]
    (:tax-rate db)))


(rf/reg-sub
  :tax-rate-field
  :<- [:tax-rate]
  (fn [tax-rate _]
    {:valid-state (if (and (js/isNaN (js/parseFloat tax-rate))
                           (not (blank? tax-rate)))
                    :error
                    nil)
     :value tax-rate}))

(rf/reg-sub
  :count-new-people
  :<- [:people]
  (fn [people _]
    (range (inc (count people)))))

(rf/reg-sub
  :count-existing-people
  :<- [:people]
  (fn [people _]
    (range (count people))))

(rf/reg-sub
  :person
  :<- [:people]
  (fn [people [_ id]]
    (get people id "")))

(rf/reg-sub
  :items
  (fn [db _]
    (:items db)))

(rf/reg-sub
  :count-new-items
  :<- [:items]
  (fn [items _]
    (range (inc (count items)))))

(rf/reg-sub
  :count-existing-items
  :<- [:items]
  (fn [items _]
    (range (count items))))

(rf/reg-sub
  :item-name
  :<- [:items]
  (fn [items [_ id]]
    (get-in items [id :item-name] "")))

(rf/reg-sub
  :item-price
  :<- [:items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-price] :or {item-price const/default-price}} (get items id)]
      {:valid-state (when (and (js/isNaN (js/parseFloat item-price))
                             (util/not-blank? item-price))
                      :error)
       :disabled? (blank? item-name)
       :value item-price})))

(rf/reg-sub
  :item-quantity
  :<- [:items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-quantity] :or {item-quantity const/default-quantity}} (get items id)]
      {:valid-state (when (and (js/isNaN (js/parseInt item-quantity))
                            (util/not-blank? item-quantity))
                      :error)
       :disabled? (blank? item-name)
       :value item-quantity})))

(rf/reg-sub
  :item-taxable
  :<- [:items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-taxable] :or {item-taxable true}} (get items id)]
      {:disabled? (blank? item-name)
       :value item-taxable})))

(rf/reg-sub
  :item-owned?
  :<- [:people]
  :<- [:owners]
  (fn [[people owners] [_ person-name item-id]]
    (get-in owners [person-name item-id] false)))

(rf/reg-sub
  :item-cost-map
  :<- [:items]
  (fn [items _]
    (map (fn [[k v]] [k (dissoc v :item-name)]) items)))

(rf/reg-sub
  :item-costs
  :<- [:item-cost-map]
  :<- [:owners]
  :<- [:tax-rate]
  calc/calc-item-cost)

(rf/reg-sub
  :owed
  :<- [:item-costs]
  :<- [:owners]
  (fn [data [_ person-name]]
    (calc/calc-owed data person-name)))

(rf/reg-sub
  :owed-matrix
  (fn [db _]
    (->> db
         :owed-matrix
         (into (sorted-map)))))

(rf/reg-sub
  :owed-cols
  :<- [:owed-matrix]
  (fn [owed-matrix _]
    (->> owed-matrix
         (vals)
         (map keys)
         (flatten)
         (into (sorted-set)))))
