CREATE TABLE IF NOT EXISTS `users` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `given_name` VARCHAR(16) NOT NULL,
    `surname` VARCHAR(16) NOT NULL,
    PRIMARY KEY (`id`)
    )
    --ENGINE = InnoDB;
--;;
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` INT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`)
    )
    --ENGINE = InnoDB;
--;;
CREATE TABLE `debts` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `amount` DOUBLE NOT NULL,
    `creditor_id` INT NOT NULL,
    `debtor_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `creditor`
      FOREIGN KEY (`creditor_id`)
      REFERENCES `users`(`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION,
    CONSTRAINT `debtor`
      FOREIGN KEY (`debtor_id`)
      REFERENCES `users` (`id`)
      ON DELETE CASCADE
      ON UPDATE NO ACTION
    )
    --ENGINE = InnoDB;
