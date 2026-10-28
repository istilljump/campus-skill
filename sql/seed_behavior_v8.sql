-- =====================================================================
-- seed_behavior_v8.sql —— 推荐引擎演示行为数据 v8（可重复执行 / 幂等）
--
-- 目标：为演示账号构造「浏览 / 预约 / 成交」行为记录，让推荐引擎
--       （ItemCF 协同过滤 + 内容特征混合打分）在答辩现场直接产出
--       带推荐理由的个性化结果，而非冷启动热门排序。
--
-- 依赖：campus.sql（含 user_behavior 表）+ seed_demo.sql（demo1~demo4 账号）
--       与陈技能（demo2）在售服务；建议先执行 seed_demo_v7.sql。
-- 用法：mysql -uroot --default-character-set=utf8mb4 campus_runner < sql/seed_behavior_v8.sql
--
-- 幂等策略：仅重建本脚本涉及的演示账号行为，不影响真实浏览数据。
-- =====================================================================
USE campus_runner;

SET @skUser = (SELECT id FROM user WHERE name = '测试用户' ORDER BY id LIMIT 1);
SET @s3     = (SELECT id FROM skiller WHERE user_id = (SELECT id FROM user WHERE username = 'demo2' LIMIT 1) LIMIT 1); -- 陈技能

SET @u4 = (SELECT id FROM user WHERE username = 'demo4' LIMIT 1); -- 李梦琪
SET @u5 = (SELECT id FROM user WHERE username = 'demo5' LIMIT 1);
SET @u6 = (SELECT id FROM user WHERE username = 'demo6' LIMIT 1);
SET @u7 = (SELECT id FROM user WHERE username = 'demo7' LIMIT 1);

-- 陈技能在售服务（取两个）
SET @s3a = (SELECT id FROM service_item WHERE skiller_id = @s3 AND status = 1 ORDER BY id LIMIT 1);
SET @s3b = (SELECT id FROM service_item WHERE skiller_id = @s3 AND status = 1 ORDER BY id DESC LIMIT 1);

-- 测试用户（技能者身份）在售服务，按类目取
SET @skSkiller = (SELECT id FROM skiller WHERE user_id = @skUser LIMIT 1);
SET @ownCat1 = (SELECT id FROM service_item WHERE skiller_id = @skSkiller AND category_id = 1 AND status = 1 LIMIT 1);
SET @ownCat2 = (SELECT id FROM service_item WHERE skiller_id = @skSkiller AND category_id = 2 AND status = 1 LIMIT 1);
SET @ownCat3 = (SELECT id FROM service_item WHERE skiller_id = @skSkiller AND category_id = 3 AND status = 1 LIMIT 1);
SET @ownCat7 = (SELECT id FROM service_item WHERE skiller_id = @skSkiller AND category_id = 7 AND status = 1 LIMIT 1);

-- ---------------------------------------------------------------------
-- 清理上一轮（幂等）：仅演示账号的行为
-- ---------------------------------------------------------------------
DELETE FROM user_behavior
 WHERE user_id IN (@skUser, @u4, @u5, @u6, @u7)
   AND service_item_id IN (@s3a, @s3b, @ownCat1, @ownCat2, @ownCat3, @ownCat7);

-- ---------------------------------------------------------------------
-- 行为矩阵设计（协同过滤演示核心）：
--   测试用户(@skUser)  浏览陈技能两项服务 + 预约其中一项  → 兴趣：陈技能类目
--   demo4 浏览「测试用户海报(cat2)」+ 陈技能 @s3a
--   demo5 浏览「测试用户PPT(cat1)」+ 陈技能 @s3a + 成交 PPT
--   demo6 浏览「测试用户PPT(cat1)」+「测试用户翻译(cat7)」
--   demo7 浏览陈技能 @s3a +「测试用户翻译(cat7)」+ 成交 PPT
-- → 测试用户看 @s3a 时，「看了又看」应出现 @s3b 及同类目服务；
--   demo4~7 的共同行为把陈技能的服务与测试用户的服务关联起来，
--   供纯用户账号（如 demo4）的「猜你喜欢」产生跨技能者的协同推荐。
-- ---------------------------------------------------------------------
INSERT INTO user_behavior (user_id, service_item_id, category_id, behavior_type, weight, create_time)
SELECT b.u, b.item, b.cat, b.tp, b.w, NOW() - INTERVAL b.d DAY
FROM (
  -- 测试用户：浏览陈技能两项 + 预约一项
  SELECT @skUser u, @s3a item, (SELECT category_id FROM service_item WHERE id = @s3a) cat, 1 tp, 1.0 w, 3 d
  UNION ALL SELECT @skUser, @s3b, (SELECT category_id FROM service_item WHERE id = @s3b), 1, 1.0, 2
  UNION ALL SELECT @skUser, @s3a, (SELECT category_id FROM service_item WHERE id = @s3a), 2, 3.0, 1
  -- demo4
  UNION ALL SELECT @u4, @ownCat2, 2, 1, 1.0, 5
  UNION ALL SELECT @u4, @s3a,    (SELECT category_id FROM service_item WHERE id = @s3a), 1, 1.0, 4
  -- demo5
  UNION ALL SELECT @u5, @ownCat1, 1, 1, 1.0, 6
  UNION ALL SELECT @u5, @s3a,    (SELECT category_id FROM service_item WHERE id = @s3a), 1, 1.0, 3
  UNION ALL SELECT @u5, @ownCat1, 1, 3, 5.0, 2
  -- demo6
  UNION ALL SELECT @u6, @ownCat1, 1, 1, 1.0, 4
  UNION ALL SELECT @u6, @ownCat7, 7, 1, 1.0, 3
  -- demo7
  UNION ALL SELECT @u7, @s3a,    (SELECT category_id FROM service_item WHERE id = @s3a), 1, 1.0, 5
  UNION ALL SELECT @u7, @ownCat7, 7, 1, 1.0, 2
  UNION ALL SELECT @u7, @ownCat1, 1, 3, 5.0, 1
) b
WHERE b.item IS NOT NULL AND b.u IS NOT NULL;
