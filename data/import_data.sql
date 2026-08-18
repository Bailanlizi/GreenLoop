-- ============================================
-- GreenLoop 性能测试数据导入脚本（本地 MySQL）
-- 数据库：campus_trade_test
-- 使用方法：
--   1. 将所有 CSV 文件放到 D:\Project\GreenLoop\data\
--   2. MySQL 执行：SET GLOBAL local_infile = 1;
--   3. 连接时加参数：mysql --local-infile=1 -u root -p
--   4. 执行：SOURCE D:/Project/GreenLoop/data/import_data.sql;
-- ============================================

-- 切换到测试数据库
USE campus_trade_test;

-- 开启本地文件导入权限
SET GLOBAL local_infile = 1;

-- 禁用外键检查（避免导入顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- 设置字符集
SET NAMES utf8mb4;

-- ============================================
-- 1. 导入商品分类 CATEGORY
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/category.csv'
INTO TABLE `category`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `name`, `sort_order`);

-- ============================================
-- 2. 导入面交地点 MEETUP_LOCATION
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/meetup_location.csv'
INTO TABLE `meetup_location`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `name`, `description`);

-- ============================================
-- 3. 导入用户 USER
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/user.csv'
INTO TABLE `user`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `username`, `password`, `email`, `email_verified`, `nickname`, `role`, `credit_score`, `bio`, `avatar`, `status`, `create_time`);

-- ============================================
-- 4. 导入收货地址 USER_ADDRESSES
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/user_addresses.csv'
INTO TABLE `user_addresses`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `user_id`, `recipient_name`, `phone`, `province`, `city`, `district`, `detailed_address`, `is_default`, `create_time`);

-- ============================================
-- 5. 导入商品 PRODUCT
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/product.csv'
INTO TABLE `product`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `condition_level`, `cover_image`, `status`, `delivery_options`, `create_time`, `update_time`);

-- ============================================
-- 6. 导入订单 ORDERS（处理空值）
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/orders.csv'
INTO TABLE `orders`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `product_id`, `buyer_id`, `seller_id`, `order_status`, `total_price`, `delivery_method`, `meetup_location_id`, `meetup_time_slot`, `shipping_address_id`, `shipping_provider`, `tracking_number`, `create_time`)
SET
  `meetup_location_id` = NULLIF(`meetup_location_id`, ''),
  `meetup_time_slot` = NULLIF(`meetup_time_slot`, ''),
  `shipping_address_id` = NULLIF(`shipping_address_id`, ''),
  `shipping_provider` = NULLIF(`shipping_provider`, ''),
  `tracking_number` = NULLIF(`tracking_number`, '');

-- ============================================
-- 7. 导入虚拟账户 ACCOUNT
-- ============================================
LOAD DATA LOCAL INFILE 'D:/Project/GreenLoop/data/account.csv'
INTO TABLE `account`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(`id`, `user_id`, `available_balance`, `frozen_balance`, `status`, `version`, `create_time`, `update_time`);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 验证导入结果
-- ============================================
SELECT 'category' AS table_name, COUNT(*) AS count FROM `category`
UNION ALL
SELECT 'meetup_location', COUNT(*) FROM `meetup_location`
UNION ALL
SELECT 'user', COUNT(*) FROM `user`
UNION ALL
SELECT 'user_addresses', COUNT(*) FROM `user_addresses`
UNION ALL
SELECT 'product', COUNT(*) FROM `product`
UNION ALL
SELECT 'orders', COUNT(*) FROM `orders`
UNION ALL
SELECT 'account', COUNT(*) FROM `account`;