-- =====================================================================
-- seed_demo_v7.sql —— CampusSkill 答辩演示数据 v7（可重复执行 / 幂等）
--
-- 目标：把「悬赏大厅 · 进行中订单 · 测试用户技能者工作台全板块 · 空态 · 异常态」
--       一次性补齐，Web 门户与 App H5 共用同一套接口，数据天然双端一致。
--
-- 依赖：campus.sql 已导入；主账号定位兼容「测试用户 / 张晓文（seed_demo.sql 初始名）」，
--       技能者档案缺失时自动补建，全新库可直接执行（本脚本通过 name 动态定位，不写死 id）。
-- 用法：mysql -uroot --default-character-set=utf8mb4 campus_runner < sql/seed_demo_v7.sql
--
-- 幂等策略：本脚本产生的订单号统一以 'CS7' 开头；清理时按该前缀 + 演示标记删除后重建，
--           不会影响候选种子数据（seed_demo.sql 的账户与其订单）。
-- =====================================================================
USE campus_runner;

-- ---------------------------------------------------------------------
-- 0. 前置校验：定位「测试用户」账号与其技能者档案；缺失则中断脚本
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS _seed7_guard;
CREATE TABLE _seed7_guard (k VARCHAR(32) PRIMARY KEY, v BIGINT NOT NULL);

-- 兼容全新库：测试用户可能叫「张晓文」（seed_demo.sql 初始名），二者取其一
SET @skUser = (SELECT id FROM user WHERE name IN ('测试用户', '张晓文') ORDER BY id LIMIT 1);
-- 自足式补建：全新库上该账号可能尚无技能者档案，此处直接补建（已存在则跳过）
INSERT INTO skiller (user_id, name, phone, student_no, campus, college, audit_status, runner_level,
                     daily_order_limit, completed_orders, score, credit_score, skill_level, create_time, update_time)
SELECT @skUser,
       COALESCE((SELECT name FROM user WHERE id = @skUser), '测试用户'),
       COALESCE((SELECT phone FROM user WHERE id = @skUser), '13800000001'),
       COALESCE((SELECT student_no FROM user WHERE id = @skUser), '2026010101'),
       '主校区', '计算机学院', 1, 2, 10, 0, 4.8, 105, 3, NOW(), NOW()
  FROM DUAL
 WHERE @skUser IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM skiller WHERE user_id = @skUser);
SET @skill = (SELECT id FROM skiller WHERE user_id = @skUser LIMIT 1);

-- v 为 NOT NULL：账号缺失时此处报错并中止整个脚本（mysql 批处理默认遇错停止）
INSERT INTO _seed7_guard (k, v) SELECT 'skUser', @skUser;
INSERT INTO _seed7_guard (k, v) SELECT 'skill',  @skill;

-- 其余演示账号（发布方），沿用候选账号体系
SET @u4 = (SELECT id FROM user WHERE username = 'demo4' LIMIT 1);   -- 李梦琪
SET @s3 = (SELECT id FROM skiller WHERE user_id = (SELECT id FROM user WHERE username = 'demo2' LIMIT 1) LIMIT 1); -- 陈技能
SET @cat3Svc = (SELECT id FROM service_item WHERE skiller_id = @s3 ORDER BY id LIMIT 1);

-- ---------------------------------------------------------------------
-- 1. 清理上一轮（幂等）
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS _s7o;
CREATE TEMPORARY TABLE _s7o (id BIGINT PRIMARY KEY);
INSERT INTO _s7o SELECT id FROM orders WHERE number LIKE 'CS7%';

DELETE FROM order_review       WHERE order_id IN (SELECT id FROM _s7o);
DELETE FROM credit_log         WHERE order_id IN (SELECT id FROM _s7o);
DELETE FROM dispute            WHERE order_id IN (SELECT id FROM _s7o);
DELETE FROM wallet_transaction WHERE order_id IN (SELECT id FROM _s7o);
DELETE FROM booking            WHERE order_id IN (SELECT id FROM _s7o);
DELETE FROM orders             WHERE id       IN (SELECT id FROM _s7o);
DROP TEMPORARY TABLE _s7o;

DELETE FROM booking          WHERE remark LIKE '演示数据%';
DELETE FROM withdraw_request WHERE user_id = @skUser AND remark LIKE '演示数据%';
-- 服务货架 / 作品集 / 信用流水 / 收件箱：按技能者（用户）整体重建，保证展示口径一致
DELETE FROM service_item WHERE skiller_id = @skill;
DELETE FROM portfolio    WHERE skiller_id = @skill;
DELETE FROM credit_log   WHERE skiller_id = @skill;
DELETE FROM message      WHERE recipient_id = @skUser;
-- 钱包流水重建（余额语义：余额 = 收入 + 充值 + 退款 − 支出 − 提现 − 违约金）
DELETE FROM wallet_transaction
 WHERE wallet_id = (SELECT id FROM wallet_account WHERE user_id = @skUser);

