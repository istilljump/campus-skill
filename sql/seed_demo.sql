-- =====================================================================
-- seed_demo.sql —— CampusSkill 演示种子数据（可选导入，用于答辩演示）
-- 前提：已导入 campus.sql
-- 演示账号（本地联调登录模式，微信登录未配置时任意 code 可登录）：
--   管理端      admin / 123456
--   需求方      登录 code：demo1        （张晓文，钱包余额 300）
--   需求方      登录 code：demo4        （李梦琪）
--   技能者      登录 code：demo2        （陈技能 · C3级 · 视频剪辑/PPT，余额 336）
--   技能者      登录 code：demo3        （林小艺 · C2级 · 海报设计/摄影，余额 172）
-- 数据内容：技能者入驻与作品、上架服务、近 7 天成交订单与评价、信用分流水、站内消息
-- =====================================================================
USE campus_runner;

-- ---------- 用户（openid 与本地联调登录 code 对应：campus_dev_{code}） ----------
INSERT INTO user (openid, name, phone, sex, student_no, campus, credit_score, publish_order_count, create_time) VALUES
 ('campus_dev_demo1', '张晓文', '13800000001', '女', '2026010101', '主校区', 100, 4, NOW() - INTERVAL 10 DAY),
 ('campus_dev_demo2', '陈技能', '13800000002', '男', '2026010102', '主校区', 106, 1, NOW() - INTERVAL 10 DAY),
 ('campus_dev_demo3', '林小艺', '13800000003', '女', '2026010103', '东校区', 103, 1, NOW() - INTERVAL 9 DAY),
 ('campus_dev_demo4', '李梦琪', '13800000004', '女', '2026010104', '主校区', 100, 1, NOW() - INTERVAL 8 DAY);

SET @u1 = (SELECT id FROM user WHERE openid = 'campus_dev_demo1');
SET @u2 = (SELECT id FROM user WHERE openid = 'campus_dev_demo2');
SET @u3 = (SELECT id FROM user WHERE openid = 'campus_dev_demo3');
SET @u4 = (SELECT id FROM user WHERE openid = 'campus_dev_demo4');

-- ---------- 技能者（已认证、已定级） ----------
INSERT INTO skiller (user_id, name, phone, student_no, campus, college, audit_status, runner_level, daily_order_limit, completed_orders, score, credit_score, skill_level, create_time)
VALUES (@u2, '陈技能', '13800000002', '2026010102', '主校区', '计算机学院', 1, 2, 10, 7, 4.9, 103, 3, NOW() - INTERVAL 10 DAY),
       (@u3, '林小艺', '13800000003', '2026010103', '东校区', '艺术设计学院', 1, 1, 10, 4, 4.8, 101, 2, NOW() - INTERVAL 9 DAY);
SET @s1 = (SELECT id FROM skiller WHERE user_id = @u2);
SET @s2 = (SELECT id FROM skiller WHERE user_id = @u3);

INSERT INTO skiller_audit (runner_id, real_name, student_no, campus, college, status, audit_remark, audit_time, apply_time) VALUES
 (@s1, '陈技能', '2026010102', '主校区', '计算机学院', 1, '学籍信息核验通过', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY),
 (@s2, '林小艺', '2026010103', '东校区', '艺术设计学院', 1, '学籍信息核验通过', NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY);

-- ---------- 地址簿（需求方） ----------
INSERT INTO address_book (user_id, consignee, phone, campus, building, room, detail, label, is_default) VALUES
 (@u1, '张晓文', '13800000001', '主校区', '东区3号楼', '502', '东区3号楼502', '宿舍', 1),
 (@u4, '李梦琪', '13800000004', '主校区', '南区8号楼', '311', '南区8号楼311', '宿舍', 1);

