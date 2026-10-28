-- ============================================================
-- CampusSkill 升级脚本 v6：账号密码登录体系（改造清单 A7）
-- 1) user 表新增 username / password / salt 三列
--    密码存储格式（v10 起为 BCrypt，列宽 72 兼容历史 MD5 口径）
-- 2) 演示账号（demo1~demo4）的账号密码预置已迁移至 seed_demo.sql 末尾
--    技能者端账号登录与用户端共用同一账号体系
-- 执行方式：mysql -u root -p campus_runner < upgrade_v6_account_login.sql
-- ============================================================

USE campus_runner;

ALTER TABLE `user`
  ADD COLUMN `username` varchar(32) NULL COMMENT '登录账号（唯一）' AFTER `openid`,
  ADD COLUMN `password` varchar(72) NULL COMMENT '登录密码（BCrypt，兼容历史 MD5）' AFTER `username`,
  ADD COLUMN `salt` char(32) NULL COMMENT '密码盐值（历史 MD5 口径使用）' AFTER `password`,
  ADD UNIQUE KEY `uk_user_username` (`username`);

-- 演示账号（demo1~demo4）的用户名/密码回填已迁移至 seed_demo.sql 末尾：
-- 全新库导入时 user 表为空，回填必须在种子数据之后执行才能生效。