-- ---------------------------------------------------------------------
-- 2. 补充演示发布方账号（demo5~demo7 / 密码 123456）
--    密码存储：MD5(salt + 明文)，与 v6 账号体系一致
-- ---------------------------------------------------------------------
INSERT INTO user (openid, username, password, salt, name, phone, sex, student_no, campus, credit_score, publish_order_count, create_time)
SELECT 'acct_demo5', 'demo5', '193c5036eb706d279a7f9f95e9bdfeba', 'campus_skill_demo5', '王思远', '13800000005', '男', '2026010205', '东校区', 100, 3, NOW() - INTERVAL 7 DAY
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'demo5');
INSERT INTO user (openid, username, password, salt, name, phone, sex, student_no, campus, credit_score, publish_order_count, create_time)
SELECT 'acct_demo6', 'demo6', 'd41210fb2684e44785d7294d51bbdbe0', 'campus_skill_demo6', '赵一鸣', '13800000006', '男', '2026010206', '南校区', 100, 3, NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'demo6');
INSERT INTO user (openid, username, password, salt, name, phone, sex, student_no, campus, credit_score, publish_order_count, create_time)
SELECT 'acct_demo7', 'demo7', '164253f5c2b1b83739bb2249564c4110', 'campus_skill_demo7', '孙雅婷', '13800000007', '女', '2026010207', '主校区', 100, 3, NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'demo7');

SET @u5 = (SELECT id FROM user WHERE username = 'demo5' LIMIT 1);
SET @u6 = (SELECT id FROM user WHERE username = 'demo6' LIMIT 1);
SET @u7 = (SELECT id FROM user WHERE username = 'demo7' LIMIT 1);

-- ---------------------------------------------------------------------
-- 3. 资料补齐：测试用户（用户资料 + 技能者档案）
--    提现密码设为 123456（存储为裸 MD5，与 WalletServiceImpl 一致）
-- ---------------------------------------------------------------------
UPDATE user SET name = '测试用户', phone = '13800000001', sex = '女',
                student_no = '2026010101', campus = '主校区',
                avatar = '/assets/covers/avatar-default.svg'
 WHERE id = @skUser;

UPDATE skiller SET name = '测试用户', phone = '13800000001',
                   student_no = '2026010101', campus = '主校区', college = '计算机学院',
                   skill_level = 3,                                  -- 作品定级 C3
                   withdraw_password = 'e10adc3949ba59abbe56e057f20f883e', -- MD5('123456')
                   daily_order_limit = 10, status = 1, audit_status = 1
 WHERE id = @skill;

-- ---------------------------------------------------------------------
-- 4. 服务货架（4 上架 + 1 下架，演示状态切换）
-- ---------------------------------------------------------------------
INSERT INTO service_item (skiller_id, category_id, title, description, price, delivery_days, service_mode, tags, status, sales_count, avg_score, create_time, update_time) VALUES
 (@skill, 1, '课程PPT美化与排版（30页内）', '含排版美化、配色统一、图表优化，提供 2 轮修改，24 小时内交付源文件', 60.00, 1, 1, 'PPT,排版,答辩', 1, 6, 4.9, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 3 DAY),
 (@skill, 2, '活动海报设计（含源文件·2张）', '招新 / 晚会 / 讲座海报，提供 PSD 源文件，含 2 轮修改', 80.00, 2, 1, '海报,PS,设计', 1, 4, 4.8, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 5 DAY),
 (@skill, 3, '短视频剪辑（3分钟内）', '混剪 / 字幕 / 调色包装，含 1 轮修改，支持加急交付', 100.00, 2, 1, '剪辑,字幕,Premiere', 1, 3, 5.0, NOW() - INTERVAL 26 DAY, NOW() - INTERVAL 2 DAY),
 (@skill, 7, '英文摘要翻译润色', '中译英 / 润色，学术风格，适合论文与竞赛材料，48 小时内交付', 60.00, 1, 1, '翻译,润色,学术', 1, 2, 4.9, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 4 DAY),
 (@skill, 5, '代码调试与课设指导（演示）', 'Java / Python 课程设计报错排查与思路讲解，线下约教室', 50.00, 1, 2, 'Java,调试,课设', 0, 0, 5.0, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 9 DAY);

-- ---------------------------------------------------------------------
-- 5. 作品集（3 通过 + 1 待审核 + 1 驳回；封面使用本地占位图，断网可显示）
-- ---------------------------------------------------------------------
INSERT INTO portfolio (skiller_id, category_id, title, cover_url, work_urls, description, status, audit_opinion, auditor_id, create_time, update_time) VALUES
 (@skill, 1, '课程答辩PPT美化合集', '/assets/covers/ppt.svg', '/assets/covers/ppt.svg',
  '三套课程答辩 PPT 排版案例：科技风 / 极简风 / 学术风', 1, '作品完整度高，表达清晰，定级 C3', 1, NOW() - INTERVAL 24 DAY, NOW() - INTERVAL 23 DAY),
 (@skill, 2, '校园活动海报设计集', '/assets/covers/poster.svg', '/assets/covers/poster.svg',
  '招新 / 讲座 / 晚会海报共 6 张，含主视觉与延展物料', 1, '视觉表现优秀，定级 C3', 1, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 19 DAY),
 (@skill, 3, '社团宣传片混剪', '/assets/covers/video.svg', '/assets/covers/video.svg',
  '招新宣传片与活动混剪，含字幕与调色包装', 1, '剪辑节奏好，定级 C3', 1, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 14 DAY),
 (@skill, 7, '英文摘要润色样例', '/assets/covers/translate.svg', '/assets/covers/translate.svg',
  '学术论文摘要中译英与润色样例各 3 篇', 1, '语言地道，定级 C2', 1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY),
 (@skill, 8, '手工定制钥匙扣实拍', '/assets/covers/craft.svg', '/assets/covers/craft.svg',
  '毕业季定制钥匙扣实物与打样过程', 0, NULL, NULL, NOW() - INTERVAL 2 DAY, NULL),
 (@skill, 2, '毕设展板设计（待审·已驳回）', '/assets/covers/poster.svg', '/assets/covers/poster.svg',
  '毕设展板设计初稿，分辨率不足已退回重做', 2, '图片分辨率低于 300dpi，请重新上传后再次提交', 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY);