-- ---------- 作品集（已通过审核） ----------
INSERT INTO portfolio (skiller_id, title, category_id, cover_url, work_urls, description, status, audit_opinion, create_time) VALUES
 (@s1, '课程汇报PPT美化合集', 1, 'https://picsum.photos/seed/ppt1/400/240', 'https://picsum.photos/seed/ppt1/800/450', '三套课程答辩PPT排版案例，科技风/极简风/学术风', 1, '作品完整度高，定级C3', NOW() - INTERVAL 8 DAY),
 (@s1, '社团宣传片剪辑', 3, 'https://picsum.photos/seed/vid1/400/240', 'https://picsum.photos/seed/vid1/800/450', '招新宣传片与活动混剪示例', 1, '剪辑节奏好，定级C3', NOW() - INTERVAL 8 DAY),
 (@s2, '社团招新海报设计', 2, 'https://picsum.photos/seed/post1/400/240', 'https://picsum.photos/seed/post1/800/450', '六张招新与活动海报', 1, '视觉表现优秀，定级C2', NOW() - INTERVAL 7 DAY);

-- ---------- 服务货架（上架中） ----------
INSERT INTO service_item (skiller_id, category_id, title, description, price, delivery_days, service_mode, tags, status, sales_count, avg_score, create_time) VALUES
 (@s1, 1, '课程汇报PPT美化（30页内）', '提供排版美化、配色统一、图表优化，含2轮修改，24小时交付', 60, 1, 1, 'PPT,排版,答辩', 1, 5, 4.9, NOW() - INTERVAL 8 DAY),
 (@s1, 3, '短视频剪辑（3分钟内）', '混剪/字幕/调色包装，含1轮修改', 100, 2, 1, '剪辑,字幕,Premiere', 1, 2, 5.0, NOW() - INTERVAL 8 DAY),
 (@s2, 2, '社团活动海报设计（2张）', '招新/晚会/讲座海报，提供源文件，含2轮修改', 80, 2, 1, '海报,PS,设计', 1, 3, 4.8, NOW() - INTERVAL 7 DAY),
 (@s2, 4, '校园人像约拍（1小时）', '校内取景，精修9张，底片全送', 120, 3, 2, '摄影,约拍,精修', 1, 1, 5.0, NOW() - INTERVAL 7 DAY);

-- ---------- 近 7 天成交订单（悬赏模式，已完成，供趋势大屏展示） ----------
INSERT INTO orders (number, user_id, runner_id, type_id, mode, title, description, delivery_address, campus, reward_amount, platform_fee, runner_income, status, pay_method, pay_status, order_time, pay_time, deliver_time, finish_time, create_time, update_time, rework_count) VALUES
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 6 DAY)*1000), '100001'), @u1, @s1, 1, 1, '高数课件PPT美化', '60页课件统一风格', '线上交付', '主校区', 60, 6.0, 54.0, 5, 2, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 5 HOUR, NOW() - INTERVAL 6 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 6 HOUR, 0),
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 5 DAY)*1000), '100002'), @u4, @s2, 2, 1, '招新海报设计', '两张，现代简约风', '线上交付', '东校区', 80, 8.0, 72.0, 5, 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR, NOW() - INTERVAL 5 DAY + INTERVAL 22 HOUR, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 22 HOUR, 0),
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 4 DAY)*1000), '100003'), @u1, @s1, 3, 1, '晚会开场视频剪辑', '两分钟混剪加字幕', '线上交付', '主校区', 100, 12.0, 88.0, 5, 2, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 30 HOUR, NOW() - INTERVAL 4 DAY + INTERVAL 31 HOUR, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 31 HOUR, 0),
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 3 DAY)*1000), '100004'), @u4, @s2, 2, 1, '讲座宣传海报', '单张，学术风', '线上交付', '东校区', 40, 4.0, 36.0, 5, 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 18 HOUR, NOW() - INTERVAL 3 DAY + INTERVAL 19 HOUR, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 19 HOUR, 0),
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 2 DAY)*1000), '100005'), @u1, @s1, 5, 1, '课程设计代码调试', 'Java课设报错排查', '线上交付', '主校区', 50, 5.0, 45.0, 5, 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 8 HOUR, NOW() - INTERVAL 2 DAY + INTERVAL 9 HOUR, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 9 HOUR, 0),
 (CONCAT('CR', FLOOR(UNIX_TIMESTAMP(NOW() - INTERVAL 1 DAY)*1000), '100006'), @u4, @s1, 1, 1, '答辩PPT美化（紧急）', '20页，明天要用', '线上交付', '主校区', 70, 7.0, 63.0, 5, 2, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 1 DAY + INTERVAL 7 HOUR, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 7 HOUR, 0);

