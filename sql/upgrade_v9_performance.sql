-- ============================================================
-- CampusSkill 升级脚本 v9：性能优化
-- 1) orders 表补充技能者维度索引：加速「每日接单上限」统计与技能者订单维度查询
--    （抢单条件更新走主键，无需额外索引；此处补齐权限校验里 count 查询的扫描路径）
-- 执行方式：mysql -u root -p campus_runner < upgrade_v9_performance.sql
-- ============================================================

USE campus_runner;

-- 每日接单上限统计：where runner_id=? and status in(3,4) and date(update_time)=curdate()
SET @stat := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_runner_status_time');
SET @sql := IF(@stat = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_orders_runner_status_time` (`runner_id`, `status`, `update_time`)',
    'SELECT ''idx_orders_runner_status_time 已存在'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