-- ---------------------------------------------------------------------
-- 6. 订单
--    状态：1待支付 2待接单 3进行中 4已交付 5已完成 6已取消 7已超时 8返修中 9仲裁中
--    订单号统一以 CS7 开头，便于本脚本幂等清理
-- ---------------------------------------------------------------------

-- 6.1 悬赏大厅：待接单（status=2 且已支付，timeout_time 保持未来避免被定时任务回收）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, create_time, update_time)
VALUES
 ('CS7202610130001', @skUser, NULL, 1, 1, '毕业论文答辩PPT美化（25页）', '需要统一样式、优化图表排版，明天下午前交付', '线上交付', '线上交付', '主校区', 80.00, 8.00, 72.00, 2, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 40 MINUTE, NOW() - INTERVAL 40 MINUTE, NOW() - INTERVAL 40 MINUTE, NOW() - INTERVAL 40 MINUTE),
 ('CS7202610130002', @skUser, NULL, 3, 1, '社团招新宣传视频剪辑', '素材已拍好，需要两分钟成片，加字幕和转场', '东区活动中心', '线上交付', '东校区', 100.00, 12.00, 88.00, 2, NOW() + INTERVAL 2 DAY, NOW() + INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR),
 ('CS7202610130003', @skUser, NULL, 7, 1, '数学建模竞赛论文翻译润色', '摘要与引言部分，约 3000 字，需学术表达地道', '线上交付', '线上交付', '主校区', 120.00, 12.00, 108.00, 2, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR),
 ('CS7202610130004', @u4, NULL, 2, 1, '迎新晚会主视觉海报设计', '需要主视觉一张 + 衍生物料两张，风格青春活泼', '线上交付', '线上交付', '南校区', 60.00, 6.00, 54.00, 2, NOW() + INTERVAL 2 DAY, NOW() + INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR),
 ('CS7202610130005', @skUser, NULL, 5, 1, 'Java课程设计代码调试', '课设运行时抛出空指针，需要排查并讲清思路', '图书馆研讨室', '线上交付', '主校区', 50.00, 5.00, 45.00, 2, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 20 HOUR),
 ('CS7202610130006', @u4, NULL, 6, 1, '简历美化与排版（1份）', '投递互联网实习，需要突出项目经历，控制在一页', '线上交付', '线上交付', '东校区', 30.00, 3.00, 27.00, 2, NOW() + INTERVAL 2 DAY, NOW() + INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
 ('CS7202610130007', @u5, NULL, 4, 1, '校园人像写真约拍（1小时）', '毕业季写真，校内取景，精修 9 张', '图书馆门口', '图书馆门口', '主校区', 150.00, 15.00, 135.00, 2, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 30 HOUR),
 ('CS7202610130008', @u6, NULL, 8, 1, '手工定制毕业纪念相册', '需要手工装帧，含封面刻字，共 20 页', '线上交付', '线上交付', '南校区', 200.00, 20.00, 180.00, 2, NOW() + INTERVAL 5 DAY, NOW() + INTERVAL 5 DAY, 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY);

-- 6.2 测试用户（技能者）· 进行中（status=3）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, create_time, update_time)
VALUES
 ('CS7202610130101', @u4, @skill, 3, 1, '校园歌手大赛宣传片剪辑', '决赛预热片，90 秒，需要卡点与字幕', '大学生活动中心', '线上交付', '主校区', 100.00, 12.00, 88.00, 3, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 20 HOUR, 2, 1, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 4 HOUR, NOW()),
 ('CS7202610130102', @u5, @skill, 2, 1, '选课攻略专题海报（3张）', '教务处公众号推文配图，需要信息层级清晰', '线上交付', '线上交付', '东校区', 90.00, 9.00, 81.00, 3, NOW() + INTERVAL 2 DAY, NOW() + INTERVAL 22 HOUR, 2, 1, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 2 HOUR, NOW());

-- 6.3 测试用户（技能者）· 返修中（status=8）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610130301', @u6, @skill, 2, 1, '实验室招新H5页面视觉稿', '首版配色偏暗，需要更明亮活泼并调整按钮层级', '线上交付', '线上交付', '南校区', 120.00, 12.00, 108.00, 8, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 12 HOUR, 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 8 HOUR);

