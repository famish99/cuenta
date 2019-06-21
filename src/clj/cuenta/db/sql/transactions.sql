-- :name insert-user :i!
INSERT INTO users (given_name, surname) VALUES :t:new-user

-- :name select-user :? :1
SELECT user_id AS id
  FROM users
 WHERE UPPER (given_name) = UPPER (:given-name)

-- :name select-users :?
SELECT user_id AS `user-id`,
       given_name AS `given-name`
  FROM users
 ORDER BY given_name

-- :name insert-vendor :i!
INSERT INTO vendors (vendor_name)
VALUES (:vendor-name)

-- :name select-vendor :? :1
SELECT vendor_id AS id
  FROM vendors
 WHERE UPPER (vendor_name) = UPPER (:vendor-name)

-- :name select-vendors :?
SELECT vendor_id AS `vendor-id`,
       vendor_name AS `vendor-name`
  FROM vendors
 ORDER BY vendor_name

-- :name insert-item :i!
INSERT INTO items (item_name, item_price, item_taxable, vendor_id)
VALUES (:item-name, :item-price, :item-taxable, :vendor-id)

-- :name select-item :? :1
SELECT item_name AS `item-name`,
       item_price AS `item-price`
  FROM items
 WHERE item_id = :item-id

-- :name select-items :?
SELECT item_id AS `item-id`,
       item_name AS `item-name`,
       item_price AS `item-price`,
       item_taxable AS `item-taxable`
  FROM items
 WHERE vendor_id = :vendor-id

-- :name insert-transaction :i!
INSERT INTO transactions (tax_rate, tip_amount, total_cost, credit_to, vendor_id)
VALUES (:tax-rate, :tip-amount, :total-cost, :credit-to, :vendor-id)

-- :name insert-transaction-item :i!
INSERT INTO transaction_items (item_quantity, transaction_id, item_id)
VALUES :t:new-item

-- :name insert-transaction-owners :!
INSERT INTO transaction_owners (user_id, transaction_id, item_id)
VALUES :t*:owners

-- :name insert-debt-version :i!
INSERT INTO debt_table () VALUES ()

-- :name insert-debts :!
INSERT INTO debts (table_id, creditor_id, debtor_id, amount)
VALUES :t*:debts

-- :name select-debts :?
SELECT creditor_id AS creditor,
       debtor_id AS debtor,
       amount
  FROM debts
 WHERE table_id =
       /*~ (if (:table-id params) */
       :table-id
       /*~*/
       (SELECT MAX(d_table_id) FROM debt_table)
       /*~ ) ~*/

-- :name select-transaction-count :? :1
SELECT COUNT(transaction_id) AS `row-count`
  FROM transactions
   USE INDEX (PRIMARY)

-- :name select-transactions :?
SELECT transaction_id AS `transaction-id`,
       tax_rate AS `tax-rate`,
       tip_amount AS `tip-amount`,
       total_cost AS `total-cost`,
       date_added AS `date-added`,
       vendor_name AS `vendor-name`,
       given_name AS `given-name`,
       surname
  FROM transactions AS t
  JOIN vendors AS v
    ON t.vendor_id = v.vendor_id
  JOIN users AS u
    ON t.credit_to = u.user_id
/*~ (when (:last-id params) */
 WHERE transaction_id < :last-id
/*~ ) ~*/
 ORDER BY transaction_id
  DESC
 LIMIT
       /*~ (if (:r-limit params) */
       :r-limit
       /*~*/
       10
       /*~ ) ~*/

-- :name select-transaction-items :?
SELECT t_i.t_item_id AS `item-id`,
       item_name AS `item-name`,
       item_price AS `item-price`,
       item_taxable AS `item-taxable`,
       item_quantity AS `item-quantity`
  FROM transaction_items AS t_i
  JOIN items AS i
    ON t_i.item_id = i.item_id
 WHERE t_i.transaction_id = :transaction-id
 ORDER BY t_i.t_item_id

-- :name select-transaction-owners :?
SELECT item_id AS `item-id`,
       given_name AS `given-name`,
       surname
  FROM transaction_owners AS t_o
  JOIN users AS u
    ON t_o.user_id = u.user_id
 WHERE t_o.transaction_id = :transaction-id
