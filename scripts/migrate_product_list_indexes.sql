-- 为已有 GreenLoop 数据库的公开商品列表添加索引；可重复执行。
SET @schema_name = DATABASE();

SET @index_name = 'idx_product_status_create_time';
SET @exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'product' AND index_name = @index_name);
SET @sql = IF(@exists = 0, 'CREATE INDEX idx_product_status_create_time ON product (status, create_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_name = 'idx_product_status_category_create_time';
SET @exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'product' AND index_name = @index_name);
SET @sql = IF(@exists = 0, 'CREATE INDEX idx_product_status_category_create_time ON product (status, category_id, create_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_name = 'idx_product_status_price_create_time';
SET @exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = @schema_name AND table_name = 'product' AND index_name = @index_name);
SET @sql = IF(@exists = 0, 'CREATE INDEX idx_product_status_price_create_time ON product (status, price, create_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