-- 6.4 测试用户（技能者）· 已交付待验收（status=4，自动验收倒计时可演示）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, auto_accept_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610130201', @u5, @skill, 2, 1, '毕业纪念册排版设计', '40 页纪念册内页排版，含目录与章节页', '线上交付', '线上交付', '东校区', 80.00, 8.00, 72.00, 4, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 6 HOUR, NOW() + INTERVAL 42 HOUR, 0, NOW() - INTERVAL 2 DAY, NOW()),
 ('CS7202610130202', @u4, @skill, 7, 1, '英语四六级作文批改（10篇）', '逐篇批注并给出提分建议，附范文', '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 4, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 30 HOUR, NOW() + INTERVAL 18 HOUR, 0, NOW() - INTERVAL 2 DAY, NOW());

-- 6.5 测试用户（技能者）· 已完成（status=5，近 7 天分布；其中 2 单今日完成 → 今日收入 > 0）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, auto_accept_time, finish_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610131001', @u4, @skill, 1, 1, '高数期末真题讲解PPT美化', '60 页课件统一风格，重点公式加视觉强调', '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 5, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR, 2, 1, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 8 MINUTE, 0, NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 8 MINUTE),
 ('CS7202610131002', @u5, @skill, 3, 1, '团支部风采展示视频剪辑', '三分钟风采展示，需要背景音乐卡点与字幕', '线上交付', '线上交付', '东校区', 100.00, 12.00, 88.00, 5, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR, 2, 1, NOW() - INTERVAL 28 HOUR, NOW() - INTERVAL 28 HOUR, NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 9 HOUR, NOW() - INTERVAL 9 HOUR, NOW() - INTERVAL 25 MINUTE, 0, NOW() - INTERVAL 28 HOUR, NOW() - INTERVAL 25 MINUTE),
 ('CS7202610131003', @u6, @skill, 1, 1, '线性代数复习提纲排版', '复习提纲排版美化，需要公式编号统一', '线上交付', '线上交付', '主校区', 50.00, 5.00, 45.00, 5, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 3 HOUR, 0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY - INTERVAL 3 HOUR),
 ('CS7202610131004', @u4, @skill, 3, 1, '寝室文化节宣传视频', '宿舍文化节宣传片，两分半，需配乐和字幕', '线上交付', '线上交付', '主校区', 150.00, 18.00, 132.00, 5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 8 HOUR, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY - INTERVAL 8 HOUR),
 ('CS7202610131005', @u7, @skill, 2, 1, '运动会加油海报设计', '班级方阵加油海报一张，需要突出班级口号', '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 10 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 13 HOUR, 0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY - INTERVAL 13 HOUR),
 ('CS7202610131006', @u4, @skill, 4, 1, '校园人物专访摄影', '人物专访配图，含修图与氛围调色', '图书馆', '图书馆', '主校区', 150.00, 15.00, 135.00, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 7 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 7 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR),
 ('CS7202610131007', @u5, @skill, 5, 1, '数据结构课设调试', '课设二叉树遍历结果不对，需要排查并讲解', '线上交付', '线上交付', '东校区', 100.00, 10.00, 90.00, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 7 HOUR, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 7 HOUR),
 ('CS7202610131008', @u6, @skill, 6, 1, '保研简历精修', '保研夏令营投递简历，需要突出科研经历', '线上交付', '线上交付', '南校区', 40.00, 4.00, 36.00, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 17 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 17 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 12 HOUR, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 12 HOUR),
 ('CS7202610131009', @u7, @skill, 7, 1, '文献摘要中译英润色', '两篇文献摘要翻译，需要学术表达', '线上交付', '线上交付', '主校区', 90.00, 9.00, 81.00, 5, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 10 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 4 HOUR, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 4 HOUR),
 ('CS7202610131010', @u4, @skill, 8, 1, '社团文化衫图案设计', '文化衫前后幅图案，需要矢量源文件', '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 5, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 9 HOUR, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 9 HOUR),
 ('CS7202610131011', @u5, @skill, 1, 1, '微积分考前串讲PPT', '考前串讲课件排版，含例题与公式排版', '线上交付', '线上交付', '东校区', 80.00, 8.00, 72.00, 5, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 3 DAY, 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 14 HOUR, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY - INTERVAL 14 HOUR),
 ('CS7202610131012', @u6, @skill, 2, 1, '毕业季主题海报（2张）', '毕业季主题海报两张，需要手绘插画风', '线上交付', '线上交付', '南校区', 80.00, 8.00, 72.00, 5, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, 2, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 7 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 7 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 3 HOUR, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 3 HOUR),
 ('CS7202610131013', @u4, @skill, 3, 1, '3分钟社团宣传短片', '宣传片完整版剪辑，含片头片尾与字幕', '线上交付', '线上交付', '主校区', 200.00, 24.00, 176.00, 5, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, 2, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 12 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 8 HOUR, 2, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 8 HOUR),
 ('CS7202610131014', @u7, @skill, 5, 1, 'Python爬虫脚本调试', '爬取校园公告时被封禁，需要加请求头与限速', '线上交付', '线上交付', '主校区', 80.00, 8.00, 72.00, 5, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, 2, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 17 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 17 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 13 HOUR, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 4 DAY - INTERVAL 13 HOUR),
 ('CS7202610131015', @u4, @skill, 4, 1, '证件照精修与换底', '证件照换底色并精修，需要蓝底与白底各一版', '线上交付', '线上交付', '主校区', 120.00, 12.00, 108.00, 5, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, 2, 1, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 5 HOUR, 0, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 5 HOUR),
 ('CS7202610131016', @u5, @skill, 1, 1, '考研复试PPT制作', '复试自我介绍课件，需要有案例展示页', '线上交付', '线上交付', '东校区', 120.00, 12.00, 108.00, 5, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, 2, 1, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 10 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 14 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 14 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR, 0, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 10 HOUR),
 ('CS7202610131017', @u6, @skill, 2, 1, '校园广播台LOGO设计', '广播台新 LOGO，需要一版单色版本', '线上交付', '线上交付', '南校区', 100.00, 10.00, 90.00, 5, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, 2, 1, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 19 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 19 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 15 HOUR, 0, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 5 DAY - INTERVAL 15 HOUR),
 ('CS7202610131018', @u7, @skill, 8, 1, '手作钥匙扣定制（10个）', '毕业季定制钥匙扣，需要刻班级与姓名', '线上交付', '线上交付', '主校区', 100.00, 10.00, 90.00, 5, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY, 2, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY - INTERVAL 9 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 4 HOUR, 0, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 4 HOUR),
 ('CS7202610131019', @u4, @skill, 7, 1, '雅思作文批改（5篇）', 'Task 2 作文批改，需要给出评分与提升建议', '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 5, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY, 2, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY - INTERVAL 10 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 13 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 9 HOUR, 0, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 9 HOUR),
 ('CS7202610131020', @u5, @skill, 6, 1, '实习求职简历排版', '投递简历排版优化，控制在一页并补充项目量化描述', '线上交付', '线上交付', '东校区', 30.00, 3.00, 27.00, 5, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY, 2, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 7 DAY - INTERVAL 8 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 18 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 14 HOUR, 0, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 6 DAY - INTERVAL 14 HOUR);

-- 6.6 预约模式订单（mode=2）：支撑技能者端「预约单」与用户端「我的预约」
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, service_item_id, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, auto_accept_time, finish_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610131101', @u4, @skill, 1, 2, (SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 1 LIMIT 1),
  '【预约】课程PPT美化与排版（30页内）', '预约线下沟通后交付，需求：课程汇报 PPT 排版美化',
  '线上交付', '线上交付', '主校区', 60.00, 6.00, 54.00, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, 2, 1,
  NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 6 HOUR,
  NOW() - INTERVAL 2 DAY - INTERVAL 6 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR),
 ('CS7202610131102', @u5, @skill, 7, 2, (SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 7 LIMIT 1),
  '【预约】英文摘要翻译润色', '预约翻译服务，两篇摘要，48 小时内交付',
  '线上交付', '线上交付', '东校区', 60.00, 6.00, 54.00, 3, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 2, 1,
  NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR, NULL,
  NULL, NULL, 0, NOW() - INTERVAL 5 HOUR, NOW());

-- 6.7 测试用户（技能者）· 仲裁中（status=9）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610130401', @u4, @skill, 2, 1, '短视频账号头图设计', '账号头图与简介卡，需求方认为与参考风格偏差较大', '线上交付', '线上交付', '主校区', 100.00, 10.00, 90.00, 9, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, 2, 1, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 3 DAY, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 2 DAY);

-- 6.8 测试用户（技能者）· 已取消（status=6，技能者原因取消）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  cancel_reason, cancel_by, pay_method, pay_status,
  order_time, pay_time, cancel_time, create_time, update_time)
VALUES
 ('CS7202610130501', @u6, @skill, 5, 1, '期末复习资料整理', '整理各科复习提纲，因冲突取消', '线上交付', '线上交付', '南校区', 40.00, 4.00, 36.00, 6, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, '临时有考试安排，无法按时交付，已与需求方协商取消', 2, 2, 2, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY);

-- 6.9 测试用户（技能者）· 已超时（status=7，超时未交付 → 退款 + 违约金 + 信用 -5）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  cancel_reason, cancel_by, pay_method, pay_status,
  order_time, pay_time, pickup_time, cancel_time, create_time, update_time)
