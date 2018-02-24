-- :name insert-user :i!
INSERT INTO users (given_name, surname) VALUES :t:new-user

-- :name select-user :? :1
SELECT user_id as id
  FROM users
 WHERE UPPER (given_name) = UPPER (:given-name)

-- :name insert-vendor :i!
INSERT INTO vendors (vendor_name)
VALUES (:vendor-name)

-- :name select-vendor :? :1
SELECT vendor_id as id
  FROM vendors
 WHERE UPPER (vendor_name) = UPPER (:vendor-name)

-- :name insert-item :i!
INSERT INTO items (item_name, item_price, item_taxable, vendor_id)
VALUES (:item-name, :item-price, :item-taxable, :vendor-id)

-- :name select-item :? :1
SELECT item_id as id
  FROM items
 WHERE UPPER (item_name) = UPPER (:item-name)
   AND vendor_id = :vendor-id

-- :name insert-transaction :i!
INSERT INTO transactions (tax_rate, tip_amount, total_cost, credit_to, vendor_id)
VALUES (:tax-rate, :tip-amount, :total-cost, :credit-to, :vendor-id)

-- :name insert-transaction-item :i!
INSERT INTO transaction_items (item_quantity, item_id, transaction_id)
VALUES :t:new-item

-- :name insert-transaction-owners :!
INSERT INTO transaction_owners (user_id, item_id, transaction_id)
VALUES :t*:owners

-- :name insert-debt-version :i!
INSERT INTO debt_table () VALUES ()

-- :name insert-debts :!
INSERT INTO debts (amount, table_id, creditor_id, debtor_id)
VALUES :t*:debts

-- :name select-debts :?
SELECT creditor, u.given_name AS debtor, amount
  FROM (SELECT u.given_name AS creditor, d.debtor_id, d.amount
          FROM debts AS d
               JOIN users AS u
               ON d.creditor_id = u.user_id
         WHERE table_id =
               /*~ (if (:table-id params) */
               :table-id
               /*~*/
               (SELECT MAX(d_table_id) FROM debt_table)
               /*~ ) ~*/
	   ) AS c_join
  JOIN users AS u
    ON c_join.debtor_id = u.user_id

-- :name select-transactions :?
SELECT transaction_id,
       tax_rate as `tax-rate`,
       tip_amount as `tip-amount`,
       total_cost as `total-cost`,
       date_added as `date-added`,
       vendor_name as `vendor-name`,
       given_name as `given-name`,
       surname
  FROM transactions as t
  JOIN vendors as v
    ON t.vendor_id = v.vendor_id
  JOIN users as u
    ON t.credit_to = u.user_id
 ORDER BY transaction_id
  DESC
 LIMIT 10
