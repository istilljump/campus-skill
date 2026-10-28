-- ============================================================
-- CampusSkill 升级脚本 v10：安全加固（BCrypt 密码口径）
-- 1) user.password 由 char(32)（MD5）扩展为 varchar(72)，容纳 60 位 BCrypt 散列
--    存量 MD5 口令无需重置：PasswordUtil 登录时兼容校验并自动重哈希升级
-- 执行方式：mysql -u root -p campus_runner < upgrade_v10_security.sql
-- ============================================================

USE campus_runner;

SET @len := (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'password');
SET @ddl := IF(@len < 72,
    'ALTER TABLE `user` MODIFY COLUMN `password` varchar(72) NULL COMMENT ''登录密码（BCrypt，兼容历史 MD5）''',
    'SELECT ''user.password 已满足 BCrypt 长度'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