VALUES
 ('CS7202610130601', @u4, @skill, 4, 1, '校园风景壁纸精修（5张）', '校园风景壁纸精修，超时未交付由平台处理', '线上交付', '线上交付', '主校区', 70.00, 7.00, 63.00, 7, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, '超时未交付，平台自动退款并按规则扣除违约金', 3, 2, 2, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 2 DAY);

-- 6.10 测试用户（作为需求方）· 进行中 / 已交付（用户端「进行中的订单」与验收倒计时）
INSERT INTO orders
 (number, user_id, runner_id, type_id, mode, title, description, pickup_address, delivery_address, campus,
  reward_amount, platform_fee, runner_income, status, expected_time, timeout_time,
  pay_method, pay_status, order_time, pay_time, pickup_time, deliver_time, auto_accept_time, rework_count, create_time, update_time)
VALUES
 ('CS7202610132001', @skUser, @s3, 1, 1, '毕设答辩PPT美化（加急）', '导师要求明天上午前交付，需要统一图表样式与配色', '线上交付', '线上交付', '主校区', 90.00, 9.00, 81.00, 3, NOW() + INTERVAL 20 HOUR, NOW() + INTERVAL 18 HOUR, 2, 1, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 5 HOUR, NULL, NULL, 0, NOW() - INTERVAL 6 HOUR, NOW()),
 ('CS7202610132002', @skUser, @s3, 3, 1, '实验室宣传视频剪辑（已交付）', '实验室招新宣传片，两分钟，含字幕与配乐', '线上交付', '线上交付', '主校区', 120.00, 14.40, 105.60, 4, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 2 DAY, 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 5 HOUR, NOW() + INTERVAL 43 HOUR, 0, NOW() - INTERVAL 3 DAY, NOW());

