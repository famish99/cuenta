(ns cuenta.db.transactions
  (:require [hugsql.core :as hug]
            [clojure.tools.logging :as log]
            [cuenta.calc :as calc]
            [cuenta.db :as db]))

;; -- import SQL functions ---------------------------------------------------

(hug/def-db-fns "cuenta/db/sql/transactions.sql")

(hug/def-sqlvec-fns "cuenta/db/sql/transactions.sql")

;; -- retrieval section ------------------------------------------------------

;; --- lookup helpers section

(defn force-find-user-id
  [conn {:keys [user-id existing]}]
  (if existing
    user-id
    (:generated_key (insert-user conn {:new-user [user-id ""]}))))

(defn force-find-item-id
  [conn vendor-id item]
  (let [item-id (get-in item [:item-name :item-id])
        v-item (select-item conn (assoc item :item-id item-id))
        is-same? (= (:item-price v-item) (:item-price item))
        adj-item (-> item
                     (update :item-taxable #(if (false? %) 0 1))
                     (assoc :vendor-id vendor-id))]
    (cond
      (and (get-in item [:item-name :existing])
           is-same?)
      item-id
      (and (get-in item [:item-name :existing])
           (not is-same?))
      (-> adj-item
          (assoc :item-name (:item-name v-item))
          (->> (insert-item conn))
          :generated_key)
      :else
      (-> adj-item
          (update :item-name :item-id)
          (->> (insert-item conn))
          :generated_key))))

(def find-item-id (db/memoize-transaction force-find-item-id))

(def find-user-id (db/memoize-transaction force-find-user-id))

(defn group-by-id
  [key coll]
  (->> (for [item coll]
         {(get item key) (dissoc item key)})
       (into {})))

(defn group-owners
  [conn params]
  (as-> (select-transaction-owners conn params) r
        (for [item r]
          {(:item-id item) [(select-keys item [:given-name])]})
        (apply merge-with into r)
        (for [[k v] r] {k {:owners v}})
        (into (sorted-map) r)))

;; --- lookup functions section

(defn find-debt
  [conn & args]
  (as-> (when-let [version (first args)] {:table-id version}) r
        (select-debts conn r)
        (for [{:keys [creditor debtor amount]} r]
          {{:user-id creditor} {{:user-id debtor} amount}})
        (apply merge-with conj r)))

(defn find-items
  [conn params]
  (->>
    (for [{:keys [item-id] :as item} (select-items conn params)]
      {item-id (select-keys item [:item-name :item-price :item-taxable])})
    (into {})))

(defn find-transaction
  [conn params]
  (let [query-params (select-keys params [:transaction-id])
        item-owners (group-owners conn query-params)]
    (->> (select-transaction-items conn query-params)
         (group-by-id :item-id)
         (merge-with conj item-owners))))

(defn find-transactions
  [conn params]
  (let [per-page (or (:r-limit params) 10)
        page-count (-> (select-transaction-count conn)
                       :row-count
                       (/ per-page)
                       (Math/ceil)
                       int)]
    (as-> (select-transactions conn params) r
          (for [{:keys [transaction-id] :as item} r]
            {transaction-id (dissoc item :transaction-id)})
          (into {:page-count page-count} r))))

(defn find-users
  [conn _]
  (->>
    (for [{:keys [user-id given-name]} (select-users conn)]
      {user-id given-name})
    (into {})))

(defn find-vendor
  [conn {:keys [vendor-id existing]}]
  (if existing
    vendor-id
    (:generated_key (insert-vendor conn {:vendor-name vendor-id}))))

(defn find-vendors
  [conn _]
  (->>
    (for [{:keys [vendor-id vendor-name]} (select-vendors conn)]
      {vendor-id vendor-name})
    (into {})))

;; -- insertion section ------------------------------------------------------

;; --- insertion helpers

(defn add-debts
  [conn debt-matrix]
  (as-> (insert-debt-version conn) r
        (:generated_key r)
        (for [[creditor debt-map] debt-matrix
              [debtor debt] debt-map]
          [debt r (:user-id creditor) (:user-id debtor)])
        (insert-debts conn {:debts r})))

(defn add-transaction-item
  [conn {:keys [vendor-id transaction-id]} item]
  (as-> (find-item-id conn vendor-id item) r
        (insert-transaction-item
          conn {:new-item [(or (:item-quantity item) 1) r transaction-id]})
        (:generated_key r)))

(defn add-transaction-children
  [conn {:keys [items owner-matrix transaction-id]
         :as   transaction}]
  (let [item-map (->> (for [[item-key item] items]
                        {item-key
                         (add-transaction-item conn transaction item)})
                      (into {}))]
    (as-> owner-matrix r
          (for [[owner o-item-map] r
                [item-key _] (->> o-item-map (filter second))]
            [(find-user-id conn owner) (get item-map item-key) transaction-id])
          (insert-transaction-owners conn {:owners r}))))

(defn add-transaction
  [conn transaction]
  (let [n-transaction (-> transaction
                          (update :credit-to #(find-user-id conn %))
                          (assoc :vendor-id
                                 (->> (get transaction :vendor-name "unknown")
                                      (find-vendor conn))))]
    (as-> n-transaction r
          (insert-transaction conn r)
          (hash-map :transaction-id (:generated_key r))
          (merge n-transaction r)
          (add-transaction-children conn r))))

;; --- insertion functions section

(defn process-transaction
  [conn {:keys [items tax-rate credit-to owner-matrix tip-amount people] :as curr-t}]
  (let [owed-matrix (find-debt conn)
        own-mat-with-ids (->> (for [[person items] owner-matrix]
                                {{:user-id (find-user-id conn person)} items})
                              (into {}))
        credit-to {:user-id (find-user-id conn (or credit-to (-> people first)))}
        item-costs (calc/calc-item-cost [items own-mat-with-ids tax-rate])
        total-cost (calc/total-cost 1 [item-costs tip-amount])]
    (->> (assoc curr-t :total-cost total-cost)
         (add-transaction conn))
    (->> (for [person people]
           {:user-id (find-user-id conn person)})
         (into [])
         (remove #{credit-to})
         (map #(hash-map (select-keys % [:user-id])
                         (calc/calc-owed
                           [item-costs own-mat-with-ids tip-amount total-cost]
                           %)))
         (into {})
         (update owed-matrix
                 (select-keys credit-to [:user-id])
                 (partial merge-with +))
         calc/reduce-debts
         (add-debts conn))
    (find-debt conn)))
