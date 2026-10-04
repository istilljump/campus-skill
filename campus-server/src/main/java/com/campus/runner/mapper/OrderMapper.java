package com.campus.runner.mapper;

import com.campus.runner.dto.OrdersPageQueryDTO;
import com.campus.runner.entity.Orders;
import com.campus.runner.vo.OrderHallVO;
import com.campus.runner.vo.OrderStatisticsVO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    @Insert("insert into orders (number, user_id, runner_id, type_id, mode, service_item_id, title, description, pickup_address, delivery_address, campus, " +
            "reward_amount, platform_fee, runner_income, status, expected_time, timeout_time, pay_method, pay_status, " +
            "order_time, pay_time, create_time, update_time, deliverable_url, deliverable_note, deliver_time, rework_count, auto_accept_time) " +
            "values (#{number}, #{userId}, #{runnerId}, #{typeId}, #{mode}, #{serviceItemId}, #{title}, #{description}, #{pickupAddress}, #{deliveryAddress}, #{campus}, " +
            "#{rewardAmount}, #{platformFee}, #{runnerIncome}, #{status}, #{expectedTime}, #{timeoutTime}, #{payMethod}, #{payStatus}, " +
            "#{orderTime}, #{payTime}, #{createTime}, #{updateTime}, #{deliverableUrl}, #{deliverableNote}, #{deliverTime}, #{reworkCount}, #{autoAcceptTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Orders order);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select * from orders where number = #{number}")
    Orders getByNumber(String number);

    int update(Orders order);

    /**
     * 抢单：仅当订单仍处于待接单状态时更新成功，利用数据库行级条件保证并发抢单只有一人成功
     * @return 影响行数，1 表示抢单成功，0 表示订单已被抢或状态不符
     */
    @Update("update orders set runner_id = #{runnerId}, status = 3, update_time = now() " +
            "where id = #{id} and status = 2 and pay_status = 1")
    int grabOrder(@Param("id") Long id, @Param("runnerId") Long runnerId);

    /**
     * 用户端-我的订单分页
     */
    Page<Orders> pageUserOrders(@Param("userId") Long userId, @Param("q") OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 技能者端-订单大厅分页（待接单订单）
     */
    Page<OrderHallVO> pageHallOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 技能者端-接单记录分页
     */
    Page<Orders> pageRunnerOrders(@Param("runnerId") Long runnerId, @Param("q") OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端-订单列表分页
     */
    Page<Orders> pageAdminOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 类型关联订单数（删除类型前校验）
     */
    @Select("select count(1) from orders where type_id = #{typeId}")
    int countByTypeId(Long typeId);

    /**
     * 平台订单统计
     */
    @Select("select" +
            " (select count(1) from orders) as totalOrders," +
            " (select count(1) from orders where date(create_time) = curdate()) as todayOrders," +
            " (select count(1) from orders where status = 2) as pendingCount," +
            " (select count(1) from orders where status = 3) as inProgressCount," +
            " (select count(1) from orders where status = 5) as completedCount," +
            " (select count(1) from orders where status = 6) as cancelledCount," +
            " (select ifnull(sum(reward_amount), 0) from orders where pay_status = 1) as totalReward," +
            " (select ifnull(sum(platform_fee), 0) from orders where pay_status = 1) as totalPlatformFee")
    OrderStatisticsVO statistics();

    /**
     * 技能者当日已接单数（进行中+已送达）
     */
    @Select("select count(1) from orders where runner_id = #{runnerId} and status in (3, 4) and date(update_time) = curdate()")
    int countRunnerTodayOrders(@Param("runnerId") Long runnerId);

    /**
     * 技能者今日收入（今日完成订单的实得金额合计）
     */
    @Select("select ifnull(sum(runner_income), 0) from orders " +
            "where runner_id = #{runnerId} and status = 5 and date(finish_time) = curdate()")
    BigDecimal sumRunnerTodayIncome(@Param("runnerId") Long runnerId);

    /**
     * 时间区间内技能者完成订单数
     */
    @Select("select count(1) from orders where runner_id = #{runnerId} and status = 5 " +
            "and date(finish_time) between #{begin} and #{end}")
    int countRunnerOrdersBetween(@Param("runnerId") Long runnerId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 时间区间内技能者收入合计
     */
    @Select("select ifnull(sum(runner_income), 0) from orders where runner_id = #{runnerId} and status = 5 " +
            "and date(finish_time) between #{begin} and #{end}")
    BigDecimal sumRunnerIncomeBetween(@Param("runnerId") Long runnerId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 超时未支付订单（下单15分钟仍未支付）
     */
    @Select("select * from orders where status = 1 and order_time <= now() - interval 15 minute")
    List<Orders> findUnpaidTimeoutOrders();

    /**
     * 超时未交付订单（进行中或返修中且超过超时时间）
     */
    @Select("select * from orders where status in (3, 8) and timeout_time is not null and timeout_time <= #{now}")
    List<Orders> findOverdueDeliveryOrders(@Param("now") LocalDateTime now);

    /**
     * 到达自动验收截止时间仍为已交付的订单（48小时未验收自动确认）
     */
    @Select("select * from orders where status = 4 and auto_accept_time is not null and auto_accept_time <= #{now}")
    List<Orders> findAutoAcceptOrders(@Param("now") LocalDateTime now);

    /**
     * 超时无人接单订单（已支付但超过超时时间仍待接单）
     */
    @Select("select * from orders where status = 2 and pay_status = 1 and timeout_time is not null and timeout_time <= #{now}")
    List<Orders> findUnclaimedTimeoutOrders(@Param("now") LocalDateTime now);

    /**
     * 数据大屏-按日统计订单趋势（begin 及以后的每一天）
     */
    @Select("select date_format(create_time, '%Y-%m-%d') as date, count(*) as orderCount, " +
            "sum(case when status in (4, 5) then 1 else 0 end) as completedCount, " +
            "ifnull(sum(case when pay_status = 1 then reward_amount else 0 end), 0) as amount " +
            "from orders where create_time >= #{begin} " +
            "group by date_format(create_time, '%Y-%m-%d') order by date")
    List<Map<String, Object>> statDailyTrend(@Param("begin") LocalDate begin);

    /**
     * 数据大屏-技能类目订单量 TOP N
     */
    @Select("select t.name as name, count(*) as count from orders o " +
            "join skill_category t on o.type_id = t.id " +
            "group by t.name order by count desc limit #{limit}")
    List<Map<String, Object>> statTypeRank(@Param("limit") int limit);

    /**
     * 数据大屏-各校区订单量分布
     */
    @Select("select ifnull(campus, '未填写') as name, count(*) as count from orders " +
            "group by ifnull(campus, '未填写') order by count desc")
    List<Map<String, Object>> statCampusRank();

    /**
     * 技能者端-按日统计完成单量与收入（按送达时间）
     */
    @Select("select date_format(finish_time, '%Y-%m-%d') as date, count(*) as orderCount, " +
            "ifnull(sum(runner_income), 0) as amount from orders " +
            "where runner_id = #{runnerId} and status in (4, 5) and finish_time >= #{begin} " +
            "group by date_format(finish_time, '%Y-%m-%d') order by date")
    List<Map<String, Object>> statRunnerDailyTrend(@Param("runnerId") Long runnerId, @Param("begin") LocalDate begin);
}