-- ---------------------------------------------------------------------
-- 7. 评价（12 条：10 个五星 + 2 个四星，供评分与「好评 +2」信用流水）
-- ---------------------------------------------------------------------
INSERT INTO order_review (order_id, user_id, runner_id, score, quality_score, content, tags, is_anonymous, create_time)
SELECT o.id, o.user_id, o.runner_id, 5, 5,
       CONCAT('「', o.title, '」交付得又快又好，排版和配色都很满意，答辩加分了！'),
       '质量高,响应快', 0, o.finish_time + INTERVAL 3 HOUR
  FROM orders o
 WHERE o.runner_id = @skill AND o.status = 5
 ORDER BY o.finish_time DESC, o.id DESC
 LIMIT 12;

UPDATE order_review r
   JOIN (SELECT o.id FROM orders o WHERE o.runner_id = @skill AND o.status = 5
          ORDER BY o.finish_time DESC, o.id DESC LIMIT 2 OFFSET 5) x ON x.id = r.order_id
   SET r.score = 4, r.quality_score = 4,
       r.content = '整体不错，细节还可以再打磨一下，沟通很顺畅。',
       r.tags = '沟通顺畅,交付准时',
       r.is_anonymous = 1;

-- ---------------------------------------------------------------------
-- 8. 钱包与流水（余额 = 收入 + 充值 + 退款 − 支出 − 提现 − 违约金）
-- ---------------------------------------------------------------------
INSERT INTO wallet_account (user_id, balance, frozen_amount, total_income, total_expense, status, create_time)
SELECT @skUser, 0.00, 0.00, 0.00, 0.00, 1, NOW() - INTERVAL 60 DAY
WHERE NOT EXISTS (SELECT 1 FROM wallet_account WHERE user_id = @skUser);

SET @wallet = (SELECT id FROM wallet_account WHERE user_id = @skUser LIMIT 1);

-- 8.1 技能服务收入（与已完成订单一一对应）
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT @wallet, 1, o.runner_income, o.id, CONCAT('技能服务收入-', o.title), o.finish_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 5;

-- 8.2 充值
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time) VALUES
 (@wallet, 3, 500.00, NULL, '账户充值', NOW() - INTERVAL 55 DAY);

-- 8.3 作为需求方发单的支付支出（历史已完成订单）
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT @wallet, 2, o.reward_amount, o.id, CONCAT('发布悬赏支付-', o.title), o.pay_time
  FROM orders o WHERE o.user_id = @skUser AND o.status = 5 AND o.pay_status = 1;

-- 8.4 退款（取消 / 超时订单退回）
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT @wallet, 5, o.reward_amount, o.id, CONCAT('订单退款-', o.title),
       IFNULL(o.cancel_time, o.update_time)
  FROM orders o WHERE o.user_id = @skUser AND o.pay_status = 2;

-- 8.5 提现（待处理与已打款实际出账；已驳回不出账）
INSERT INTO withdraw_request (user_id, amount, status, apply_time, audit_time, pay_time, remark) VALUES
 (@skUser, 500.00, 1, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY + INTERVAL 3 HOUR, NOW() - INTERVAL 20 DAY + INTERVAL 6 HOUR, '演示数据·已打款'),
 (@skUser, 300.00, 1, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY + INTERVAL 2 HOUR, NOW() - INTERVAL 12 DAY + INTERVAL 5 HOUR, '演示数据·已打款'),
 (@skUser, 150.00, 1, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 5 DAY  + INTERVAL 2 HOUR, NOW() - INTERVAL 5 DAY  + INTERVAL 4 HOUR, '演示数据·已打款'),
 (@skUser, 200.00, 0, NOW() - INTERVAL 6 HOUR, NULL, NULL, '演示数据·待处理（提现审核演示）'),
 (@skUser, 800.00, 2, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 3 DAY  + INTERVAL 1 HOUR, NULL, '演示数据·已驳回（提现余额不足）');

INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT @wallet, 4, w.amount, NULL, CONCAT('提现申请-', IF(w.status = 0, '待处理', '已打款')), w.apply_time
  FROM withdraw_request w WHERE w.user_id = @skUser AND w.status IN (0, 1) AND w.remark LIKE '演示数据%';

-- 8.6 违约金（超时未交付，由技能者承担）
INSERT INTO wallet_transaction (wallet_id, type, amount, order_id, remark, create_time)
SELECT @wallet, 6, ROUND(o.reward_amount * 0.1, 2), o.id, CONCAT('超时未交付违约金-', o.title), o.cancel_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 7;

-- 8.7 汇总钱包账户（保证余额、累计收支与流水口径一致）
UPDATE wallet_account w SET
  w.total_income  = (SELECT IFNULL(SUM(t.amount), 0) FROM wallet_transaction t WHERE t.wallet_id = w.id AND t.type = 1),
  w.total_expense = (SELECT IFNULL(SUM(t.amount), 0) FROM wallet_transaction t WHERE t.wallet_id = w.id AND t.type IN (2, 4, 6)),
  w.balance       = (SELECT IFNULL(SUM(CASE WHEN t.type IN (1, 3, 5) THEN t.amount ELSE -t.amount END), 0)
                       FROM wallet_transaction t WHERE t.wallet_id = w.id),
  w.frozen_amount = (SELECT IFNULL(SUM(x.amount), 0) FROM withdraw_request x WHERE x.user_id = w.user_id AND x.status = 0),
  w.update_time   = NOW()
 WHERE w.user_id = @skUser;

-- ---------------------------------------------------------------------
-- 9. 信用分流水（覆盖全部 5 条规则：交付+1 / 好评+2 / 返修-3 / 超时-5 / 仲裁判责-10）
-- ---------------------------------------------------------------------
INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @skill, 1, '交付完成+1', o.id, o.finish_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 5;

INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @skill, 2, '好评+2', r.order_id, r.create_time
  FROM order_review r WHERE r.runner_id = @skill AND r.score = 5;

INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @skill, -3, '返修-3', o.id, o.update_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 8;

INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @skill, -5, '超时未交付-5', o.id, o.cancel_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 7;

INSERT INTO credit_log (skiller_id, delta, reason, order_id, create_time)
SELECT @skill, -10, '仲裁判责-10', o.id, o.update_time
  FROM orders o WHERE o.runner_id = @skill AND o.status = 9;

-- ---------------------------------------------------------------------
-- 10. 消息中心（测试用户收件箱：4 条未读 + 4 条已读）
-- ---------------------------------------------------------------------
INSERT INTO message (recipient_id, title, content, order_id, is_read, create_time) VALUES
 (@skUser, '收到新的悬赏', '悬赏大厅新增「毕业论文答辩PPT美化（25页）」，悬赏 ¥80，快去抢单吧', NULL, 0, NOW() - INTERVAL 35 MINUTE),
 (@skUser, '订单已交付', '您接单的「毕业纪念册排版设计」已提交交付物，等待需求方验收', (SELECT id FROM orders WHERE number = 'CS7202610130201'), 0, NOW() - INTERVAL 6 HOUR),
 (@skUser, '需求方申请返修', '「实验室招新H5页面视觉稿」被退回返修：配色偏暗，请 12 小时内重新交付', (SELECT id FROM orders WHERE number = 'CS7202610130301'), 0, NOW() - INTERVAL 8 HOUR),
 (@skUser, '作品审核通过', '恭喜！您的作品「校园活动海报设计集」已通过审核，定级 C3', NULL, 0, NOW() - INTERVAL 19 DAY),
 (@skUser, '订单已完成结算', '「团支部风采展示视频剪辑」已验收，¥88.00 已入账，信用分 +1', (SELECT id FROM orders WHERE number = 'CS7202610131002'), 1, NOW() - INTERVAL 20 MINUTE),
 (@skUser, '提现申请已提交', '您的 ¥200.00 提现申请已提交，平台将在 1 个工作日内审核打款', NULL, 1, NOW() - INTERVAL 6 HOUR),
 (@skUser, '平台公告', '校园技能工坊上线「作品集定级」机制：C3 作品可解锁每日更高接单额度', NULL, 1, NOW() - INTERVAL 3 DAY),
 (@skUser, '信用分变动提醒', '「校园风景壁纸精修（5张）」超时未交付，信用分 -5，请合理安排接单量', (SELECT id FROM orders WHERE number = 'CS7202610130601'), 1, NOW() - INTERVAL 2 DAY);

