-- =====================================================================
-- upgrade_v4_skill.sql —— CampusSkill 校园技能工坊改造脚本
-- 基于 v3（含 message 表）库执行：先跑 upgrade_v3_message.sql，再跑本脚本
-- 内容：
--   1) 表改名：errand_type→skill_category, runner→skiller, runner_audit→skiller_audit
--   2) orders 扩展交付/验收/返修/仲裁字段，skiller 扩展信用分/作品定级字段
--   3) 新增 5 张表：portfolio / service_item / booking / credit_log / dispute
--   4) 技能类目种子数据（替换原跑腿类目）
-- =====================================================================

-- ---------------------------------------------------------------- 1. 表改名
RENAME TABLE `errand_type` TO `skill_category`;
RENAME TABLE `runner` TO `skiller`;
RENAME TABLE `runner_audit` TO `skiller_audit`;

-- ---------------------------------------------------------------- 2. 字段扩展
-- 订单表：交付物、验收、返修、预约模式
ALTER TABLE `orders`
  MODIFY COLUMN `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待支付 2待接单 3进行中 4已交付 5已完成 6已取消 7已超时 8返修中 9仲裁中',
  ADD COLUMN `mode` tinyint NOT NULL DEFAULT '1' COMMENT '撮合模式 1悬赏 2服务预约' AFTER `type_id`,
  ADD COLUMN `service_item_id` bigint DEFAULT NULL COMMENT '关联服务货架id（预约模式）' AFTER `mode`,
  ADD COLUMN `deliverable_url` varchar(500) DEFAULT NULL COMMENT '交付物地址（文件/图片URL）' AFTER `delivery_address`,
  ADD COLUMN `deliverable_note` varchar(500) DEFAULT NULL COMMENT '交付说明' AFTER `deliverable_url`,
  ADD COLUMN `deliver_time` datetime DEFAULT NULL COMMENT '交付时间' AFTER `pickup_time`,
  ADD COLUMN `rework_count` int NOT NULL DEFAULT '0' COMMENT '返修次数' AFTER `deliver_time`,
  ADD COLUMN `auto_accept_time` datetime DEFAULT NULL COMMENT '自动验收截止时间' AFTER `rework_count`,
  ADD KEY `idx_orders_service_item_id` (`service_item_id`);

-- 技能者表：信用分、作品定级
ALTER TABLE `skiller`
  COMMENT='技能者表',
  MODIFY COLUMN `runner_level` tinyint NOT NULL DEFAULT '1' COMMENT '接单等级 1普通 2铜牌 3银牌 4金牌',
  ADD COLUMN `credit_score` int NOT NULL DEFAULT '100' COMMENT '技能信用分（C3≥90 C2≥70 C1<70）' AFTER `score`,
  ADD COLUMN `skill_level` tinyint DEFAULT NULL COMMENT '作品定级 1 C1 2 C2 3 C3（作品审核通过后由管理员评定）' AFTER `credit_score`;

-- 类目表改注释
ALTER TABLE `skill_category` COMMENT='技能类目表';
ALTER TABLE `skiller_audit` COMMENT='技能者认证审核表';

-- 评价表：质量分与信用联动字段
ALTER TABLE `order_review`
  ADD COLUMN `quality_score` tinyint DEFAULT NULL COMMENT '交付质量分 1-5（与准时评分分开）' AFTER `score`;

-- ---------------------------------------------------------------- 3. 新增表
-- 3.1 技能作品集（作品认证的材料载体）
CREATE TABLE `portfolio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `skiller_id` bigint NOT NULL COMMENT '技能者id',
  `title` varchar(64) NOT NULL COMMENT '作品标题',
  `category_id` bigint DEFAULT NULL COMMENT '所属技能类目id',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `work_urls` varchar(1000) DEFAULT NULL COMMENT '作品文件URL列表，逗号分隔',
  `description` varchar(500) DEFAULT NULL COMMENT '作品说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_opinion` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_portfolio_skiller_id` (`skiller_id`),
  KEY `idx_portfolio_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能作品集';

-- 3.2 技能服务货架（技能者挂牌的可预约服务）
CREATE TABLE `service_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `skiller_id` bigint NOT NULL COMMENT '技能者id',
  `category_id` bigint NOT NULL COMMENT '技能类目id',
  `title` varchar(64) NOT NULL COMMENT '服务标题，如：课程PPT美化（20页内）',
  `description` varchar(500) DEFAULT NULL COMMENT '服务说明（含交付物形式与修改轮次）',
  `price` decimal(10,2) NOT NULL COMMENT '挂牌价',
  `delivery_days` tinyint NOT NULL DEFAULT '3' COMMENT '承诺交付天数',
  `service_mode` tinyint NOT NULL DEFAULT '1' COMMENT '服务形式 1线上交付 2线下进行',
  `tags` varchar(255) DEFAULT NULL COMMENT '技能标签，逗号分隔',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0下架 1上架 2封禁',
  `sales_count` int NOT NULL DEFAULT '0' COMMENT '累计成交数',
  `avg_score` decimal(3,1) NOT NULL DEFAULT '5.0' COMMENT '服务均分',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_item_skiller_id` (`skiller_id`),
  KEY `idx_service_item_category_id` (`category_id`),
  KEY `idx_service_item_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能服务货架';

