-- ============================================
-- GreenLoop 支付压测数据重置脚本（压测库专用）
-- 用法：每轮压测前执行一次，将数据恢复到初始状态
--   mysql --local-infile=1 -u root -p campus_trade_test < scripts/reset_pay_test_data.sql
-- 注意：会清空压测库所有资金单据/流水，请勿在生产库执行！
-- ============================================
USE campus_trade_test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清空资金单据与不可变流水（压测库专用于测试，直接删除）
DELETE FROM account_flow;
DELETE FROM settlement_order;
DELETE FROM refund_order;
DELETE FROM account_freeze_record;
DELETE FROM payment_order;

-- 2. 恢复所有订单为待支付状态，清空履约痕迹，刷新支付期限（NOW+30min）
UPDATE orders
SET order_status = 'PENDING_PAYMENT',
    shipping_provider = NULL,
    tracking_number = NULL,
    payment_deadline = DATE_ADD(NOW(), INTERVAL 30 MINUTE)
WHERE order_status <> 'PENDING_PAYMENT' OR payment_deadline IS NULL OR payment_deadline <= NOW();

-- 3. 恢复所有账户余额与乐观锁版本
UPDATE account SET available_balance = 5000.00, frozen_balance = 0.00, version = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- 4. 校验结果
SELECT 'orders 待支付' AS info, COUNT(*) AS cnt FROM orders WHERE order_status = 'PENDING_PAYMENT' AND payment_deadline > NOW();
SELECT 'payment_order 已清空' AS info, COUNT(*) AS cnt FROM payment_order;
SELECT 'account 余额已恢复' AS info, COUNT(*) AS cnt FROM account WHERE available_balance = 5000.00 AND frozen_balance = 0.00;