-- ---------------------------------------------------------------------
-- 11. 预约单（技能者端「预约单」+ 用户端「我的预约」，覆盖待确认/已确认/已完成/已拒绝）
-- ---------------------------------------------------------------------
INSERT INTO booking (service_item_id, order_id, user_id, skiller_id, expect_time, remark, status, create_time, update_time) VALUES
 ((SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 2 LIMIT 1), NULL, @u4, @skill,
  NOW() + INTERVAL 3 DAY, '演示数据·海报设计，希望周六下午沟通需求', 0, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR),
 ((SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 1 LIMIT 1),
  (SELECT id FROM orders WHERE number = 'CS7202610131101'), @u4, @skill,
  NOW() - INTERVAL 3 DAY, '演示数据·课程PPT美化，需含 2 轮修改', 2, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY),
 ((SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 7 LIMIT 1),
  (SELECT id FROM orders WHERE number = 'CS7202610131102'), @u5, @skill,
  NOW() + INTERVAL 1 DAY, '演示数据·两篇摘要翻译润色', 1, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 4 HOUR),
 ((SELECT id FROM service_item WHERE skiller_id = @skill AND category_id = 3 LIMIT 1), NULL, @u6, @skill,
  NOW() + INTERVAL 2 DAY, '演示数据·视频剪辑，档期已满暂不接单', 3, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
 (@cat3Svc, NULL, @skUser, @s3,
  NOW() + INTERVAL 2 DAY, '演示数据·预约陈技能的短视频剪辑服务', 0, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR),
 (@cat3Svc, NULL, @skUser, @s3,
  NOW() + INTERVAL 1 DAY, '演示数据·档期冲突，技能者已拒绝', 3, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY);

-- ---------------------------------------------------------------------
-- 12. 仲裁工单（待仲裁 1 条 + 已裁定放款技能者 1 条）
-- ---------------------------------------------------------------------
INSERT INTO dispute (order_id, raised_by, reason_type, description, evidence_urls, status, verdict, admin_id, create_time, update_time) VALUES
 ((SELECT id FROM orders WHERE number = 'CS7202610130401'), @u4, 1,
  '交付物与沟通时的参考风格偏差较大，希望退款或重新设计', '/assets/covers/poster.svg', 0, NULL, NULL,
  NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
 ((SELECT id FROM orders WHERE number = 'CS7202610131002'), @u5, 2,
  '交付时间比约定晚了几小时，但内容质量认可，双方已协商解决', '/assets/covers/video.svg', 2,
  '经核实交付内容符合约定标准，判定放款技能者，订单正常结算', 1,
  NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 18 HOUR);

-- ---------------------------------------------------------------------
-- 13. 收尾：按实际数据重算技能者统计口径（完成单数 / 评分 / 等级 / 信用分）
-- ---------------------------------------------------------------------
UPDATE skiller s SET
  s.completed_orders = (SELECT COUNT(*) FROM orders o WHERE o.runner_id = s.id AND o.status = 5),
  s.score            = ROUND(IFNULL((SELECT AVG(r.score) FROM order_review r WHERE r.runner_id = s.id), 5.0), 1),
  s.credit_score     = 100 + IFNULL((SELECT SUM(c.delta) FROM credit_log c WHERE c.skiller_id = s.id), 0)
 WHERE s.id = @skill;

-- 接单等级：与 RunnerServiceImpl 的阈值保持一致（20 铜牌 / 50 银牌 / 100 金牌）
UPDATE skiller SET runner_level = CASE
    WHEN completed_orders >= 100 THEN 4
    WHEN completed_orders >= 50  THEN 3
    WHEN completed_orders >= 20  THEN 2
    ELSE 1 END
 WHERE id = @skill;

-- 发单数口径与订单表对齐（含取消/超时单，反映真实发单量）
UPDATE user SET publish_order_count = (SELECT COUNT(*) FROM orders WHERE user_id = @skUser)
 WHERE id = @skUser;

-- ---------------------------------------------------------------------
-- 14. 清理校验表并输出核对结果
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS _seed7_guard;

SELECT '测试用户技能者工作台数据核对' AS 项, s.id AS 技能者id, s.name AS 姓名,
       s.completed_orders AS 累计完成单, s.score AS 评分, s.credit_score AS 信用分,
       s.runner_level AS 接单等级, s.skill_level AS 作品定级,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND status = 3) AS 进行中,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND status = 4) AS 已交付,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND status = 5) AS 已完成,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND status = 8) AS 返修中,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND status = 9) AS 仲裁中,
       (SELECT COUNT(*) FROM orders WHERE runner_id = s.id AND DATE(finish_time) = CURDATE() AND status = 5) AS 今日完成,
       (SELECT IFNULL(SUM(runner_income),0) FROM orders WHERE runner_id = s.id AND DATE(finish_time) = CURDATE() AND status = 5) AS 今日收入,
       (SELECT balance FROM wallet_account WHERE user_id = s.user_id) AS 钱包余额,
       (SELECT frozen_amount FROM wallet_account WHERE user_id = s.user_id) AS 冻结金额,
       (SELECT COUNT(*) FROM withdraw_request WHERE user_id = s.user_id) AS 提现记录,
       (SELECT COUNT(*) FROM message WHERE recipient_id = s.user_id AND is_read = 0) AS 未读消息,
       (SELECT COUNT(*) FROM service_item WHERE skiller_id = s.id) AS 服务数,
       (SELECT COUNT(*) FROM portfolio WHERE skiller_id = s.id) AS 作品数
  FROM skiller s WHERE s.id = @skill;

SELECT '悬赏大厅（待接单）' AS 项, COUNT(*) AS 数量,
       IFNULL(SUM(reward_amount), 0) AS 悬赏总额
  FROM orders WHERE status = 2 AND pay_status = 1;
