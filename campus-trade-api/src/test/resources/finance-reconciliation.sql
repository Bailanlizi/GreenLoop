-- GreenLoop 资金与一致性对账 SQL
-- 使用方式：仅在独立测试库执行。设置 @window_start 为本次测试开始时间；
-- 若测试前库已清空，@opening_balance 保持 0.00。
SET @window_start = '2026-01-01 00:00:00';
SET @opening_balance = 0.00;

-- R1 资金守恒：difference 必须为 0.00。
-- 当前实现的退款是“冻结余额 -> 可用余额”的平台内转移，不会减少账户总额，
-- 因而只能作为展示字段，不能计入守恒公式；若未来接入出金退款，才应扣除该出金金额。
SELECT
    ROUND(COALESCE((SELECT SUM(available_balance + frozen_balance) FROM account), 0)
        - @opening_balance
        - COALESCE((SELECT SUM(amount) FROM recharge_order
                    WHERE status = 'SUCCESS' AND success_time >= @window_start), 0), 2) AS difference,
    COALESCE((SELECT SUM(amount) FROM refund_order
              WHERE status = 'SUCCESS' AND success_time >= @window_start), 0) AS internal_refund_amount;

-- R2 支付订单应有且仅有一条冻结记录；金额、付款单号及买家必须一致。
SELECT p.order_id, 'payment-freeze mismatch' AS anomaly
FROM payment_order p
LEFT JOIN account_freeze_record f ON f.order_id = p.order_id
WHERE p.create_time >= @window_start
GROUP BY p.order_id, p.payment_no, p.buyer_id, p.amount
HAVING COUNT(f.id) <> 1
    OR MAX(f.payment_no <> p.payment_no) = 1
    OR MAX(f.user_id <> p.buyer_id) = 1
    OR MAX(f.amount <> p.amount) = 1;

-- R3 终态支付必须与订单终态及退款/结算单一一对应；中间态 FROZEN 不应产生退款或结算。
SELECT p.order_id, 'payment-terminal-chain mismatch' AS anomaly
FROM payment_order p
JOIN orders o ON o.id = p.order_id
LEFT JOIN refund_order r ON r.order_id = p.order_id AND r.status = 'SUCCESS'
LEFT JOIN settlement_order s ON s.order_id = p.order_id AND s.status = 'SUCCESS'
WHERE p.create_time >= @window_start
GROUP BY p.order_id, p.status, o.order_status, p.amount
HAVING (p.status = 'REFUNDED' AND (o.order_status <> 'CANCELLED' OR COUNT(r.id) <> 1 OR COUNT(s.id) <> 0 OR MAX(r.amount <> p.amount) = 1))
    OR (p.status = 'SETTLED' AND (o.order_status <> 'COMPLETED' OR COUNT(s.id) <> 1 OR COUNT(r.id) <> 0 OR MAX(s.amount <> p.amount) = 1))
    OR (p.status = 'FROZEN' AND (COUNT(r.id) <> 0 OR COUNT(s.id) <> 0));

-- R4 流水自身的余额快照必须与变化量一致，且余额不得为负。
SELECT id, flow_no, 'flow snapshot mismatch' AS anomaly
FROM account_flow
WHERE create_time >= @window_start
  AND (available_after <> available_before + available_change
       OR frozen_after <> frozen_before + frozen_change
       OR available_after < 0 OR frozen_after < 0);

-- R5 每个账户当前余额必须与本窗口最后一条流水快照一致。
SELECT a.id AS account_id, 'account-last-flow mismatch' AS anomaly
FROM account a
JOIN (
    SELECT f.account_id, f.available_after, f.frozen_after
    FROM account_flow f
    JOIN (
        SELECT account_id, MAX(id) AS last_id
        FROM account_flow
        WHERE create_time >= @window_start
        GROUP BY account_id
    ) last_flow ON last_flow.last_id = f.id
) f ON f.account_id = a.id
WHERE a.available_balance <> f.available_after OR a.frozen_balance <> f.frozen_after;

-- R6 表级唯一键的防御性验证；两条查询均应返回 0 行。
SELECT buyer_id, request_id, COUNT(*) AS duplicate_count
FROM payment_order
WHERE create_time >= @window_start
GROUP BY buyer_id, request_id
HAVING COUNT(*) > 1;

SELECT user_id, request_id, COUNT(*) AS duplicate_count
FROM recharge_order
WHERE create_time >= @window_start
GROUP BY user_id, request_id
HAVING COUNT(*) > 1;
