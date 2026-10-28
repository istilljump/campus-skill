# CampusSkill 校园技能工坊 —— 校园技能互助服务市场

基于 SpringBoot 2.7 + MyBatis + Redis + JWT 的 C2C 校园技能服务撮合平台（Maven 多模块）。有技能的同学（设计、剪辑、编程、摄影等）经**作品认证**后挂牌接单；有需求的同学发布**悬赏**或**预约**技能者的服务，平台担保交易，订单完成后沉淀**技能信用档案**。

> 前身：campus-runner 校园跑腿平台（v4 改造为技能工坊，数据库与接口均已迁移，见 `sql/upgrade_v4_skill.sql`）。

## 模块结构

| 模块 | 说明 |
|---|---|
| campus-common | 通用层：常量、异常、JWT/Redis/OSS 工具、统一返回 |
| campus-pojo | 实体 / DTO / VO，与数据库表一一对应 |
| campus-server | 业务层：Mapper、Service、Controller（三端）、定时任务 |

## 核心功能

- **三端隔离**：用户端 `/user/**`、技能者端 `/skiller/**`、管理端 `/admin/**`，独立 JWT 拦截器
- **双模式撮合**：
  - 悬赏模式——用户发单定价 → 技能者大厅抢单（Redis 锁 + 条件更新防重复）
  - 预约模式——技能者上架**服务货架**（定价/交付天数/线上或线下）→ 用户预约 → 技能者确认 → 下单支付
- **交付验收闭环（v4 核心）**：接单 → 开始服务 → **上传交付物交付**（状态4）→ 用户**验收**（结算）/ **返修**（≤2次）/ **仲裁**（管理端裁定：退款用户 / 放款技能者 / 驳回）；已交付 48 小时未验收自动确认
- **结算时点 = 验收**：技能者到账 = 悬赏 - 类目服务费率（默认10%），生成双方钱包流水；预支付资金托管
- **钱包体系**：懒开户、原子余额变动、充值、提现（密码校验 + 冻结 + 审核）、全量流水
- **技能者体系**：实名/学号认证 + **作品集认证**（管理员评审作品评定 C1/C2/C3 等级）、接单等级自动晋升、评分自动重算、每日接单上限
- **信用分引擎（v4）**：交付完成 +1、好评 +2、返修 -3、超时未交付 -5（并扣违约金）、仲裁判责 -10；信用等级 C3≥90 / C2≥70 / C1<70；管理端信用榜单
- **定时任务**：15 分钟未支付自动取消、超时无人接单自动退款、超时未交付（状态3/8）退款 + 违约金 + 信用-5、超48h 未验收自动验收
- **消息中心 + WebSocket**：关键节点站内信，`/ws/{token}` 实时推送，未读徽标、断线重连
- **个性化推荐引擎（v8）**：浏览/预约/成交行为自动埋点（`user_behavior` 表），混合推荐 = ItemCF 协同过滤 + 内容特征（类目偏好/标签 Jaccard/价格贴近度）+ 质量因子三路加权（0.45/0.30/0.25）；首页「猜你喜欢」、服务详情「看了又看」均带推荐理由；无行为用户冷启动回退「热门精选」，已成交与本人挂牌服务自动排除
- **管理端**：数据大屏（近7/30天成交趋势、热门技能类目 TOP5、校区分布）、作品审核、认证审核、**仲裁工单**、提现审核、技能者管理、技能类目管理（含费率）、**信用榜单**

## 快速启动

环境要求：JDK 8+（17 实测通过）、Maven 3.6+、MySQL 8.0、Redis 5+

```bash
# 1. 建库导入（campus.sql 已含 v4 全部表结构与技能类目种子数据）
mysql -uroot -e "CREATE DATABASE campus_runner DEFAULT CHARACTER SET utf8mb4"
mysql -uroot campus_runner < campus.sql
# 可选：导入演示数据（演示账号、作品、服务、近7天成交订单与评价，答辩演示推荐）
mysql -uroot campus_runner < sql/seed_demo.sql
# 旧库（v3 及以前）升级：
mysql -uroot campus_runner < sql/upgrade_v3_message.sql
mysql -uroot campus_runner < sql/upgrade_v4_skill.sql
# v5 管理端功能增强（系统公告表，旧库必须执行；新库用 campus.sql 可跳过）：
mysql -uroot campus_runner < sql/upgrade_v5_admin_console.sql
# v8 个性化推荐（user_behavior 行为表，旧库必须执行；新库用 campus.sql 可跳过；可重复执行，自动补 source 列）：
mysql -uroot campus_runner < sql/upgrade_v8_recommend.sql
# v9 性能优化（orders 技能者维度索引，建议执行）：
mysql -uroot campus_runner < sql/upgrade_v9_performance.sql

# 2. 修改 campus-server/src/main/resources/application-dev.yml 的数据库/Redis 连接

# 3. 构建启动
mvn package -DskipTests
java -jar campus-server/target/campus-server-1.0-SNAPSHOT.jar
```

