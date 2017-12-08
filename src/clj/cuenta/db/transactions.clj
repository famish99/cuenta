(ns cuenta.db.transactions
  (:require [hugsql.core :as hug]))

(hug/def-db-fns "cuenta/db/sql/transactions.sql")

(hug/def-sqlvec-fns "cuenta/db/sql/transactions.sql")

(defn force-find-user-id
  [conn given-name]
  (as-> given-name r
        (if-let [user (select-user conn {:given-name r})]
          (:id user)
          (:generated_key (insert-user conn {:new-user [r ""]})))))

(def find-user-id (memoize force-find-user-id))

(defn force-find-item-id
  [conn vendor-id item]
  (as-> item r
        (update r :item-taxable #(if (false? %) 0 1))
        (assoc r :vendor-id vendor-id)
        (if-let [q-item (select-item conn r)]
          (:id q-item)
          (:generated_key (insert-item conn r)))))

(def find-item-id (memoize force-find-item-id))

(defn find-vendor
  [conn vendor-name]
  (as-> {:vendor-name vendor-name} r
    (if-let [vendor (select-vendor conn r)]
      (:id vendor)
      (:generated_key (insert-vendor conn r)))))

(defn add-transaction-item
  [conn {:keys [vendor-id transaction-id]} item]
  (as-> (find-item-id conn vendor-id item) r
        (vector (or (:item-quantity item) 1) r transaction-id)
        (insert-transaction-item conn {:new-item r})
        (:generated_key r)))

(defn add-transaction-children
  [conn {:keys [people items owner-matrix transaction-id vendor-id]
         :as transaction}]
  (let [user-map (->> (for [person people]
                        {person (find-user-id conn person)})
                      (into {}))
        item-map (->> (for [[item-key item] items]
                        {item-key
                         (add-transaction-item conn transaction item)})
                      (into {}))]
    (as-> owner-matrix r
          (for [[owner o-item-map] r
                [item-key item-owned] (->> o-item-map (filter second))]
            [(get user-map owner) (get item-map item-key) transaction-id])
          (into [] r)
          (hash-map :owners r)
          (add-transaction-owners conn r))))

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
