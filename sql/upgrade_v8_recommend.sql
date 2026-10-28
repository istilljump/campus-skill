-- ============================================================
-- CampusSkill 升级脚本 v8：个性化推荐引擎
-- 1) user_behavior 用户行为表：记录用户对技能服务的浏览/预约/成交行为，
--    作为推荐算法（ItemCF 协同过滤 + 内容特征加权）的输入信号
-- 执行方式：mysql -u root -p campus_runner < upgrade_v8_recommend.sql
-- 新库无需执行：campus.sql 已含本表
-- ============================================================

USE campus_runner;

CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `service_item_id` bigint NOT NULL COMMENT '技能服务id',
  `category_id` bigint DEFAULT NULL COMMENT '技能类目id（行为发生时冗余）',
  `behavior_type` tinyint NOT NULL COMMENT '行为类型 1浏览 2预约 3成交',
  `source` tinyint NOT NULL DEFAULT '4' COMMENT '行为来源 1市场列表 2猜你喜欢 3看了又看 4其他（推荐效果实验用）',
  `weight` decimal(4,1) NOT NULL DEFAULT '1.0' COMMENT '行为权重（浏览1.0/预约3.0/成交5.0）',
  `create_time` datetime NOT NULL COMMENT '行为时间',
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user_time` (`user_id`, `create_time`),
  KEY `idx_behavior_item` (`service_item_id`),
  KEY `idx_behavior_user_item` (`user_id`, `service_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为记录（推荐引擎数据源）';

-- 已按 v8 初版建过表的旧库：补 source 列（重复执行报 Duplicate column 可忽略）
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_behavior' AND COLUMN_NAME = 'source');
SET @ddl := IF(@col = 0,
    'ALTER TABLE `user_behavior` ADD COLUMN `source` tinyint NOT NULL DEFAULT ''4'' COMMENT ''行为来源 1市场列表 2猜你喜欢 3看了又看 4其他'' AFTER `behavior_type`',
    'SELECT ''user_behavior.source 已存在'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
