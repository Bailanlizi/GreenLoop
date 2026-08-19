-- ============================================
-- GreenLoop 支付压测数据重置脚本（压测库专用）
-- 用法：每轮压测前执行一次，将 JMeter 数据集恢复到初始状态
--   mysql --local-infile=1 -u root -p < scripts/reset_pay_test_data.sql
--
-- 【作用范围】只重置 JMeter 压测数据集：
--   用户 1-1000（testuser_*）、订单 100001-110000
-- 不触碰：perf/k6 夹具（990001/990002 及其 10 万余额）、
--         管理员账号、并发测试夹具（900001/900002，其清理由测试自身 setUp 负责）
-- ============================================
USE campus_trade_test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清空 JMeter 订单的资金单据与不可变流水（按订单号段限定范围）
DELETE FROM account_flow          WHERE user_id BETWEEN 1 AND 1000;
DELETE FROM settlement_order      WHERE order_id BETWEEN 100001 AND 110000;
DELETE FROM refund_order          WHERE order_id BETWEEN 100001 AND 110000;
DELETE FROM account_freeze_record WHERE order_id BETWEEN 100001 AND 110000;
DELETE FROM payment_order         WHERE order_id BETWEEN 100001 AND 110000;
DELETE FROM recharge_order        WHERE user_id BETWEEN 1 AND 1000;

-- 2. 恢复 JMeter 订单为待支付状态，清空履约痕迹，刷新支付期限（NOW+30min）
UPDATE orders
SET order_status = 'PENDING_PAYMENT',
    shipping_provider = NULL,
    tracking_number = NULL,
    payment_deadline = DATE_ADD(NOW(), INTERVAL 30 MINUTE)
WHERE id BETWEEN 100001 AND 110000;

-- 3. 恢复 JMeter 测试账户余额与乐观锁版本（不动 perf-buyer 的 100000 余额）
UPDATE account SET available_balance = 5000.00, frozen_balance = 0.00, version = 1
WHERE user_id BETWEEN 1 AND 1000;

SET FOREIGN_KEY_CHECKS = 1;

-- 4. 校验结果
SELECT 'JMeter 订单待支付' AS info, COUNT(*) AS cnt FROM orders
WHERE id BETWEEN 100001 AND 110000 AND order_status = 'PENDING_PAYMENT' AND payment_deadline > NOW();
SELECT 'JMeter 支付单已清空' AS info, COUNT(*) AS cnt FROM payment_order
WHERE order_id BETWEEN 100001 AND 110000;
SELECT 'JMeter 账户余额已恢复' AS info, COUNT(*) AS cnt FROM account
WHERE user_id BETWEEN 1 AND 1000 AND available_balance = 5000.00 AND frozen_balance = 0.00;
SELECT 'perf 夹具余额不受影响' AS info, available_balance AS cnt FROM account WHERE user_id = 990001;
