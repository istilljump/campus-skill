-- ============================================================
-- CampusSkill 升级脚本 v5：管理端功能增强
-- 1) announcement 系统公告表：管理端广播公告的主记录
--    （广播时同步写入 user/skiller 收件箱 message 表并 WebSocket 推送）
-- 执行方式：mysql -u root -p campus_runner < upgrade_v5_admin_console.sql
-- ============================================================

USE campus_runner;

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(64) NOT NULL COMMENT '公告标题',
  `content` varchar(500) DEFAULT NULL COMMENT '公告内容',
  `recipient_count` int NOT NULL DEFAULT '0' COMMENT '送达人数（用户+技能者去重）',
  `admin_id` bigint DEFAULT NULL COMMENT '发布管理员id',
  `admin_name` varchar(32) DEFAULT NULL COMMENT '发布管理员姓名',
  `create_time` datetime NOT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_announcement_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告';