启动后访问 `http://localhost:8080/doc.html` 查看在线接口文档。

## 前端页面

| 入口 | 地址 | 说明 |
|---|---|---|
| 🖥️ 管理端控制台（工程版·推荐） | http://localhost:8080/admin-web/ | Vue3 + Vite + Element Plus + Pinia + ECharts 蓝白活力风桌面工作台：数据大屏、订单、预约、技能者、作品审核、仲裁工单、信用榜单、认证/提现审核、类目、系统公告；源码见 `admin-web/` |
| 🌐 Web 门户（桌面端·推荐） | http://localhost:8080/web-app/ | Vue3 + Vite + Element Plus 蓝白活力风桌面工作台：用户端与技能者端业务一体（概览/悬赏大厅/技能市场/发布悬赏/订单全流程/钱包/消息/地址/资料/入驻 + 接单台/服务/作品/技能者中心），双身份登录自动联动导航；源码见 `web-app/` |

### 管理端工程版（admin-web）

- 源码位置：仓库根目录 `admin-web/`（Vue 3.5 + Vite 6 + Element Plus + Pinia + Vue Router + ECharts + Axios）
- 开发模式：`npm install && npm run dev`，Vite 已配置 `/admin → http://localhost:8080` 代理
- 部署模式：`npm run build` 后将 `dist/` 拷贝到 `campus-server/src/main/resources/static/admin-web/`，随 jar 一起发布（hash 路由）
- **蓝白活力主题**：与 Web 门户同一设计语言（浅灰白底 + B 站蓝 #00AEEC 主色 + 粉色 #FB7299 点缀，Element Plus 品牌色覆盖，无新增依赖）
- **数据大屏**：核心指标、成交趋势、热门技能类目 TOP5、各校区订单占比、订单状态分布，30 秒自动刷新 + 全屏演示模式
- **系统公告**（v5 新增）：`系统配置 → 公告管理` 发布公告，实时推送全体用户与技能者收件箱（在线用户 WebSocket 弹窗提醒），历史记录含送达人数；接口 `POST /admin/message/broadcast`、`GET /admin/message/page`
- **预约管理**（v5 新增）：`业务管理 → 预约管理` 查看全平台预约单（状态筛选/联查用户与技能者），支持管理员取消异常预约（仅待确认，双端站内通知）；接口 `GET /admin/booking/page`、`PUT /admin/booking/{id}/cancel`

- 管理端账号：`admin / 123456`
- 微信登录未配置 appid 时自动降级为本地联调模式：任意 code 登录，openid 为 `campus_dev_{code}`；认证通过后即可登录技能者端

### 演示账号（导入 seed_demo.sql 后可用）

| 端 | 登录方式 | 说明 |
|---|---|---|
| 管理端 | admin / 123456 | 含近 7 天成交数据、信用榜单、作品/认证/仲裁工单演示 |
| Web 门户 · 用户身份 | 账号 `demo1` / `123456` | 张晓文：已有 3 笔已完成订单、余额 ¥210 |
| Web 门户 · 用户身份 | 账号 `demo4` / `123456` | 李梦琪：有已完成订单与默认地址 |
| Web 门户 · 技能者身份 | 账号 `demo2` / `123456` | 陈技能：C3 作品定级、信用 103、在售 2 项服务、作品集 2 件 |
| Web 门户 · 技能者身份 | 账号 `demo3` / `123456` | 林小艺：C2 作品定级、在售海报设计/摄影服务 |

> 登录形态：用户端/技能者端支持**账号密码登录 + 注册**（主入口，MD5 加盐存储）与**体验码登录**（联调降级入口，输入任意码即可，对应微信登录未配置 appid 的本地降级模式）。密码可在「我的 → 修改密码」变更。详见 `sql/upgrade_v6_account_login.sql`。

### 答辩演示数据（seed_demo_v7.sql，推荐）

`sql/seed_demo_v7.sql` 面向答辩演示，补齐 **悬赏大厅待接单、进行中/已交付/返修中/仲裁中订单、技能者工作台全部板块（接单/收益/提现/消息/资料/服务/作品/预约）**，并刻意保留**空态与异常态**（已取消、已超时、已驳回、待审核）便于现场演示三态。

```bash
mysql -uroot campus_runner < sql/seed_demo_v7.sql   # 可重复执行（幂等），按订单号前缀 CS7 识别并重建自身数据
mysql -uroot campus_runner < sql/seed_behavior_v8.sql  # 推荐引擎演示行为数据（幂等）：让「猜你喜欢/看了又看」现场直接产出个性化结果
node tools/verify-demo.mjs                          # 演示就绪自检：逐板块拉接口确认都有内容（32 项）
```

