USE campus_trade;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `favorites`;
DROP TABLE IF EXISTS `messages`;
DROP TABLE IF EXISTS `ratings`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `product_demands`;
DROP TABLE IF EXISTS `product_images`;
DROP TABLE IF EXISTS `user_addresses`;
DROP TABLE IF EXISTS `settlement_order`;
DROP TABLE IF EXISTS `refund_order`;
DROP TABLE IF EXISTS `account_freeze_record`;
DROP TABLE IF EXISTS `payment_order`;
DROP TABLE IF EXISTS `recharge_order`;
DROP TABLE IF EXISTS `account_flow`;
DROP TABLE IF EXISTS `account`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `product_embeddings`;
DROP TABLE IF EXISTS `product_risks`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `meetup_location`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT,
                        `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名 (学号)',
                        `password` VARCHAR(255) NOT NULL COMMENT '密码 (加密存储)',
                        `nickname` VARCHAR(50) COMMENT '昵称',
                        `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色 (USER, ADMIN)',
                        `credit_score` INT NOT NULL DEFAULT 100 COMMENT '用户信誉分',
                        `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                        `status` TINYINT DEFAULT 1 COMMENT '账户状态 (1:正常 0:禁用)',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `category` (
                            `id` INT AUTO_INCREMENT,
                            `name` VARCHAR(50) NOT NULL,
                            `sort_order` INT DEFAULT 0,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE `meetup_location` (
                                   `id` INT AUTO_INCREMENT,
                                   `name` VARCHAR(100) NOT NULL,
                                   `description` VARCHAR(255) DEFAULT NULL,
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园交易地点表';

CREATE TABLE `product` (
                           `id` BIGINT AUTO_INCREMENT,
                           `seller_id` BIGINT NOT NULL,
                           `category_id` INT NOT NULL,
                           `title` VARCHAR(100) NOT NULL,
                           `description` TEXT,
                           `price` DECIMAL(18, 2) NOT NULL,
                           `condition_level` TINYINT COMMENT '新旧程度 (1-5)',
                           `cover_image` VARCHAR(255) NOT NULL,
                           `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '商品状态 (AVAILABLE, LOCKED, SOLD, DELISTED)',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           KEY `idx_product_status_create_time` (`status`, `create_time`),
                           KEY `idx_product_status_category_create_time` (`status`, `category_id`, `create_time`),
                           KEY `idx_product_status_price_create_time` (`status`, `price`, `create_time`),
                           FOREIGN KEY (`seller_id`) REFERENCES `user`(`id`),
                           FOREIGN KEY (`category_id`) REFERENCES `category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE `orders` (
                          `id` BIGINT AUTO_INCREMENT,
                          `product_id` BIGINT NOT NULL,
                          `buyer_id` BIGINT NOT NULL,
                          `seller_id` BIGINT NOT NULL,
                          `order_status` VARCHAR(30) DEFAULT 'PENDING_PAYMENT',
                          `total_price` DECIMAL(18, 2) NOT NULL,
                          `delivery_method` VARCHAR(50) DEFAULT 'ON_CAMPUS_MEETUP',
                          `meetup_location_id` INT,
                          `meetup_time_slot` VARCHAR(50) DEFAULT NULL,
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          `payment_deadline` DATETIME NULL COMMENT '支付截止时间',
                          PRIMARY KEY (`id`),
                          FOREIGN KEY (`product_id`) REFERENCES `product`(`id`),
                          FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
                          FOREIGN KEY (`seller_id`) REFERENCES `user`(`id`),
                          FOREIGN KEY (`meetup_location_id`) REFERENCES `meetup_location`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `ratings` (
                           `id` BIGINT AUTO_INCREMENT,
                           `order_id` BIGINT NOT NULL,
                           `rater_id` BIGINT NOT NULL,
                           `ratee_id` BIGINT NOT NULL,
                           `score` TINYINT NOT NULL,
                           `comment` TEXT,
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_order_rater` (`order_id`, `rater_id`),
                           FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                           FOREIGN KEY (`rater_id`) REFERENCES `user`(`id`),
                           FOREIGN KEY (`ratee_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评价表';

CREATE TABLE `messages` (
                            `id` BIGINT AUTO_INCREMENT,
                            `sender_id` BIGINT NOT NULL,
                            `receiver_id` BIGINT NOT NULL,
                            `product_id` BIGINT DEFAULT NULL,
                            `content` TEXT NOT NULL,
                            `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            KEY `idx_conversation` (`sender_id`, `receiver_id`),
                            FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`),
                            FOREIGN KEY (`receiver_id`) REFERENCES `user`(`id`),
                            FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户私信表';

CREATE TABLE `favorites` (
                             `id` BIGINT AUTO_INCREMENT,
                             `user_id` BIGINT NOT NULL,
                             `product_id` BIGINT NOT NULL,
                             `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
                             FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                             FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

CREATE TABLE `notifications` (
                                 `id` BIGINT AUTO_INCREMENT,
                                 `user_id` BIGINT NOT NULL COMMENT '通知的接收者ID',
                                 `type` VARCHAR(50) NOT NULL COMMENT '通知类型 (e.g., NEW_ORDER, NEW_MESSAGE)',
                                 `content` VARCHAR(255) NOT NULL COMMENT '通知的简要内容',
                                 `related_id` BIGINT COMMENT '关联的对象ID (如订单ID, 商品ID)',
                                 `related_type` VARCHAR(32) COMMENT '关联对象类型：ORDER / PRODUCT',
                                 `source_event_id` VARCHAR(64) COMMENT '异步投递来源事件ID，用于幂等',
                                 `is_read` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已读',
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_notification_source_event` (`source_event_id`),
                                 KEY `idx_notifications_user_read_time` (`user_id`, `is_read`, `create_time`),
                                 FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

CREATE TABLE `outbox_event` (
    `id` BIGINT AUTO_INCREMENT,
    `event_id` VARCHAR(64) NOT NULL,
    `event_type` VARCHAR(50) NOT NULL,
    `recipient_id` BIGINT NOT NULL,
    `related_id` BIGINT NULL,
    `related_type` VARCHAR(32) NULL,
    `content` VARCHAR(255) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `retry_count` INT NOT NULL DEFAULT 0,
    `next_retry_time` DATETIME NULL,
    `last_error` VARCHAR(500) NULL,
    `published_time` DATETIME NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_id` (`event_id`),
    KEY `idx_outbox_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知可靠投递 Outbox';

CREATE TABLE `product_images` (
                                  `id` BIGINT AUTO_INCREMENT,
                                  `product_id` BIGINT NOT NULL COMMENT '关联的商品ID',
                                  `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
                                  `sort_order` INT DEFAULT 0 COMMENT '图片排序',
                                  PRIMARY KEY (`id`),
                                  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

CREATE TABLE `product_embeddings` (
                                      `product_id` BIGINT NOT NULL,
                                      `embedding` MEDIUMTEXT NOT NULL,
                                      `model` VARCHAR(50) NOT NULL,
                                      `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`product_id`),
                                      FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品向量表';

CREATE TABLE `product_risks` (
                                 `id` BIGINT AUTO_INCREMENT,
                                 `product_id` BIGINT NOT NULL,
                                 `risk_level` VARCHAR(20) NOT NULL,
                                 `reasons` TEXT,
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 KEY `idx_product_risk_product` (`product_id`),
                                 FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品风控记录';

INSERT INTO `category` (`name`, `sort_order`) VALUES ('教材书籍', 1), ('电子产品', 2), ('生活用品', 3), ('代步工具', 4), ('服饰鞋包', 5), ('文具用品', 6), ('其他杂项', 99);
INSERT INTO `meetup_location` (`name`, `description`) VALUES ('图书馆正门', '图书馆正门入口处'), ('第一教学楼', '一教大厅'), ('第一食堂', '一食堂门口'), ('紫荆公寓1号楼', '宿舍楼下');

ALTER TABLE `user`
    ADD COLUMN `email` VARCHAR(255) NULL UNIQUE COMMENT '用户邮箱' AFTER `password`,
    ADD COLUMN `email_verified` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '邮箱是否已验证' AFTER `email`;

ALTER TABLE `user`
    ADD COLUMN `bio` TEXT NULL COMMENT '个人简介' AFTER `credit_score`;

CREATE TABLE `user_addresses` (
                                  `id` BIGINT AUTO_INCREMENT,
                                  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
                                  `recipient_name` VARCHAR(100) NOT NULL COMMENT '收件人姓名',
                                  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
                                  `province` VARCHAR(50) NOT NULL COMMENT '省份',
                                  `city` VARCHAR(50) NOT NULL COMMENT '城市',
                                  `district` VARCHAR(50) NOT NULL COMMENT '区/县',
                                  `detailed_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
                                  `is_default` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认地址',
                                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

ALTER TABLE `orders`
    ADD COLUMN `shipping_address_id` BIGINT NULL COMMENT '收货地址ID' AFTER `meetup_time_slot`,
    ADD COLUMN `shipping_provider` VARCHAR(50) NULL COMMENT '快递公司' AFTER `shipping_address_id`,
    ADD COLUMN `tracking_number` VARCHAR(100) NULL COMMENT '快递单号' AFTER `shipping_provider`,
    ADD CONSTRAINT `fk_order_address` FOREIGN KEY (`shipping_address_id`) REFERENCES `user_addresses`(`id`) ON DELETE SET NULL;

ALTER TABLE `product`
    ADD COLUMN `delivery_options` VARCHAR(100) NOT NULL DEFAULT 'MEETUP' COMMENT '支持的配送方式 (MEETUP,SHIPPING)' AFTER `status`;

CREATE TABLE `account` (
                           `id` BIGINT AUTO_INCREMENT,
                           `user_id` BIGINT NOT NULL,
                           `available_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
                           `frozen_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
                           `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                           `version` INT NOT NULL DEFAULT 0,
                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_account_user` (`user_id`),
                           CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                           CONSTRAINT `ck_account_available` CHECK (`available_balance` >= 0),
                           CONSTRAINT `ck_account_frozen` CHECK (`frozen_balance` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户模拟资金账户';

CREATE TABLE `recharge_order` (
                                  `id` BIGINT AUTO_INCREMENT,
                                  `recharge_no` VARCHAR(40) NOT NULL,
                                  `user_id` BIGINT NOT NULL,
                                  `request_id` VARCHAR(64) NOT NULL,
                                  `amount` DECIMAL(18,2) NOT NULL,
                                  `status` VARCHAR(20) NOT NULL,
                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `success_time` DATETIME NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
                                  UNIQUE KEY `uk_recharge_request` (`user_id`, `request_id`),
                                  KEY `idx_recharge_time` (`create_time`),
                                  CONSTRAINT `fk_recharge_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                                  CONSTRAINT `ck_recharge_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟充值订单';

CREATE TABLE `payment_order` (
                                 `id` BIGINT AUTO_INCREMENT,
                                 `payment_no` VARCHAR(40) NOT NULL,
                                 `order_id` BIGINT NOT NULL,
                                 `buyer_id` BIGINT NOT NULL,
                                 `request_id` VARCHAR(64) NOT NULL,
                                 `amount` DECIMAL(18,2) NOT NULL,
                                 `status` VARCHAR(20) NOT NULL,
                                 `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `paid_time` DATETIME NULL,
                                 `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_payment_no` (`payment_no`),
                                 UNIQUE KEY `uk_payment_order` (`order_id`),
                                 UNIQUE KEY `uk_payment_request` (`buyer_id`, `request_id`),
                                 KEY `idx_payment_time` (`create_time`),
                                 CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                                 CONSTRAINT `fk_payment_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
                                 CONSTRAINT `ck_payment_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额支付订单';

CREATE TABLE `account_freeze_record` (
                                         `id` BIGINT AUTO_INCREMENT,
                                         `freeze_no` VARCHAR(40) NOT NULL,
                                         `order_id` BIGINT NOT NULL,
                                         `payment_no` VARCHAR(40) NOT NULL,
                                         `account_id` BIGINT NOT NULL,
                                         `user_id` BIGINT NOT NULL,
                                         `amount` DECIMAL(18,2) NOT NULL,
                                         `status` VARCHAR(20) NOT NULL,
                                         `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_freeze_no` (`freeze_no`),
                                         UNIQUE KEY `uk_freeze_order` (`order_id`),
                                         CONSTRAINT `fk_freeze_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                                         CONSTRAINT `fk_freeze_account` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
                                         CONSTRAINT `fk_freeze_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                                         CONSTRAINT `ck_freeze_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金冻结记录';

CREATE TABLE `refund_order` (
                                `id` BIGINT AUTO_INCREMENT,
                                `refund_no` VARCHAR(40) NOT NULL,
                                `order_id` BIGINT NOT NULL,
                                `payment_no` VARCHAR(40) NOT NULL,
                                `buyer_id` BIGINT NOT NULL,
                                `amount` DECIMAL(18,2) NOT NULL,
                                `status` VARCHAR(20) NOT NULL,
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `success_time` DATETIME NULL,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_refund_no` (`refund_no`),
                                UNIQUE KEY `uk_refund_order` (`order_id`),
                                KEY `idx_refund_time` (`create_time`),
                                CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                                CONSTRAINT `fk_refund_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
                                CONSTRAINT `ck_refund_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单退款记录';

CREATE TABLE `settlement_order` (
                                    `id` BIGINT AUTO_INCREMENT,
                                    `settlement_no` VARCHAR(40) NOT NULL,
                                    `order_id` BIGINT NOT NULL,
                                    `payment_no` VARCHAR(40) NOT NULL,
                                    `buyer_id` BIGINT NOT NULL,
                                    `seller_id` BIGINT NOT NULL,
                                    `amount` DECIMAL(18,2) NOT NULL,
                                    `status` VARCHAR(20) NOT NULL,
                                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `success_time` DATETIME NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_settlement_no` (`settlement_no`),
                                    UNIQUE KEY `uk_settlement_order` (`order_id`),
                                    KEY `idx_settlement_time` (`create_time`),
                                    CONSTRAINT `fk_settlement_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
                                    CONSTRAINT `fk_settlement_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user`(`id`),
                                    CONSTRAINT `fk_settlement_seller` FOREIGN KEY (`seller_id`) REFERENCES `user`(`id`),
                                    CONSTRAINT `ck_settlement_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单结算记录';

CREATE TABLE `account_flow` (
                                `id` BIGINT AUTO_INCREMENT,
                                `flow_no` VARCHAR(40) NOT NULL,
                                `account_id` BIGINT NOT NULL,
                                `user_id` BIGINT NOT NULL,
                                `business_type` VARCHAR(30) NOT NULL,
                                `business_no` VARCHAR(40) NOT NULL,
                                `available_change` DECIMAL(18,2) NOT NULL,
                                `frozen_change` DECIMAL(18,2) NOT NULL,
                                `available_before` DECIMAL(18,2) NOT NULL,
                                `available_after` DECIMAL(18,2) NOT NULL,
                                `frozen_before` DECIMAL(18,2) NOT NULL,
                                `frozen_after` DECIMAL(18,2) NOT NULL,
                                `remark` VARCHAR(255) NULL,
                                `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_flow_no` (`flow_no`),
                                KEY `idx_flow_user_time` (`user_id`, `create_time`),
                                KEY `idx_flow_business` (`business_no`),
                                CONSTRAINT `fk_flow_account` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
                                CONSTRAINT `fk_flow_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                                CONSTRAINT `ck_flow_available_after` CHECK (`available_after` >= 0),
                                CONSTRAINT `ck_flow_frozen_after` CHECK (`frozen_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变账户资金流水';

INSERT IGNORE INTO `account` (`user_id`)
SELECT `id` FROM `user`;
DROP TABLE IF EXISTS `product_demands`;
CREATE TABLE `product_demands` (
                                   `id` BIGINT AUTO_INCREMENT,
                                   `user_id` BIGINT NOT NULL COMMENT '需求发布用户ID',
                                   `category_id` INT DEFAULT NULL COMMENT '期望分类',
                                   `keyword` VARCHAR(100) DEFAULT NULL COMMENT '关键词',
                                   `min_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '最低价',
                                   `max_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '最高价',
                                   `condition_level` TINYINT DEFAULT NULL COMMENT '最低成色要求',
                                   `delivery_options` VARCHAR(100) DEFAULT NULL COMMENT '期望配送方式',
                                   `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE,CLOSED)',
                                   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品需求表';
