-- ============================================================
-- CampusSkill 压测数据清理（与 sql/data_factory.sql 配套）
--
-- 识别规则（只清理数据工厂产生的数据，不碰真实/演示数据）：
--   用户与技能者：user.openid LIKE 'campus_dev_lf%'
--   订单：orders.number LIKE 'LF%' 且 title LIKE '压测订单%'
--   流水：remark LIKE '压测%' / '流水工厂%'
-- 使用：mysql -u root -p campus_runner < loadtest_reset.sql
-- ============================================================

USE campus_runner;

-- 先清流水（依赖 wallet_id / order_id 外键语义）
DELETE wt FROM wallet_transaction wt
WHERE wt.remark LIKE '压测%' OR wt.remark LIKE '流水工厂%';

-- 订单（压测订单号固定 LF 前缀 + 标题标识双保险）
DELETE o FROM orders o
WHERE o.number LIKE 'LF%' AND o.title LIKE '压测订单%';

-- 提现申请 / 风控 / 对账（按 remark 或对象用户清理）
DELETE wr FROM withdraw_request wr
JOIN user u ON u.id = wr.user_id WHERE u.openid LIKE 'campus_dev_lf%';

DELETE rcl FROM risk_control_log rcl
JOIN user u ON u.id = rcl.target_id AND rcl.target_type = 1 WHERE u.openid LIKE 'campus_dev_lf%';

DELETE rr FROM reconcile_record rr
JOIN user u ON u.id = rr.user_id WHERE u.openid LIKE 'campus_dev_lf%';

-- 技能者（先 skiller 后 user，user_id 依赖）
DELETE s FROM skiller s
JOIN user u ON u.id = s.user_id WHERE u.openid LIKE 'campus_dev_lf%';

-- 钱包
DELETE w FROM wallet_account w
JOIN user u ON u.id = w.user_id WHERE u.openid LIKE 'campus_dev_lf%';

-- 用户
DELETE FROM user WHERE openid LIKE 'campus_dev_lf%';

SELECT
  (SELECT COUNT(*) FROM user WHERE openid LIKE 'campus_dev_lf%')        AS 残留用户,
  (SELECT COUNT(*) FROM orders WHERE number LIKE 'LF%')                 AS 残留订单,
  (SELECT COUNT(*) FROM wallet_transaction WHERE remark LIKE '压测%')   AS 残留流水;
