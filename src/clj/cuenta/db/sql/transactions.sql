-- :name insert-user :i!
INSERT INTO `users` (`given_name`, `surname`) VALUES :t:new-user

-- :name select-user :? :1
SELECT `id` FROM `users`
WHERE UPPER (`given_name`) = UPPER (:given-name)

-- :name insert-vendor :i!
INSERT INTO `vendors` (`vendor_name`) VALUES (:vendor-name)

-- :name select-vendor :? :1
SELECT `id` FROM `vendors` WHERE UPPER (`vendor_name`) = UPPER (:vendor-name)

-- :name insert-item :i!
INSERT INTO `items` (`item_name`, `item_price`, `item_taxable`, `vendor_id`)
VALUES (:item-name, :item-price, :item-taxable, :vendor-id)

-- :name select-item :? :1
SELECT `id` FROM `items`
WHERE UPPER (`item_name`) = UPPER (:item-name) AND
`vendor_id` = :vendor-id

-- :name insert-transaction :i!
INSERT INTO `transactions` (`tax_rate`, `credit_to`, `vendor_id`)
VALUES (:tax-rate, :credit-to, :vendor-id)

-- :name insert-transaction-item :i!
INSERT INTO `transaction_items` (
    `item_quantity`,
    `item_id`,
    `transaction_id`
    )
VALUES :t:new-item

-- :name insert-transaction-owners :! :n
INSERT INTO `transaction_owners` (
    `user_id`,
    `item_id`,
    `transaction_id`
    )
VALUES :t*:owners
