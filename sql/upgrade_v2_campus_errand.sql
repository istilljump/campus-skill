-- ============================================================
-- 校园综合跑腿服务平台 - 阶段2 数据库结构改造
-- 删除外卖专属表，改造保留表，新增跑腿业务表，重构订单主表
-- ============================================================
USE campus_runner;

-- 1. 删除外卖专属业务表
DROP TABLE IF EXISTS shopping_cart;
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS setmeal_dish;
DROP TABLE IF EXISTS setmeal;
DROP TABLE IF EXISTS dish_flavor;
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS category;

-- 2. 用户表扩充：学号、校区、信用分、累计发单数
ALTER TABLE `user`
    ADD COLUMN `student_no` varchar(20) DEFAULT NULL COMMENT '学号' AFTER `avatar`,
    ADD COLUMN `campus` varchar(50) DEFAULT NULL COMMENT '校区' AFTER `student_no`,
    ADD COLUMN `credit_score` int NOT NULL DEFAULT '100' COMMENT '信用分' AFTER `campus`,
    ADD COLUMN `publish_order_count` int NOT NULL DEFAULT '0' COMMENT '累计发单数' AFTER `credit_score`;

-- 3. 地址簿表扩充：校区、楼栋、房间号
ALTER TABLE `address_book`
    ADD COLUMN `campus` varchar(50) DEFAULT NULL COMMENT '校区' AFTER `detail`,
    ADD COLUMN `building` varchar(100) DEFAULT NULL COMMENT '楼栋' AFTER `campus`,
    ADD COLUMN `room` varchar(50) DEFAULT NULL COMMENT '房间号' AFTER `building`;

-- 4. 跑腿员表（原骑手业务实体，平台认证后生效）
CREATE TABLE `runner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '关联用户id',
  `name` varchar(32) DEFAULT NULL COMMENT '跑腿员姓名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `student_no` varchar(20) DEFAULT NULL COMMENT '学号',
  `campus` varchar(50) DEFAULT NULL COMMENT '校区',
  `college` varchar(50) DEFAULT NULL COMMENT '学院',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '认证状态 0待认证 1已认证 2认证拒绝 3认证审核中',
  `runner_level` tinyint NOT NULL DEFAULT '1' COMMENT '跑腿等级 1普通 2铜牌 3银牌 4金牌',
  `daily_order_limit` int NOT NULL DEFAULT '10' COMMENT '每日接单上限',
  `completed_orders` int NOT NULL DEFAULT '0' COMMENT '完成订单数',
  `score` decimal(3,1) NOT NULL DEFAULT '5.0' COMMENT '综合评分',
  `withdraw_password` varchar(100) DEFAULT NULL COMMENT '提现密码(加密存储)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态 0禁用 1启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_runner_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员表';

-- 5. 跑腿订单类型表
CREATE TABLE `errand_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '类型名称（如：帮我取、帮我送、代买）',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `description` varchar(255) DEFAULT NULL COMMENT '类型说明',
  `fee_rate` decimal(3,2) NOT NULL DEFAULT '0.10' COMMENT '平台服务费率',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0停用 1启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单类型表';

