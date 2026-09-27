# campus-runner 校园综合跑腿服务平台

基于 SpringBoot 2.7 + MyBatis + Redis + JWT 的 C2C 校园跑腿平台（ Maven 多模块）。发单用户发布跑腿需求（取快递/代买/代办），认证跑腿员在大厅抢单，平台按类型费率收取服务费。

## 模块结构

| 模块 | 说明 |
|---|---|
| campus-common | 通用层：常量、异常、JWT/Redis/OSS 工具、统一返回 |
| campus-pojo | 实体 / DTO / VO，与数据库表一一对应 |
| campus-server | 业务层：Mapper、Service、Controller（三端）、定时任务 |

## 核心功能

- **三端隔离**：用户端 `/user/**`、跑腿员端 `/runner/**`、管理端 `/admin/**`，独立 JWT 拦截器（跑腿员 token 携带 runnerId 身份声明）
- **订单流程**：发单 → 支付 → 大厅抢单（Redis 锁 + 数据库条件更新防重复）→ 确认取件 → 确认送达自动结算（扣平台服务费、跑腿员到账、双方流水）→ 用户确认 → 评价
- **钱包体系**：懒开户、原子余额变动（余额不透支）、充值、提现（密码校验 + 冻结 + 审核）、全量流水
- **跑腿员体系**：实名/学号认证审核、等级自动晋升（20/50/100 单）、评分自动重算、每日接单上限
- **定时任务**：15 分钟未支付自动取消、超时无人接单自动退款、超时未送达退款并扣跑腿员违约金
- **管理端看板**：订单统计、跑腿员管理、认证审核、提现审核、订单类型管理（含费率）

## 快速启动

环境要求：JDK 8+（17 实测通过）、Maven 3.6+、MySQL 8.0、Redis 5+

```bash
# 1. 建库导入（root 无密码场景请自行调整 application-dev.yml）
mysql -uroot -e "CREATE DATABASE campus_runner DEFAULT CHARACTER SET utf8mb4"
mysql -uroot campus_runner < campus.sql

# 2. 修改 campus-server/src/main/resources/application-dev.yml 的数据库/Redis 连接

# 3. 构建启动
mvn package -DskipTests
java -jar campus-server/target/campus-server-1.0-SNAPSHOT.jar
```

启动后访问 `http://localhost:8080/doc.html` 查看在线接口文档。

## 前端页面（零构建，随服务直接访问）

| 入口 | 地址 | 说明 |
|---|---|---|
| 🖥️ 管理端控制台 | http://localhost:8080/admin-app/ | Vue3 + Element Plus：数据看板、订单、跑腿员、认证/提现审核、类型管理 |
| 🙋 用户端 H5 | http://localhost:8080/user-app/ | Vant 移动风格：发单、订单跟踪、钱包、评价、地址、认证申请 |
| 🏃 跑腿员端 H5 | http://localhost:8080/runner-app/ | 抢单大厅、取件/送达、收入提现、个人中心 |

设计规范基于 [design-system-starter](https://github.com/aiskillstore/marketplace/tree/main/skills/ariegoldkin/design-system-starter) 令牌体系（蓝色品牌色 / Inter / 4px 间距），见 `static/assets/tokens.css`；Vue3 / Element Plus / Vant 依赖已自托管于 `static/assets/vendor/`，无需外网。

- 管理端账号：`admin / 123456`
- 微信登录未配置 appid 时自动降级为本地联调模式：任意 code 登录，openid 为 `campus_dev_{code}`；认证通过后即可登录跑腿员端

## 业务约定

- 订单状态：1待支付 2待接单 3进行中 4已送达 5已完成 6已取消 7已超时
- 订单类型自带平台服务费率（默认 10%），跑腿员实得 = 悬赏金额 - 服务费
- 抢单接口限流：同一跑腿员 10 秒内最多 5 次
- 完整接口清单见 [接口文档-三端.md](接口文档-三端.md)，表结构见 [数据库设计文档.md](数据库设计文档.md)
