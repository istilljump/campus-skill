package com.campus.runner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏跨表聚合查询（交易漏斗 / 资金与信任 / 时段热度）
 */
@Mapper
public interface StatisticsMapper {

    //托管中金额：钱包支付、已支付、尚未结算（待接单/进行中/已交付/返修中）的悬赏合计
    @Select("select ifnull(sum(reward_amount), 0) from orders " +
            "where pay_method = 2 and pay_status = 1 and status in (2, 3, 4, 8)")
    BigDecimal sumEscrow();

    //钱包总余额
    @Select("select ifnull(sum(balance), 0) from wallet_account")
    BigDecimal sumWalletBalance();

    //提现在途（待审核）
    @Select("select ifnull(sum(amount), 0) from withdraw_request where status = 0")
    BigDecimal sumWithdrawPending();

    //未处置风控告警
    @Select("select count(*) from risk_control_log where status = 0")
    int countRiskUnhandled();

    //近7天风控告警
    @Select("select count(*) from risk_control_log where create_time > date_sub(now(), interval 7 day)")
    int countRisk7d();

    //交易转化漏斗：单条聚合给出五阶段计数
    @Select("select count(*) as published, " +
            "sum(case when pay_status in (1, 2) then 1 else 0 end) as paid, " +
            "sum(case when runner_id is not null and pay_status in (1, 2) then 1 else 0 end) as accepted, " +
            "sum(case when deliver_time is not null then 1 else 0 end) as delivered, " +
            "sum(case when status = 5 then 1 else 0 end) as completed " +
            "from orders")
    Map<String, Object> statFunnel();

    //订单发布时段分布（近 days 天，按小时聚合）
    @Select("select hour(create_time) as h, count(*) as c from orders " +
            "where create_time > #{begin} group by hour(create_time)")
    List<Map<String, Object>> statHourly(@Param("begin") LocalDateTime begin);
}
