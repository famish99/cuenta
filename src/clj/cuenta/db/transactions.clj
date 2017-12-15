(ns cuenta.db.transactions
  (:require [hugsql.core :as hug]
            [cuenta.calc :as calc]))

(hug/def-db-fns "cuenta/db/sql/transactions.sql")

(hug/def-sqlvec-fns "cuenta/db/sql/transactions.sql")

(def mem (atom {}))

(defn clear-transaction-cache! [] (reset! mem {}))

(defn memoize-transaction
  "Returns a memoized version of a referentially transparent function with the
  intention of reducing database IO for previously made queries within a
  transaction."
  [f]
  (fn [& args]
    (if-let [e (some-> @mem (get f) (find args))]
      (val e)
      (let [ret (apply f args)]
        (swap! mem assoc-in [f args] ret)
        ret))))

(defn force-find-user-id
  [conn given-name]
  (as-> given-name r
        (if-let [user (select-user conn {:given-name r})]
          (:id user)
          (:generated_key (insert-user conn {:new-user [r ""]})))))

(def find-user-id (memoize-transaction force-find-user-id))

(defn force-find-item-id
  [conn vendor-id item]
  (as-> item r
        (update r :item-taxable #(if (false? %) 0 1))
        (assoc r :vendor-id vendor-id)
        (if-let [q-item (select-item conn r)]
          (:id q-item)
          (:generated_key (insert-item conn r)))))

(def find-item-id (memoize-transaction force-find-item-id))

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
                [item-key item-owned] (->> o-item-map (filter second))]
            [(find-user-id conn owner) (get item-map item-key) transaction-id])
          (insert-transaction-owners conn {:owners r}))))

(defn add-transaction
  [conn transaction]
  (let [vendor-id (->> (get transaction :vendor-name "unknown")
                       (find-vendor conn))
        n-transaction (-> transaction
                          (update :credit-to #(find-user-id conn %))
                          (assoc :vendor-id vendor-id))]
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
        item-costs (calc/calc-item-cost
                     [(:items curr-t) owners (:tax-rate curr-t)])]
    (add-transaction conn curr-t)
    (->> curr-t
         :people
         (remove #{credit-to})
         (map #(hash-map % (calc/calc-owed [item-costs owners] %)))
         (into {})
         (update owed-matrix credit-to (partial merge-with +))
         calc/reduce-debts
         (add-debts conn))
    (find-debt conn)))