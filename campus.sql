-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: localhost    Database: campus_runner
-- ------------------------------------------------------
-- Server version	8.0.34

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `address_book`
--

DROP TABLE IF EXISTS `address_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '详细地址',
  `campus` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '校区',
  `building` varchar(100) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '楼栋',
  `room` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '房间号',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='地址簿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address_book`
--

LOCK TABLES `address_book` WRITE;
/*!40000 ALTER TABLE `address_book` DISABLE KEYS */;
/*!40000 ALTER TABLE `address_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
INSERT INTO `employee` VALUES (1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e','13812312312','1','110101199001010047',1,'2022-02-15 15:51:20','2022-02-17 09:16:20',10,1);
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `errand_type`
--

DROP TABLE IF EXISTS `errand_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `errand_type`
--

LOCK TABLES `errand_type` WRITE;
/*!40000 ALTER TABLE `errand_type` DISABLE KEYS */;
INSERT INTO `errand_type` VALUES (1,'帮我取',NULL,'快递、外卖、文件等代取代送',0.10,1,1,'2026-09-27 15:53:01',NULL),(2,'帮我送',NULL,'文件、物品同城/同校代送',0.10,2,1,'2026-09-27 15:53:01',NULL),(3,'帮我买',NULL,'餐饮、日用品、药品等代购',0.10,3,1,'2026-09-27 15:53:01',NULL),(4,'帮我办',NULL,'代排队、代打印、代办事务',0.10,4,1,'2026-09-27 15:53:01',NULL);
/*!40000 ALTER TABLE `errand_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_review`
--

DROP TABLE IF EXISTS `order_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_review`
--

LOCK TABLES `order_review` WRITE;
/*!40000 ALTER TABLE `order_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `pickup_time` datetime DEFAULT NULL COMMENT '取件时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_orders_user_id` (`user_id`),
  KEY `idx_orders_runner_id` (`runner_id`),
  KEY `idx_orders_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `runner`
--

DROP TABLE IF EXISTS `runner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `runner`
--

LOCK TABLES `runner` WRITE;
/*!40000 ALTER TABLE `runner` DISABLE KEYS */;
/*!40000 ALTER TABLE `runner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `runner_audit`
--

DROP TABLE IF EXISTS `runner_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿员认证审核表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `runner_audit`
--

LOCK TABLES `runner_audit` WRITE;
/*!40000 ALTER TABLE `runner_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `runner_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `student_no` varchar(20) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '学号',
  `campus` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '校区',
  `credit_score` int NOT NULL DEFAULT '100' COMMENT '信用分',
  `publish_order_count` int NOT NULL DEFAULT '0' COMMENT '累计发单数',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_account`
--

DROP TABLE IF EXISTS `wallet_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_account`
--

LOCK TABLES `wallet_account` WRITE;
/*!40000 ALTER TABLE `wallet_account` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_transaction`
--

DROP TABLE IF EXISTS `wallet_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_transaction`
--

LOCK TABLES `wallet_transaction` WRITE;
/*!40000 ALTER TABLE `wallet_transaction` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdraw_request`
--

DROP TABLE IF EXISTS `withdraw_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提现申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdraw_request`
--

LOCK TABLES `withdraw_request` WRITE;
/*!40000 ALTER TABLE `withdraw_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `withdraw_request` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-27 16:26:42
