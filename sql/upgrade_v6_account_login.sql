-- ============================================================
-- CampusSkill 升级脚本 v6：账号密码登录体系（改造清单 A7）
-- 1) user 表新增 username / password / salt 三列
--    密码存储格式：MD5(salt + password)（与提现密码同用 Spring DigestUtils）
-- 2) 为演示账号 demo1~demo4 预置账号密码（账号=昵称拼音码，密码 123456）
--    技能者端账号登录与用户端共用同一账号体系
-- 执行方式：mysql -u root -p campus_runner < upgrade_v6_account_login.sql
-- ============================================================

USE campus_runner;

ALTER TABLE `user`
  ADD COLUMN `username` varchar(32) NULL COMMENT '登录账号（唯一）' AFTER `openid`,
  ADD COLUMN `password` char(32) NULL COMMENT '登录密码（MD5(salt+明文)）' AFTER `username`,
  ADD COLUMN `salt` char(32) NULL COMMENT '密码盐值' AFTER `password`,
  ADD UNIQUE KEY `uk_user_username` (`username`);

-- 演示账号预置（密码 123456；盐值固定便于 SQL 直接计算）
UPDATE `user` SET username = 'demo1', salt = 'campus_skill_demo1', password = MD5(CONCAT('campus_skill_demo1', '123456')) WHERE openid = 'campus_dev_demo1';
UPDATE `user` SET username = 'demo2', salt = 'campus_skill_demo2', password = MD5(CONCAT('campus_skill_demo2', '123456')) WHERE openid = 'campus_dev_demo2';
UPDATE `user` SET username = 'demo3', salt = 'campus_skill_demo3', password = MD5(CONCAT('campus_skill_demo3', '123456')) WHERE openid = 'campus_dev_demo3';
UPDATE `user` SET username = 'demo4', salt = 'campus_skill_demo4', password = MD5(CONCAT('campus_skill_demo4', '123456')) WHERE openid = 'campus_dev_demo4';
