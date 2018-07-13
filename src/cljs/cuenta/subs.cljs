(ns cuenta.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

;; -- utility functions -------------------------------------------------------

(defn compare-with-key
  [key-func]
  (fn [f-val s-val]
    (compare (key-func f-val) (key-func s-val))))

;; -- first level subs -------------------------------------------------------

(rf/reg-sub
  :route
  (fn [db _]
    (:route db)))

(rf/reg-sub
  :owed-matrix
  (fn [db _]
    (:owed-matrix db)))

(rf/reg-sub
  :transactions
  (fn [db _]
    (:transactions db)))

(rf/reg-sub
  :user-map
  (fn [db _]
    (:user-map db)))

;; -- second level subs ------------------------------------------------------

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
  :total-owed-col
  :<- [:owed-matrix]
  (fn [owed-matrix [_ user-map]]
    (-> owed-matrix
        vals
        (->> (map #(% (select-keys user-map [:user-id])))
             (reduce +)))))

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
