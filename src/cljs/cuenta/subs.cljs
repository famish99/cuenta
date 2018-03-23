(ns cuenta.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.string :refer [blank?]]
            [cljs-time.coerce :as c-time-c]
            [cljs-time.format :as c-time-f]
            [goog.string :as g-string]
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
  :vendor-name-field
  (fn [db _]
    (get db :vendor-name "")))

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
  :tip-amount-store
  (fn [db _]
    (:tip-amount db)))

(rf/reg-sub
  :tip-amount
  :<- [:tip-amount-store]
  (fn [tip-amount _]
    (calc/cast-float tip-amount)))

(rf/reg-sub
  :tip-amount-field
  :<- [:tip-amount-store]
  (fn [tip-amount _]
    {:valid-state (if (and (js/isNaN (js/parseFloat tip-amount))
                           (not (blank? tip-amount)))
                    :error
                    nil)
     :value tip-amount}))

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
    (let [{:keys [item-name item-price]
           :or {item-price const/default-price}} (get items id)]
      {:valid-state (when (and (js/isNaN (js/parseFloat item-price))
                             (util/not-blank? item-price))
                      :error)
       :disabled? (blank? item-name)
       :value item-price})))

(rf/reg-sub
  :item-quantity
  :<- [:items]
  (fn [items [_ id]]
    (let [{:keys [item-name item-quantity]
           :or {item-quantity const/default-quantity}} (get items id)]
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
  :<- [:owners]
  (fn [owners [_ person-name item-id]]
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
  :tax-amount
  :<- [:item-cost-map]
  :<- [:tax-rate]
  (fn [[items tax-rate] _]
    (->> (for [[_ item] items
               :when (-> item :item-taxable false? not)]
          (calc/item-calc item tax-rate))
         (apply +)
         (* (float tax-rate) 0.01))))

(rf/reg-sub
  :total-cost
  :<- [:item-costs]
  :<- [:tip-amount]
  (partial calc/total-cost 1))

(rf/reg-sub
  :claimed-total
  :<- [:item-costs]
  :<- [:tip-amount]
  (partial calc/total-cost 0))

(rf/reg-sub
  :owed
  :<- [:item-costs]
  :<- [:owners]
  :<- [:tip-amount]
  :<- [:total-cost]
  #(calc/calc-owed %1 (second %2)))

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

(rf/reg-sub
  :transaction-id
  (fn [db _]
    (:view-transaction-id db)))

(rf/reg-sub
  :transactions
  (fn [db _]
    (:transactions db)))

(rf/reg-sub
  :t-list-keys
  :<- [:transactions]
  (fn [t-list _]
    (-> t-list
        (dissoc :page-count :current-page)
        keys)))

(rf/reg-sub
  :recent-transactions
  :<- [:t-list-keys]
  (fn [t-list _]
    (->> t-list
         (sort >)
         (take 5))))

(rf/reg-sub
  :per-page
  (fn [db _]
    (:per-page db)))

(rf/reg-sub
  :curr-t-page
  :<- [:transactions]
  (fn [{:keys [current-page]} _]
    current-page))

(rf/reg-sub
  :tot-t-page
  :<- [:transactions]
  (fn [{:keys [page-count]} _]
    page-count))

(rf/reg-sub
  :transaction-list
  :<- [:transactions]
  :<- [:t-list-keys]
  :<- [:per-page]
  (fn [[{:keys [current-page]} t-list per-page] _]
    (-> t-list
        (->> (sort >))
        (nthrest (* per-page (dec current-page)))
        (->> (take per-page)))))

(rf/reg-sub
  :t-item-vendor
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :vendor-name)))

(rf/reg-sub
  :t-item-purchaser
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :given-name)))

(rf/reg-sub
  :t-item-cost
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :total-cost
        (->> (g-string/format "$%.2f")))))

(def t-item-date-formatter
  (c-time-f/formatter "MMM d"))

(rf/reg-sub
  :t-item-date
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :date-added
        (c-time-c/from-date)
        (->> (c-time-f/unparse t-item-date-formatter)))))

(rf/reg-sub
  :t-details
  (fn [db [_ t-id]]
    (get-in db [:transactions t-id])))