**演示主账号 `测试用户`**（`demo1` / `123456`，密码同账号体系；提现密码 `123456`）：用户端与技能者端双身份，技能者档案为「累计完成 21 单 · 铜牌 · 评分 4.8 · 信用 123 · 作品定级 C3 · 今日收入 ¥142 · 余额 ¥925（含冻结 ¥200）」，覆盖接单/收益/提现/消息/服务/作品/预约全部板块。

> 演示环境**不依赖外网**：favicon/应用图标、作品封面占位图全部内置在 `static/`（`tools/icons/`、`tools/covers/` 可重新生成），断网也能正常显示。

### 前端构建与静态资源

```bash
node tools/build-frontends.mjs   # 依次构建 web-app / admin-web 并同步 dist → campus-server 静态目录
```

避免"源码改了但 jar 里仍是旧页面"。

### 端到端冒烟测试

无需浏览器的全链路回归脚本（覆盖悬赏、预约、返修、仲裁、提现、消息、统计、推荐引擎 68 项断言，断言基于运行基线做增量比较，可重复执行）：

```bash
node e2e/smoke.mjs   # 在仓库根目录执行，需先启动应用
```

## 论文与文档资产（docs/）

- `docs/论文图/`：8 张论文插图（系统架构 / 核心 ER 图 / 支付托管与结算时序 / 抢单并发控制时序 / 订单状态机 / 推荐引擎流程 / 用例图 / 部署图），含 Mermaid 源码（`.mmd`）与渲染好的矢量图（`.svg`，可直接插入 Word）
- `docs/论文章节初稿/`：系统测试、系统关键技术实现、系统实验与分析三章初稿，全部数据引用自仓库内可复现脚本与报告

## 安全说明（v10）

- **密码存储**：账号密码已升级为 **BCrypt** 慢哈希存储（`PasswordUtil`）；存量 MD5(salt+明文) 口令在登录成功时自动重哈希升级，无需重置用户密码。提现密码与盐值列保留旧口径以兼容历史数据（列宽限制），迁移计划见论文「安全性设计」。
- **体验码登录**：`campus.guest-login.enabled`（默认 `true`，任意体验码即可登录，仅供本地联调与答辩演示）；**生产环境务必置为 `false`** 关闭降级入口。
- **演示密码**：`admin / 123456` 与各演示账号密码仅为答辩演示用途，请勿用于真实环境。

## 性能优化与质量保障（v9）

- **抢单接口高并发优化**：Redis Lua 脚本将「限流计数 + 订单占用占位」合并为 1 次往返（原 INCR/EXPIRE/SETNX/DEL 四次）；接单权限校验加本地缓存（允许 30s/拒绝 5s），热点请求不打库；orders 表补充技能者维度索引。保留 `--campus.grab.legacy=true` 启动参数可切换优化前实现做 A/B 对比。
  100 并发 × 8 轮公平 A/B 压测（`tools/loadtest-grab.mjs`）：**QPS 1226 → 1537（+25%）、平均时延 58ms → 47ms（-19%）、P95 78ms → 60ms（-23%）**，正确性不变（每轮恰好 1 人抢单成功），报告见 `tools/loadtest/report.md`
- **资金链路测试**：`WalletServiceTest`（懒开户/余额变动/不足拒绝 5 项单测）+ `GrabWalletConcurrencyIT`（30 人并发抢单竞态、并发充值扣款对账，独立 `campus_runner_test` 库，`mvn test -Dtest=GrabWalletConcurrencyIT`）。并发对账测试**发现并修复了懒开户 check-then-insert 竞态**（并发首充触发唯一键冲突与 gap lock 死锁，现为 Redis 锁串行化开户 + 锁定读等待）
- **推荐效果对比实验**：行为埋点带来源标记（1市场列表 2猜你喜欢 3看了又看），管理端 `GET /admin/statistics/recommendEffect` 输出各来源转化率；实验脚本 `tools/recommend-experiment.mjs` 模拟偏好驱动用户（10 用户 × 6 曝光）：**猜你喜欢兴趣类目命中率 36.7%、预约转化率 14.7%，市场列表对照组均为 0%**，报告见 `tools/loadtest/recommend-experiment-report.md`

## 业务约定

- 订单状态：1待支付 2待接单 3进行中 4已交付 5已完成 6已取消 7已超时 8返修中 9仲裁中
- 撮合模式：mode 1悬赏 2服务预约（预约确认后走同一订单/钱包/结算链路）
- 结算时点：用户验收 / 48h 自动验收 / 仲裁放款；退款场景：取消、超时无人接单、超时未交付、仲裁退款
- 信用规则：交付+1、好评+2、返修-3、超时未交付-5、仲裁判责-10
- 抢单接口限流：同一技能者 10 秒内最多 5 次
- 完整接口清单见 [接口文档-三端.md](接口文档-三端.md)，表结构见 [数据库设计文档.md](数据库设计文档.md)
