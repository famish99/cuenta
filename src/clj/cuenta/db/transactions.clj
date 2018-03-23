(ns cuenta.db.transactions
  (:require [hugsql.core :as hug]
            [clojure.tools.logging :as log]
            [cuenta.calc :as calc]
            [cuenta.db :as db]))

(hug/def-db-fns "cuenta/db/sql/transactions.sql")

(hug/def-sqlvec-fns "cuenta/db/sql/transactions.sql")

(defn force-find-user-id
  [conn given-name]
  (as-> given-name r
        (if-let [user (select-user conn {:given-name r})]
          (:id user)
          (:generated_key (insert-user conn {:new-user [r ""]})))))

(def find-user-id (db/memoize-transaction force-find-user-id))

(defn force-find-item-id
  [conn vendor-id item]
  (as-> item r
        (update r :item-taxable #(if (false? %) 0 1))
        (assoc r :vendor-id vendor-id)
        (if-let [q-item (select-item conn r)]
          (:id q-item)
          (:generated_key (insert-item conn r)))))

(def find-item-id (db/memoize-transaction force-find-item-id))

(defn find-vendor
  [conn vendor-name]
  (as-> {:vendor-name vendor-name} r
        (if-let [vendor (select-vendor conn r)]
          (:id vendor)
          (:generated_key (insert-vendor conn r)))))

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

(defn add-debts
  [conn debt-matrix]
  (as-> (insert-debt-version conn) r
        (:generated_key r)
        (for [[creditor debt-map] debt-matrix
              [debtor debt] debt-map]
          [debt r (find-user-id conn creditor) (find-user-id conn debtor)])
        (insert-debts conn {:debts r})))

(defn find-debt
  [conn & args]
  (as-> (when-let [version (first args)] {:table-id version}) r
        (select-debts conn r)
        (for [{:keys [creditor debtor amount]} r]
          {creditor {debtor amount}})
        (apply merge-with conj r)))

(defn process-transaction
  [conn curr-t]
  (let [owed-matrix (find-debt conn)
        credit-to (or (:credit-to curr-t) (-> curr-t (get :people) first))
        owners (:owner-matrix curr-t)
        tip-amount (:tip-amount curr-t)
        item-costs (calc/calc-item-cost [(:items curr-t) owners (:tax-rate curr-t)])
        total-cost (calc/total-cost 1 [item-costs tip-amount])]
    (->> (assoc curr-t :total-cost total-cost)
         (add-transaction conn))
    (->> curr-t
         :people
         (remove #{credit-to})
         (map #(hash-map % (calc/calc-owed [item-costs owners tip-amount total-cost] %)))
         (into {})
         (update owed-matrix credit-to (partial merge-with +))
         calc/reduce-debts
         (add-debts conn))
    (find-debt conn)))

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
        (into {} r)))

(defn find-transaction
  [conn params]
  (let [query-params (select-keys params [:transaction-id])
        item-owners (group-owners conn query-params)]
    (->> (select-transaction-items conn query-params)
         (group-by-id :item-id)
         (merge-with conj item-owners))))
