CREATE TABLE IF NOT EXISTS `users` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `given_name` VARCHAR(16) NOT NULL,
    `surname` VARCHAR(16) NOT NULL,
    PRIMARY KEY (`id`)
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `vendors` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `vendor_name` VARCHAR(16) UNIQUE NOT NULL,
    PRIMARY KEY (`id`)
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `items` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `item_name` VARCHAR(64) NOT NULL,
    `item_price` DOUBLE NOT NULL,
    `item_taxable` BIT(1) NOT NULL DEFAULT 1,
    `vendor_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_i_vendor`
      FOREIGN KEY (`vendor_id`)
      REFERENCES `vendors` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `tax_rate` DOUBLE NOT NULL,
    `credit_to` INT NOT NULL,
    `vendor_id` INT NOT NULL,
    `date_added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_t_vendor`
      FOREIGN KEY (`vendor_id`)
      REFERENCES `vendors` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_t_credit_to`
      FOREIGN KEY (`credit_to`)
      REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `transaction_items` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `item_quantity` INT NOT NULL DEFAULT 1,
    `item_id` INT NOT NULL,
    `transaction_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ti_item`
      FOREIGN KEY (`item_id`)
      REFERENCES `items` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_ti_transaction`
      FOREIGN KEY (`transaction_id`)
      REFERENCES `transactions` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `transaction_owners` (
    `transaction_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `item_id` INT NOT NULL,
    PRIMARY KEY (`transaction_id`, `user_id`, `item_id`),
    CONSTRAINT `fk_to_transaction`
      FOREIGN KEY (`transaction_id`)
      REFERENCES `transactions` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_to_item`
      FOREIGN KEY (`item_id`)
      REFERENCES `transaction_items` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_to_user`
      FOREIGN KEY (`user_id`)
      REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `debt_table` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `date_added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    )
    ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `debts` (
    `amount` DOUBLE NOT NULL,
    `table_id` INT NOT NULL,
    `creditor_id` INT NOT NULL,
    `debtor_id` INT NOT NULL,
    PRIMARY KEY (`table_id`, `creditor_id`, `debtor_id`),
    CONSTRAINT `fk_debt_table`
      FOREIGN KEY (`table_id`)
      REFERENCES `debt_table` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_creditor`
      FOREIGN KEY (`creditor_id`)
      REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `fk_debtor`
      FOREIGN KEY (`debtor_id`)
      REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    ENGINE = InnoDB;
