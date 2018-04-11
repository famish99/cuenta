(ns cuenta.view-transactions.subs
  (:require [cljs-time.coerce :as c-time-c]
            [cljs-time.format :as c-time-f]
            [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]))

;; -- first level subs -------------------------------------------------------

(rf/reg-sub
  ::per-page
  (fn [db _]
    (::per-page db)))

;; -- second level subs ------------------------------------------------------

(rf/reg-sub
  ::curr-t-page
  :<- [:transactions]
  (fn [{:keys [current-page]} _]
    current-page))

(rf/reg-sub
  ::recent-transactions
  :<- [::t-list-keys]
  (fn [t-list _]
    (->> t-list
         (sort >)
         (take 5))))

(rf/reg-sub
  ::t-item-cost
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :total-cost
        (->> (g-string/format "$%.2f")))))

(def t-item-date-formatter
  (c-time-f/formatter "MMM d"))

(rf/reg-sub
  ::t-item-date
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :date-added
        (c-time-c/from-date)
        (->> (c-time-f/unparse t-item-date-formatter)))))

(rf/reg-sub
  ::t-item-purchaser
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :given-name)))

(rf/reg-sub
  ::t-item-vendor
  :<- [:transactions]
  (fn [t-list [_ t-id]]
    (-> t-list
        (get t-id)
        :vendor-name)))

(rf/reg-sub
  ::t-list-keys
  :<- [:transactions]
  (fn [t-list _]
    (-> t-list
        (dissoc :page-count :current-page)
        keys)))

(rf/reg-sub
  ::tot-t-page
  :<- [:transactions]
  (fn [{:keys [page-count]} _]
    page-count))

(rf/reg-sub
  ::transaction-list
  :<- [::curr-t-page]
  :<- [::t-list-keys]
  :<- [::per-page]
  (fn [[current-page t-list per-page] _]
    (-> t-list
        (->> (sort >))
        (nthrest (* per-page (dec current-page)))
        (->> (take per-page)))))
