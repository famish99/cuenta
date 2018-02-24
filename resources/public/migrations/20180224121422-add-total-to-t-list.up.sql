ALTER TABLE transactions
  ADD COLUMN total_cost DOUBLE NOT NULL AFTER tip_amount
--;;
UPDATE transactions as t
  JOIN (SELECT t.transaction_id,
               SUM(item_quantity * item_price *
                   (CASE item_taxable
                    WHEN 1 THEN tax_rate / 100 + 1
                    ELSE 1
                    END)) as total_cost
          FROM transactions as t
          JOIN transaction_items as t_i
            ON t.transaction_id = t_i.transaction_id
          JOIN items as i
            ON t_i.item_id = i.item_id
         GROUP BY t.transaction_id) as n
    ON t.transaction_id = n.transaction_id
   SET t.total_cost = n.total_cost


