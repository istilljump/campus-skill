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

# 2. 修改 campus-server/src/main/resources/application-dev.yml 的数据库/Redis 连接

# 3. 构建启动
mvn package -DskipTests
java -jar campus-server/target/campus-server-1.0-SNAPSHOT.jar
```

启动后访问 `http://localhost:8080/doc.html` 查看在线接口文档。

## 前端页面

| 入口 | 地址 | 说明 |
|---|---|---|
| 🖥️ 管理端控制台（工程版·推荐） | http://localhost:8080/admin-web/ | Vue3 + Vite + Element Plus + Pinia + ECharts：数据大屏、订单、技能者、作品审核、仲裁工单、信用榜单、认证/提现审核、类目管理；源码见 `admin-web/` |
| 🖥️ 管理端控制台（零构建版） | http://localhost:8080/admin-app/ | 单文件版，功能同上（无数据大屏） |
| 🙋 用户端 H5 | http://localhost:8080/user-app/ | Vant 移动风格：发布悬赏、技能市场/预约、交付验收、钱包、评价 |
| 🎨 技能者端 H5 | http://localhost:8080/skiller-app/ | 悬赏大厅、交付/返修、服务上架、作品集、预约管理、收入提现 |

### 管理端工程版（admin-web）

- 源码位置：仓库根目录 `admin-web/`（Vue 3.5 + Vite 6 + Element Plus + Pinia + Vue Router + ECharts + Axios）
- 开发模式：`npm install && npm run dev`，Vite 已配置 `/admin → http://localhost:8080` 代理
- 部署模式：`npm run build` 后将 `dist/` 拷贝到 `campus-server/src/main/resources/static/admin-web/`，随 jar 一起发布（hash 路由）
- **数据大屏**：核心指标、成交趋势、热门技能类目 TOP5、各校区订单占比、订单状态分布，30 秒自动刷新 + 全屏演示模式

- 管理端账号：`admin / 123456`
- 微信登录未配置 appid 时自动降级为本地联调模式：任意 code 登录，openid 为 `campus_dev_{code}`；认证通过后即可登录技能者端

### 演示账号（导入 seed_demo.sql 后可用）

| 端 | 登录方式 | 说明 |
|---|---|---|
| 管理端 | admin / 123456 | 含近 7 天成交数据、信用榜单、作品/认证/仲裁工单演示 |
| 用户端 H5 | 登录 code 输入 `demo1` | 张晓文：已有 3 笔已完成订单、余额 ¥210 |
| 用户端 H5 | 登录 code 输入 `demo4` | 李梦琪：有已完成订单与默认地址 |
| 技能者端 H5 | 登录 code 输入 `demo2` | 陈技能：C3 作品定级、信用 103、在售 2 项服务、作品集 2 件 |
| 技能者端 H5 | 登录 code 输入 `demo3` | 林小艺：C2 作品定级、在售海报设计/摄影服务 |

### 端到端冒烟测试

无需浏览器的全链路回归脚本（覆盖悬赏、预约、返修、仲裁、提现、消息、统计 62 项断言）：

```bash
node e2e/smoke.mjs   # 在仓库根目录执行，需先启动应用
```

## 业务约定

- 订单状态：1待支付 2待接单 3进行中 4已交付 5已完成 6已取消 7已超时 8返修中 9仲裁中
- 撮合模式：mode 1悬赏 2服务预约（预约确认后走同一订单/钱包/结算链路）
- 结算时点：用户验收 / 48h 自动验收 / 仲裁放款；退款场景：取消、超时无人接单、超时未交付、仲裁退款
- 信用规则：交付+1、好评+2、返修-3、超时未交付-5、仲裁判责-10
- 抢单接口限流：同一技能者 10 秒内最多 5 次
- 完整接口清单见 [接口文档-三端.md](接口文档-三端.md)，表结构见 [数据库设计文档.md](数据库设计文档.md)
