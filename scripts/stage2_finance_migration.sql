-- Run this migration after selecting the target database, for example:
-- mysql -u root -p campus_trade < scripts/stage2_finance_migration.sql

ALTER TABLE `orders`
    ADD COLUMN `payment_deadline` DATETIME NULL COMMENT '支付截止时间' AFTER `create_time`,
    MODIFY COLUMN `order_status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    MODIFY COLUMN `total_price` DECIMAL(18,2) NOT NULL;

ALTER TABLE `product`
    MODIFY COLUMN `price` DECIMAL(18,2) NOT NULL;

CREATE TABLE IF NOT EXISTS `account` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `available_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    `frozen_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `version` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_account_user` (`user_id`),
    CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_account_available` CHECK (`available_balance` >= 0),
    CONSTRAINT `ck_account_frozen` CHECK (`frozen_balance` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户模拟资金账户';

CREATE TABLE IF NOT EXISTS `recharge_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `recharge_no` VARCHAR(40) NOT NULL, `user_id` BIGINT NOT NULL,
    `request_id` VARCHAR(64) NOT NULL, `amount` DECIMAL(18,2) NOT NULL, `status` VARCHAR(20) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `success_time` DATETIME NULL,
    UNIQUE KEY `uk_recharge_no` (`recharge_no`), UNIQUE KEY `uk_recharge_request` (`user_id`, `request_id`),
    KEY `idx_recharge_time` (`create_time`), CONSTRAINT `fk_recharge_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_recharge_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟充值订单';

CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `payment_no` VARCHAR(40) NOT NULL, `order_id` BIGINT NOT NULL,
    `buyer_id` BIGINT NOT NULL, `request_id` VARCHAR(64) NOT NULL, `amount` DECIMAL(18,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `paid_time` DATETIME NULL, `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_payment_no` (`payment_no`), UNIQUE KEY `uk_payment_order` (`order_id`),
    UNIQUE KEY `uk_payment_request` (`buyer_id`, `request_id`), KEY `idx_payment_time` (`create_time`),
    CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
    CONSTRAINT `fk_payment_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_payment_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额支付订单';

CREATE TABLE IF NOT EXISTS `account_freeze_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `freeze_no` VARCHAR(40) NOT NULL, `order_id` BIGINT NOT NULL,
    `payment_no` VARCHAR(40) NOT NULL, `account_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(18,2) NOT NULL, `status` VARCHAR(20) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_freeze_no` (`freeze_no`), UNIQUE KEY `uk_freeze_order` (`order_id`),
    CONSTRAINT `fk_freeze_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
    CONSTRAINT `fk_freeze_account` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
    CONSTRAINT `fk_freeze_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_freeze_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金冻结记录';

CREATE TABLE IF NOT EXISTS `refund_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `refund_no` VARCHAR(40) NOT NULL, `order_id` BIGINT NOT NULL,
    `payment_no` VARCHAR(40) NOT NULL, `buyer_id` BIGINT NOT NULL, `amount` DECIMAL(18,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `success_time` DATETIME NULL,
    UNIQUE KEY `uk_refund_no` (`refund_no`), UNIQUE KEY `uk_refund_order` (`order_id`), KEY `idx_refund_time` (`create_time`),
    CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
    CONSTRAINT `fk_refund_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_refund_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单退款记录';

CREATE TABLE IF NOT EXISTS `settlement_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `settlement_no` VARCHAR(40) NOT NULL, `order_id` BIGINT NOT NULL,
    `payment_no` VARCHAR(40) NOT NULL, `buyer_id` BIGINT NOT NULL, `seller_id` BIGINT NOT NULL,
    `amount` DECIMAL(18,2) NOT NULL, `status` VARCHAR(20) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `success_time` DATETIME NULL,
    UNIQUE KEY `uk_settlement_no` (`settlement_no`), UNIQUE KEY `uk_settlement_order` (`order_id`), KEY `idx_settlement_time` (`create_time`),
    CONSTRAINT `fk_settlement_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
    CONSTRAINT `fk_settlement_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
    CONSTRAINT `fk_settlement_seller` FOREIGN KEY (`seller_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_settlement_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单结算记录';

CREATE TABLE IF NOT EXISTS `account_flow` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `flow_no` VARCHAR(40) NOT NULL, `account_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL, `business_type` VARCHAR(30) NOT NULL, `business_no` VARCHAR(40) NOT NULL,
    `available_change` DECIMAL(18,2) NOT NULL, `frozen_change` DECIMAL(18,2) NOT NULL,
    `available_before` DECIMAL(18,2) NOT NULL, `available_after` DECIMAL(18,2) NOT NULL,
    `frozen_before` DECIMAL(18,2) NOT NULL, `frozen_after` DECIMAL(18,2) NOT NULL,
    `remark` VARCHAR(255) NULL, `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_flow_no` (`flow_no`), KEY `idx_flow_user_time` (`user_id`, `create_time`), KEY `idx_flow_business` (`business_no`),
    CONSTRAINT `fk_flow_account` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
    CONSTRAINT `fk_flow_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    CONSTRAINT `ck_flow_available_after` CHECK (`available_after` >= 0),
    CONSTRAINT `ck_flow_frozen_after` CHECK (`frozen_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变账户资金流水';

INSERT IGNORE INTO `account` (`user_id`)
SELECT `id` FROM `user`;
