# JMeter 压测方案（CampusSkill v11 可信交易保障体系）

> 定位：毕业设计第 7 章「系统实验与分析」的标准压测产物。JMeter 方案用于论文与答辩展示，
> `tools/loadtest/bench.mjs` 是免安装的 Node 压测 runner，两条路径产生同口径数据。

## 环境准备

```bash
# 1. 应用运行于 localhost:8080（start.bat 或 java -jar）
# 2. 压测数据预置（幂等键场景可跳过，读路径与抢单需要）
mysql --default-character-set=utf8mb4 -uroot campus_runner < sql/data_factory.sql
mysql --default-character-set=utf8mb4 -uroot campus_runner -e "CALL proc_factory_skillers('BENCH', 60); CALL proc_factory_orders('BENCH', 50); CALL proc_factory_users('BENCH', 60);"
# 压测后清理
mysql --default-character-set=utf8mb4 -uroot campus_runner < sql/loadtest_reset.sql
```

## 方案一：JMeter（需要 JDK 8+，[下载 jmeter](https://jmeter.apache.org/download_jmeter.cgi)）

```bash
# 大厅读路径（缓存 A/B：先以 campus.cache.enabled=false 启动应用跑一轮基线，
# 再以默认（true）重启跑一轮，对比 QPS 与 P95）
jmeter -n -t hall_browse.jmx -l results_hall.jtl -e -o report_hill_cached

# 幂等重复支付（20 线程集合点同键并发充值，聚合报告中 code=1 的样本应恰好为 1）
jmeter -n -t idempotent_pay.jmx -l results_idem.jtl -e -o report_idem
```

- 抢单竞态：JMeter 需要为每个技能者换发 token（CSV 数据集 + 登录采样器），本项目用 `bench.mjs grab`
  直接完成同样的「N 人并发抢同一单」并统计"恰好一人成功"组数，推荐用 runner 跑。

## 方案二：Node runner（零依赖，Node 18+）

```bash
cd tools/loadtest
node bench.mjs hall --users 30 --duration 15      # 读路径
node bench.mjs grab --groups 5 --skillers 30      # 抢单竞态
node bench.mjs idem  --threads 20 --rounds 3      # 幂等重复支付（自动断言账实相符）
node bench.mjs all                                # 三场景快速串跑
```

输出为单行 JSON（场景/请求数/成功失败/QPS/P50/P95/P99），可直接粘贴进实验报告。

## A/B 对比口径（论文实验设计）

| 实验 | 基线 | 优化 | 观测指标 |
|---|---|---|---|
| 缓存收益 | `--campus.cache.enabled=false` 启动 | 默认启动（缓存开） | hall 列表 QPS、P95/P99 |
| 抢单优化 | 应用启动参数 `--campus.grab.legacy=true` | 默认启动（Lua 原子路径） | 抢单成功率、P95、恰好一人成功组数 |
| 幂等正确性 | —（语义验证，非性能） | — | 每轮恰好 1 次成功、余额增量 = 轮数 × 10 |
| 对账闭环 | 压测结束后立即 `POST /admin/reconcile/run` | — | 差异钱包数 = 0 |