-- 6. 跑腿员认证审核表
CREATE TABLE `runner_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `runner_id` bigint NOT NULL COMMENT '跑腿员id',
  `real_name` varchar(32) NOT NULL COMMENT '真实姓名',
  `student_no` varchar(20) NOT NULL COMMENT '学号',
  `campus` varchar(50) DEFAULT NULL COMMENT '校区',
  `college` varchar(50) DEFAULT NULL COMMENT '学院',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `student_card_img` varchar(500) DEFAULT NULL COMMENT '学生证照片',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人id',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_runner_audit_runner_id` (`runner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员认证审核表';

-- 7. 钱包账户表
CREATE TABLE `wallet_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  `frozen_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '冻结金额',
  `total_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计收入',
  `total_expense` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '累计支出',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0冻结 1正常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_wallet_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包账户表';

-- 8. 钱包流水表
CREATE TABLE `wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `wallet_id` bigint NOT NULL COMMENT '钱包账户id',
  `type` tinyint NOT NULL COMMENT '流水类型 1跑腿收入 2支付支出 3充值 4提现 5退款 6违约金',
  `amount` decimal(10,2) NOT NULL COMMENT '发生金额',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单id',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_transaction_wallet_id` (`wallet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水表';

-- 9. 提现申请表
CREATE TABLE `withdraw_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `amount` decimal(10,2) NOT NULL COMMENT '提现金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待处理 1已打款 2已驳回',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `pay_time` datetime DEFAULT NULL COMMENT '打款时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_withdraw_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现申请表';

-- 10. 订单评价表
CREATE TABLE `order_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `user_id` bigint NOT NULL COMMENT '发单用户id',
  `runner_id` bigint NOT NULL COMMENT '跑腿员id',
  `score` tinyint NOT NULL DEFAULT '5' COMMENT '评分 1-5',
  `content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `tags` varchar(255) DEFAULT NULL COMMENT '评价标签，逗号分隔',
  `is_anonymous` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否匿名 0否 1是',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_order_id` (`order_id`),
  KEY `idx_review_runner_id` (`runner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单评价表';

-- 11. 订单主表重构：删除外卖字段，替换为跑腿订单字段
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) DEFAULT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '发单用户id',
  `runner_id` bigint DEFAULT NULL COMMENT '跑腿员id（接单后回填）',
  `type_id` bigint DEFAULT NULL COMMENT '订单类型id',
  `title` varchar(100) NOT NULL COMMENT '订单标题',
  `description` varchar(500) DEFAULT NULL COMMENT '需求描述',
  `pickup_address` varchar(255) DEFAULT NULL COMMENT '取件地址',
  `delivery_address` varchar(255) NOT NULL COMMENT '送达地址',
  `campus` varchar(50) DEFAULT NULL COMMENT '校区',
  `reward_amount` decimal(10,2) NOT NULL COMMENT '悬赏金额',
  `platform_fee` decimal(10,2) DEFAULT NULL COMMENT '平台服务费',
  `runner_income` decimal(10,2) DEFAULT NULL COMMENT '跑腿员实得金额',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待支付 2待接单 3进行中 4已送达 5已完成 6已取消 7已超时',
  `expected_time` datetime DEFAULT NULL COMMENT '期望完成时间',
  `timeout_time` datetime DEFAULT NULL COMMENT '超时时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `cancel_by` tinyint DEFAULT NULL COMMENT '取消方 1用户 2跑腿员 3平台',
  `is_appealed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否申诉 0否 1是',
  `pay_method` int DEFAULT '1' COMMENT '支付方式 1微信 2钱包余额',
  `pay_status` tinyint DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2已退款',
  `order_time` datetime DEFAULT NULL COMMENT '下单时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_orders_user_id` (`user_id`),
  KEY `idx_orders_runner_id` (`runner_id`),
  KEY `idx_orders_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单表';

-- 12. 订单类型种子数据
INSERT INTO `errand_type` (`name`, `description`, `fee_rate`, `sort`, `status`, `create_time`) VALUES
('帮我取', '快递、外卖、文件等代取代送', 0.10, 1, 1, NOW()),
('帮我送', '文件、物品同城/同校代送', 0.10, 2, 1, NOW()),
('帮我买', '餐饮、日用品、药品等代购', 0.10, 3, 1, NOW()),
('帮我办', '代排队、代打印、代办事务', 0.10, 4, 1, NOW());

-- 补充：地址簿去除省市区字段（校园地址以 校区/楼栋/房间号 为主）
ALTER TABLE `address_book`
    DROP COLUMN `province_code`,
    DROP COLUMN `province_name`,
    DROP COLUMN `city_code`,
    DROP COLUMN `city_name`,
    DROP COLUMN `district_code`,
    DROP COLUMN `district_name`;

-- 补充：订单增加取件时间（阶段6 确认取件）
ALTER TABLE `orders` ADD COLUMN `pickup_time` datetime DEFAULT NULL COMMENT '取件时间' AFTER `pay_time`;

-- 补充：订单增加取件时间（阶段6 确认取件）
ALTER TABLE `orders` ADD COLUMN `pickup_time` datetime DEFAULT NULL COMMENT '取件时间' AFTER `pay_time`;