-- ---------- 评价（对应已完成订单） ----------
INSERT INTO order_review (order_id, user_id, runner_id, score, content, tags, is_anonymous, create_time)
SELECT o.id, o.user_id, o.runner_id, 5, '排版又快又好，答辩加分了！', '质量高,响应快', 0, o.finish_time FROM orders o WHERE o.title = '高数课件PPT美化';
INSERT INTO order_review (order_id, user_id, runner_id, score, content, tags, is_anonymous, create_time)
SELECT o.id, o.user_id, o.runner_id, 5, '海报设计很有想法，同学们都说好看', '有创意,沟通顺畅', 0, o.finish_time FROM orders o WHERE o.title = '招新海报设计';
INSERT INTO order_review (order_id, user_id, runner_id, score, content, tags, is_anonymous, create_time)
SELECT o.id, o.user_id, o.runner_id, 4, '剪辑不错，字幕再细致些更好', '交付准时', 1, o.finish_time FROM orders o WHERE o.title = '晚会开场视频剪辑';
INSERT INTO order_review (order_id, user_id, runner_id, score, content, tags, is_anonymous, create_time)
SELECT o.id, o.user_id, o.runner_id, 5, ' Debug一小时就解决了，救大命', '专业,高效', 0, o.finish_time FROM orders o WHERE o.title = '课程设计代码调试';

-- ---------- 钱包（技能者收入 + 需求方余额） ----------
INSERT INTO wallet_account (user_id, balance, frozen_amount, total_income, total_expense, status, create_time) VALUES
 (@u1, 210.00, 0.00, 0.00, 290.00, 1, NOW() - INTERVAL 10 DAY),
 (@u4, 150.00, 0.00, 0.00, 190.00, 1, NOW() - INTERVAL 8 DAY),
 (@u2, 336.00, 0.00, 336.00, 0.00, 1, NOW() - INTERVAL 10 DAY),
 (@u3, 172.00, 0.00, 172.00, 0.00, 1, NOW() - INTERVAL 9 DAY);

INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT w.id, 3, 500, NULL, '演示初始充值', w.create_time FROM wallet_account w WHERE w.user_id IN (@u1, @u4);
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT w.id, 1, 336, NULL, '技能服务收入（演示汇总）', NOW() - INTERVAL 1 DAY FROM wallet_account w WHERE w.user_id = @u2;
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT w.id, 1, 172, NULL, '技能服务收入（演示汇总）', NOW() - INTERVAL 1 DAY FROM wallet_account w WHERE w.user_id = @u3;
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT w.id, 2, o.reward_amount, o.id, CONCAT('技能订单支付-', o.number), o.pay_time FROM wallet_account w JOIN orders o ON o.user_id = w.user_id WHERE o.status = 5;

-- ---------- 信用分流水 ----------
INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @s1, 1, '交付完成+1', o.id, o.finish_time FROM orders o WHERE o.title = '高数课件PPT美化';
INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @s1, 2, '好评+2', o.id, o.finish_time FROM orders o WHERE o.title = '课程设计代码调试';
INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @s2, 1, '交付完成+1', o.id, o.finish_time FROM orders o WHERE o.title = '招新海报设计';

-- ---------- 站内消息 ----------
INSERT INTO message (recipient_id, title, content, order_id, is_read, create_time) VALUES
 (@u1, '订单已交付', '您的订单「答辩PPT美化（紧急）」已交付，请及时验收', NULL, 0, NOW() - INTERVAL 1 DAY + INTERVAL 7 HOUR),
 (@u2, '收到新的悬赏', '技能悬赏大厅有新的PPT美化需求，快去看看吧', NULL, 0, NOW() - INTERVAL 2 HOUR),
 (@u3, '作品审核通过', '恭喜！您的作品「社团招新海报设计」已通过审核，定级：C2', NULL, 1, NOW() - INTERVAL 7 DAY),
 (@u4, '欢迎来到 CampusSkill', '在这里发布技能需求，或把你的技能变现吧！', NULL, 0, NOW() - INTERVAL 8 DAY);
