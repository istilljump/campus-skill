-- ============================================================
-- CampusSkill 升级脚本 v11：可信交易保障体系（对账 + 风控 + 幂等）
-- 1) reconcile_record  资金对账差异记录（对账定时任务每轮落库，仅记差异行）
-- 2) risk_control_log  风控告警记录（规则引擎命中即落库，管理端可处置）
-- 3) wallet_transaction 增加按钱包维度索引（对账聚合查询提速）
-- 幂等令牌基于 Redis（campus:idem:*），无表结构变更。
-- 执行方式（目标库由命令行参数指定，脚本内不写死 USE）：
--   开发库：mysql -u root -p --default-character-set=utf8mb4 campus_runner < upgrade_v11_trust.sql
--   测试库：mysql -u root -p --default-character-set=utf8mb4 campus_runner_test < upgrade_v11_trust.sql
-- 可重复执行。
-- ============================================================

-- ---------- 1. 资金对账差异记录 ----------
CREATE TABLE IF NOT EXISTS `reconcile_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(32) NOT NULL COMMENT '对账批次号（一轮一个批次）',
  `wallet_id` bigint NOT NULL COMMENT '钱包id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `balance` decimal(12,2) NOT NULL COMMENT '当前余额',
  `expected_balance` decimal(12,2) NOT NULL COMMENT '按流水重算的期望余额',
  `diff_amount` decimal(12,2) NOT NULL COMMENT '差异金额 = 余额 - 期望余额',
  `tx_count` int NOT NULL DEFAULT '0' COMMENT '该钱包流水笔数',
  `checked_wallets` int NOT NULL DEFAULT '0' COMMENT '本轮对账钱包总数（批次冗余，便于查询展示）',
  `create_time` datetime NOT NULL COMMENT '对账时间',
  PRIMARY KEY (`id`),
  KEY `idx_reconcile_batch` (`batch_no`),
  KEY `idx_reconcile_wallet` (`wallet_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金对账差异记录（仅记余额与流水不一致的钱包）';

-- ---------- 2. 对账批次汇总（每轮必落一行，含 0 差异轮；大屏/对账中心汇总口径） ----------
CREATE TABLE IF NOT EXISTS `reconcile_batch` (
  `batch_no` varchar(32) NOT NULL COMMENT '对账批次号',
  `checked_wallets` int NOT NULL COMMENT '本轮检查钱包总数',
  `mismatch_count` int NOT NULL COMMENT '本轮差异钱包数',
  `all_matched` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否全部一致',
  `create_time` datetime NOT NULL COMMENT '对账时间',
  PRIMARY KEY (`batch_no`),
  KEY `idx_reconcile_batch_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金对账批次汇总（每轮一行）';

-- ---------- 3. 风控告警记录 ----------
CREATE TABLE IF NOT EXISTS `risk_control_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_code` varchar(32) NOT NULL COMMENT '规则编码，如 R001',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名称，如：高频发单',
  `risk_level` tinyint NOT NULL COMMENT '风险等级 1低 2中 3高',
  `target_type` tinyint NOT NULL COMMENT '对象类型 1用户 2技能者',
  `target_id` bigint NOT NULL COMMENT '对象id（target_type 对应的 user.id / skiller.id）',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id（可空）',
  `detail` varchar(500) DEFAULT NULL COMMENT '命中详情（人类可读）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '处置状态 0未处置 1已处置',
  `handle_remark` varchar(255) DEFAULT NULL COMMENT '处置备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处置时间',
  `create_time` datetime NOT NULL COMMENT '命中时间',
  PRIMARY KEY (`id`),
  KEY `idx_risk_status` (`status`, `create_time`),
  KEY `idx_risk_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风控规则命中记录';

-- ---------- 4. 安全加固：openid 唯一索引 ----------
-- 开发态登录以 openid 定位用户，openid 重复意味着令牌可能命中他人账号。
-- 微信注册用户 openid 唯一；账号注册用户 openid 为 acct_ 前缀唯一占位；存量无重复（仅允许 NULL 多行）。
SET @dup := (SELECT COUNT(*) FROM (SELECT openid FROM `user` WHERE openid IS NOT NULL GROUP BY openid HAVING COUNT(*) > 1) t);
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_openid');
SET @ddl := IF(@dup = 0 AND @idx = 0,
    'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_openid` (`openid`)',
    IF(@idx > 0, 'SELECT ''uk_user_openid 已存在'' AS note',
       'SELECT CONCAT(''存在重复 openid '', @dup, '' 组，请先人工去重再执行升级'') AS warning'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 5. 流水表按钱包维度索引（对账聚合） ----------
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_transaction' AND INDEX_NAME = 'idx_wallet_tx_wallet');
SET @ddl := IF(@idx = 0,
    'ALTER TABLE `wallet_transaction` ADD INDEX `idx_wallet_tx_wallet` (`wallet_id`, `type`)',
    'SELECT ''idx_wallet_tx_wallet 已存在'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