-- 3.3 服务预约单（服务预约模式）
CREATE TABLE `booking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `service_item_id` bigint NOT NULL COMMENT '服务id',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id（支付后回填）',
  `user_id` bigint NOT NULL COMMENT '预约用户id',
  `skiller_id` bigint NOT NULL COMMENT '技能者id',
  `expect_time` datetime DEFAULT NULL COMMENT '期望开始时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '需求备注',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待确认 1已确认 2已完成 3已拒绝 4已取消',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_booking_service_item_id` (`service_item_id`),
  KEY `idx_booking_user_id` (`user_id`),
  KEY `idx_booking_skiller_id` (`skiller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务预约单';

-- 3.4 技能者信用分流水
CREATE TABLE `credit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `skiller_id` bigint NOT NULL COMMENT '技能者id',
  `delta` int NOT NULL COMMENT '分值变动（正加负减）',
  `reason` varchar(64) DEFAULT NULL COMMENT '变动原因，如：按时交付+1/返修-3/超时交付-5/仲裁判责-10',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_credit_log_skiller_id` (`skiller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能者信用分流水';

-- 3.5 纠纷仲裁工单
CREATE TABLE `dispute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `raised_by` bigint NOT NULL COMMENT '发起方用户id',
  `reason_type` tinyint DEFAULT NULL COMMENT '纠纷类型 1质量不符 2延期 3其他',
  `description` varchar(500) DEFAULT NULL COMMENT '纠纷描述',
  `evidence_urls` varchar(1000) DEFAULT NULL COMMENT '凭证URL列表，逗号分隔',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待仲裁 1仲裁-退款用户 2仲裁-放款技能者 3已驳回',
  `verdict` varchar(500) DEFAULT NULL COMMENT '仲裁意见',
  `admin_id` bigint DEFAULT NULL COMMENT '仲裁管理员id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dispute_order_id` (`order_id`),
  KEY `idx_dispute_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='纠纷仲裁工单';

-- ---------------------------------------------------------------- 4. 种子数据
-- 技能类目（替换原跑腿类目：帮我取/帮我送/帮我买/帮我办）
TRUNCATE TABLE `skill_category`;
INSERT INTO `skill_category` (`id`,`name`,`icon`,`description`,`fee_rate`,`sort`,`status`,`create_time`) VALUES
 (1,'PPT美化',NULL,'课程汇报、答辩、路演PPT排版与美化',0.10,1,1,NOW()),
 (2,'海报设计',NULL,'社团招新、活动宣传、晚会海报设计',0.10,2,1,NOW()),
 (3,'视频剪辑',NULL,'短视频剪辑、活动混剪、字幕包装',0.12,3,1,NOW()),
 (4,'摄影约拍',NULL,'人像约拍、活动跟拍、证件照精修',0.10,4,1,NOW()),
 (5,'编程调试',NULL,'课程设计、代码调试、程序讲解辅导',0.10,5,1,NOW()),
 (6,'简历美化',NULL,'简历排版优化、求职文书润色',0.10,6,1,NOW()),
 (7,'翻译润色',NULL,'论文外语摘要润色、资料翻译',0.10,7,1,NOW()),
 (8,'手工定制',NULL,'手绘、手账、礼品定制与代做',0.10,8,1,NOW());
