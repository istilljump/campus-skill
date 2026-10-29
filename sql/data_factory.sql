-- ============================================================
-- CampusSkill 数据工厂（v11 可信交易保障体系 · 公共底座）
--
-- 用途：压测 / 并发测试 / 对账验证共用的批量造数工具。
--   1. proc_factory_users(tag, n)        批量创建用户 + 钱包（懒开户语义等价的直插）
--   2. proc_factory_skillers(tag, n)     批量创建已认证技能者（user + skiller 审核通过）
--   3. proc_factory_orders(tag, n)       批量创建已支付待接单悬赏（含成对钱包流水，保证对账一致）
--   4. proc_factory_wallet_flows(tag, walletUserId, n)  为指定用户生成 n 对充提流水
--
-- 数据标识：所有造数用户 openid 以 'campus_dev_lf{tag}' 规则命名，
--   压测脚本可直接用 code='lf{tag}{i}' 登录（开发态 openid = campus_dev_{code}）。
-- 清理：执行 sql/loadtest_reset.sql（按 openid / 订单号前缀识别，见文件头说明）。
-- ============================================================

USE campus_runner;

DROP PROCEDURE IF EXISTS proc_factory_users;
DELIMITER $$
CREATE PROCEDURE proc_factory_users(IN p_tag VARCHAR(16), IN p_n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  WHILE i <= p_n DO
    -- 幂等：openid 唯一索引（v11）+ NOT EXISTS，重复 CALL 不产生脏数据
    INSERT INTO user (openid, name, campus, credit_score, publish_order_count, create_time)
    SELECT CONCAT('campus_dev_lf', p_tag, 'u', i), CONCAT('压测用户', p_tag, '-', i), '主校区', 100, 0, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE openid = CONCAT('campus_dev_lf', p_tag, 'u', i));
    -- 钱包直插（与懒开户等价，避免并发开户在造数阶段互相等待）
    INSERT INTO wallet_account (user_id, balance, frozen_amount, total_income, total_expense, status, create_time, update_time)
    SELECT u.id, 0, 0, 0, 0, 1, NOW(), NOW() FROM user u
    WHERE u.openid = CONCAT('campus_dev_lf', p_tag, 'u', i)
      AND NOT EXISTS (SELECT 1 FROM wallet_account w WHERE w.user_id = u.id);
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS proc_factory_skillers;
DELIMITER $$
CREATE PROCEDURE proc_factory_skillers(IN p_tag VARCHAR(16), IN p_n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_uid BIGINT;
  CALL proc_factory_users(p_tag, p_n);
  WHILE i <= p_n DO
    SELECT id INTO v_uid FROM user WHERE openid = CONCAT('campus_dev_lf', p_tag, 'u', i);
    INSERT INTO skiller (user_id, name, phone, student_no, campus, college, audit_status, runner_level,
                         daily_order_limit, completed_orders, score, credit_score, status, create_time, update_time)
    SELECT v_uid, CONCAT('压测技能者', p_tag, '-', i), CONCAT('1390000', LPAD(i % 10000, 4, '0')),
           CONCAT('LF', p_tag, i), '主校区', '计算机学院', 1, 1, 100000, 0, 5.0, 100, 1, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM skiller WHERE user_id = v_uid);
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS proc_factory_orders;
DELIMITER $$
CREATE PROCEDURE proc_factory_orders(IN p_tag VARCHAR(16), IN p_n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_uid BIGINT;
  DECLARE v_num VARCHAR(50);
  DECLARE v_reward DECIMAL(10,2);
  -- 悬赏模式订单发布者：取 tag 对应的第一个工厂用户（若不存在则创建）
  CALL proc_factory_users(p_tag, 1);
  SELECT id INTO v_uid FROM user WHERE openid = CONCAT('campus_dev_lf', p_tag, 'u1');
  WHILE i <= p_n DO
    SET v_num = CONCAT('LF', UPPER(HEX(RANDOM_BYTES(4))), i);
    SET v_reward = 20.00 + (i % 30);
    INSERT INTO orders (number, user_id, type_id, mode, title, description, delivery_address, campus,
                        reward_amount, platform_fee, runner_income, status, rework_count, timeout_time,
                        pay_method, pay_status, order_time, pay_time, create_time, update_time)
    VALUES (v_num, v_uid, 1, 1, CONCAT('压测订单', p_tag, '-', i), '数据工厂生成', '压测楼1', '主校区',
            v_reward, v_reward * 0.10, v_reward * 0.90, 2, 0, DATE_ADD(NOW(), INTERVAL 24 HOUR),
            2, 1, NOW(), NOW(), NOW(), NOW());
    -- 成对流水：充值（+悬赏）与支付（-悬赏），保证钱包对账恒等式成立
    INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
    SELECT id, 3, v_reward, NULL, CONCAT('压测充值-', p_tag), NOW() FROM wallet_account WHERE user_id = v_uid;
    INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
    SELECT w.id, 2, v_reward, o.id, CONCAT('压测支付-', v_num), NOW()
    FROM wallet_account w, orders o WHERE w.user_id = v_uid AND o.number = v_num;
    -- 钱包累计收支同步（充值计收入、支付计支出）
    UPDATE wallet_account SET total_income = total_income + v_reward, total_expense = total_expense + v_reward
    WHERE user_id = v_uid;
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS proc_factory_wallet_flows;
DELIMITER $$
CREATE PROCEDURE proc_factory_wallet_flows(IN p_tag VARCHAR(16), IN p_user_idx INT, IN p_n INT)
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_wid BIGINT;
  CALL proc_factory_users(p_tag, p_user_idx);
  SELECT id INTO v_wid FROM wallet_account WHERE user_id = (SELECT id FROM user WHERE openid = CONCAT('campus_dev_lf', p_tag, 'u', p_user_idx));
  WHILE i <= p_n DO
    -- 一充一提成对插入（提现冻结与充值方向相反，净额为 0）
    INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
    VALUES (v_wid, 3, 10.00, NULL, CONCAT('流水工厂充值-', p_tag, '-', i), NOW());
    INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
    VALUES (v_wid, 4, 10.00, NULL, CONCAT('流水工厂提现-', p_tag, '-', i), NOW());
    UPDATE wallet_account SET total_income = total_income + 10, total_expense = total_expense + 10 WHERE id = v_wid;
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

-- ---------- 使用示例 ----------
-- CALL proc_factory_users('b1', 500);        -- 500 个可登录压测用户
-- CALL proc_factory_skillers('b1', 200);     -- 200 个已认证技能者（code= lfb1u{i} 登录）
-- CALL proc_factory_orders('b1', 1000);      -- 1000 张已支付待接单悬赏
-- CALL proc_factory_wallet_flows('f1', 9, 2000); -- 给第9个用户造 4000 条流水（对账压测）
