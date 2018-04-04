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

(defn map-suggest
  [input-map & _]
  (for [[key name] input-map]
    {:value key :label name}))

(defn compare-with-key
  [key-func]
  (fn [f-val s-val]
    (compare (key-func f-val) (key-func s-val))))

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
  :vendor-name
  (fn [db _]
    (:vendor-name db)))

(rf/reg-sub
  :vendor-name-field
  :<- [:vendor-name]
  (fn [{:keys [vendor-id]}]
    vendor-id))

(rf/reg-sub
  :vendor-map
  (fn [db _]
    (:vendor-map db)))

(rf/reg-sub
  :vendor-suggest
  :<- [:vendor-name]
  :<- [:vendor-map]
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
    (get-in items [id :item-name :item-id])))

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
  :<- [:people]
  :<- [:item-costs]
  :<- [:owners]
  :<- [:tip-amount]
  :<- [:total-cost]
  (fn [[people & args] [_ pos]]
    (->> (get people pos)
         (calc/calc-owed args))))

(rf/reg-sub
  :owed-matrix
  (fn [db _]
    (:owed-matrix db)))

(rf/reg-sub
  :owed-table
  :<- [:owed-matrix]
  :<- [:user-map]
  (fn [[owed-matrix user-map] _]
     (into
       (sorted-map-by
         (fn [& vs]
           (->> (map #(get user-map (:user-id %)) vs)
                (apply compare))))
       owed-matrix)))

(rf/reg-sub
  :get-user-name
  :<- [:user-map]
  (fn [user-map [_ {:keys [user-id]}]]
    (get user-map user-id)))

(rf/reg-sub
  :owed-cols
  :<- [:owed-matrix]
  :<- [:user-map]
  (fn [[owed-matrix user-map] _]
    (->> owed-matrix
         vals
         (map keys)
         flatten
         (map #(assoc % :user-name (get user-map (:user-id %))))
         (into (sorted-set-by (compare-with-key :user-name))))))

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
  :<- [:curr-t-page]
  :<- [:t-list-keys]
  :<- [:per-page]
  (fn [[current-page t-list per-page] _]
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

(rf/reg-sub
  :user-map
  (fn [db _]
    (:user-map db)))

(rf/reg-sub
  :people-suggest
  :<- [:user-map]
  map-suggest)

(rf/reg-sub
  :people-field
  :<- [:people]
  :<- [:user-map]
  (fn [[people user-map] _]
    (for [person people]
      (get user-map person))))

(rf/reg-sub
  :person-field
  :<- [:user-map]
  (fn [user-map [_ {:keys [existing user-id]}]]
    (if existing
      (get user-map user-id)
      user-id)))

(rf/reg-sub
  :person
  :<- [:people]
  (fn [people [_ pos]]
    (-> people
        (get pos)
        :user-id)))

(rf/reg-sub
  :person-suggest
  :<- [:people]
  :<- [:people-suggest]
  (fn [[people suggest] [_ pos]]
    (let [{:keys [existing user-id]} (get people pos)]
      (if (or existing (nil? user-id))
        suggest
        (conj suggest {:value user-id :label user-id})))))

(rf/reg-sub
  :item-map
  (fn [db _]
    (:item-map db)))

(rf/reg-sub
  :items-suggest
  :<- [:item-map]
  (fn [item-map _]
    (for [[key {:keys [item-name]}] item-map]
      {:value key :label item-name})))

(rf/reg-sub
  :item-suggest
  :<- [:items]
  :<- [:items-suggest]
  (fn [[items suggest] [_ pos]]
    (let [{:keys [existing item-id]} (get-in items [pos :item-name])]
      (if (or existing (nil? item-id))
        suggest
        (conj suggest {:value item-id :label item-id})))))

(rf/reg-sub
  :credit-to
  (fn [db _]
    (:credit-to db)))

(rf/reg-sub
  :credit-to-value
  :<- [:credit-to]
  :<- [:people]
  (fn [[credit-to people] _]
    (-> (or credit-to (first people))
        :user-id)))

(rf/reg-sub
  :credit-to-disabled
  :<- [:people]
  (fn [people _]
    (< (count people) 1)))

(rf/reg-sub
  :credit-to-suggest
  :<- [:people]
  :<- [:user-map]
  (fn [[people user-map] _]
    (for [{:keys [user-id existing]} people]
      {:value user-id :label (if existing (get user-map user-id) user-id)})))
