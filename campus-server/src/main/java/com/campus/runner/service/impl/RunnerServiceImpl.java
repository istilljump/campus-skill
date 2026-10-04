package com.campus.runner.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.runner.constant.JwtClaimsConstant;
import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.RunnerConstant;
import com.campus.runner.constant.WeChatConstant;
import com.campus.runner.dto.RunnerAuditApplyDTO;
import com.campus.runner.entity.Runner;
import com.campus.runner.entity.RunnerAudit;
import com.campus.runner.entity.User;
import com.campus.runner.exception.AuditNotPassedException;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.exception.LoginFailedException;
import com.campus.runner.mapper.OrderMapper;
import com.campus.runner.mapper.RunnerAuditMapper;
import com.campus.runner.mapper.RunnerMapper;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.properties.JwtProperties;
import com.campus.runner.properties.WeChatProperties;
import com.campus.runner.service.RunnerService;
import com.campus.runner.result.PageResult;
import com.campus.runner.service.WalletService;
import com.campus.runner.utils.HttpClientUtil;
import com.campus.runner.utils.JwtUtil;
import com.campus.runner.vo.RunnerCenterVO;
import com.campus.runner.vo.RunnerVO;
import com.campus.runner.vo.UserLoginVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RunnerServiceImpl implements RunnerService {

    //等级晋升门槛：完成订单数
    private static final int LEVEL_BRONZE_MIN = 20;
    private static final int LEVEL_SILVER_MIN = 50;
    private static final int LEVEL_GOLD_MIN = 100;

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RunnerMapper runnerMapper;

    @Autowired
    private RunnerAuditMapper runnerAuditMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private com.campus.runner.mapper.OrderReviewMapper orderReviewMapper;

    @Autowired
    private WalletService walletService;

    @Override
    public UserLoginVO login(String code) {
        String openid = resolveOpenid(code);

        User user = userMapper.getUserByOpenid(openid);
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.saveUser(user);
        }

        Runner runner = runnerMapper.getByUserId(user.getId());
        if (runner == null || runner.getAuditStatus() == null || runner.getAuditStatus() != RunnerConstant.AUDIT_PASSED) {
            throw new LoginFailedException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        if (runner.getStatus() == null || runner.getStatus() != 1) {
            throw new LoginFailedException(MessageConstant.ACCOUNT_LOCKED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.RUNNER_ID, runner.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(user.getId())
                .openid(openid)
                .token(token)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAudit(Long userId, RunnerAuditApplyDTO dto) {
        Runner runner = runnerMapper.getByUserId(userId);
        if (runner == null) {
            //首次申请：创建技能者档案并初始化钱包
            runner = Runner.builder()
                    .userId(userId)
                    .name(dto.getRealName())
                    .studentNo(dto.getStudentNo())
                    .campus(dto.getCampus())
                    .college(dto.getCollege())
                    .auditStatus(RunnerConstant.AUDIT_REVIEWING)
                    .runnerLevel(RunnerConstant.LEVEL_NORMAL)
                    .dailyOrderLimit(RunnerConstant.DEFAULT_DAILY_LIMIT)
                    .completedOrders(0)
                    .score(new BigDecimal("5.0"))
                    .status(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            runnerMapper.insert(runner);
            walletService.getWallet(userId);
        } else if (runner.getAuditStatus() != null && runner.getAuditStatus() == RunnerConstant.AUDIT_REVIEWING) {
            throw new BusinessException(MessageConstant.RUNNER_AUDIT_REVIEWING);
        }

        RunnerAudit audit = RunnerAudit.builder()
                .runnerId(runner.getId())
                .realName(dto.getRealName())
                .studentNo(dto.getStudentNo())
                .campus(dto.getCampus())
                .college(dto.getCollege())
                .idCard(dto.getIdCard())
                .studentCardImg(dto.getStudentCardImg())
                .status(RunnerAudit.PENDING)
                .applyTime(LocalDateTime.now())
                .build();
        runnerAuditMapper.insert(audit);

        Runner upd = Runner.builder().id(runner.getId()).auditStatus(RunnerConstant.AUDIT_REVIEWING).build();
        runnerMapper.update(upd);
        log.info("技能者认证申请已提交，userId={}, runnerId={}", userId, runner.getId());
    }

    @Override
    public RunnerCenterVO getCenter(Long userId) {
        Runner runner = runnerMapper.getByUserId(userId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        return RunnerCenterVO.builder()
                .id(runner.getId())
                .name(runner.getName())
                .auditStatus(runner.getAuditStatus())
                .runnerLevel(runner.getRunnerLevel())
                .dailyOrderLimit(runner.getDailyOrderLimit())
                .todayOrderCount(orderMapper.countRunnerTodayOrders(runner.getId()))
                .todayIncome(orderMapper.sumRunnerTodayIncome(runner.getId()))
                .completedOrders(runner.getCompletedOrders())
                .score(runner.getScore())
                .creditScore(runner.getCreditScore())
                .skillLevel(runner.getSkillLevel())
                .balance(walletService.getWallet(userId).getBalance())
                .build();
    }

    @Override
    public List<com.campus.runner.vo.TrendPointVO> dailyTrend(Long userId, int days) {
        Runner runner = runnerMapper.getByUserId(userId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        days = Math.max(1, Math.min(days, 30));
        java.time.LocalDate begin = java.time.LocalDate.now().minusDays(days - 1L);
        java.util.Map<String, Map<String, Object>> byDate = new java.util.HashMap<>();
        for (Map<String, Object> row : orderMapper.statRunnerDailyTrend(runner.getId(), begin)) {
            byDate.put(String.valueOf(row.get("date")), row);
        }
        List<com.campus.runner.vo.TrendPointVO> result = new java.util.ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = begin.plusDays(i).toString();
            Map<String, Object> row = byDate.get(date);
            result.add(com.campus.runner.vo.TrendPointVO.builder()
                    .date(date)
                    .orderCount(row != null ? ((Number) row.get("orderCount")).intValue() : 0)
                    .completedCount(row != null ? ((Number) row.get("orderCount")).intValue() : 0)
                    .amount(row != null && row.get("amount") != null
                            ? new BigDecimal(String.valueOf(row.get("amount"))) : BigDecimal.ZERO)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(Long runnerId) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null || runner.getCompletedOrders() == null) {
            return;
        }
        int completed = runner.getCompletedOrders();
        int level;
        if (completed >= LEVEL_GOLD_MIN) {
            level = RunnerConstant.LEVEL_GOLD;
        } else if (completed >= LEVEL_SILVER_MIN) {
            level = RunnerConstant.LEVEL_SILVER;
        } else if (completed >= LEVEL_BRONZE_MIN) {
            level = RunnerConstant.LEVEL_BRONZE;
        } else {
            level = RunnerConstant.LEVEL_NORMAL;
        }
        if (level != runner.getRunnerLevel()) {
            Runner upd = Runner.builder().id(runnerId).runnerLevel(level).build();
            runnerMapper.update(upd);
            log.info("技能者等级更新，runnerId={}, level={}", runnerId, level);
        }
    }

    @Override
    public void updateScore(Long runnerId) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            return;
        }
        BigDecimal avg = orderReviewMapper.avgScoreByRunnerId(runnerId);
        runnerMapper.updateScore(runnerId, avg);
    }

    @Override
    public void checkGrabPermission(Long runnerId) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        if (runner.getAuditStatus() == null || runner.getAuditStatus() != RunnerConstant.AUDIT_PASSED) {
            throw new AuditNotPassedException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        if (runner.getStatus() == null || runner.getStatus() != 1) {
            throw new BusinessException(MessageConstant.ACCOUNT_LOCKED);
        }
        int todayCount = orderMapper.countRunnerTodayOrders(runnerId);
        if (runner.getDailyOrderLimit() != null && todayCount >= runner.getDailyOrderLimit()) {
            throw new BusinessException(MessageConstant.DAILY_LIMIT_REACHED);
        }
    }

    @Override
    public PageResult<RunnerVO> page(Integer page, Integer pageSize, String name, String campus, Integer auditStatus, Integer status) {
        PageHelper.startPage(page, pageSize);
        Page<Runner> runnerPage = (Page<Runner>) runnerMapper.page(name, campus, auditStatus, status);
        List<RunnerVO> vos = runnerPage.getResult().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(runnerPage.getTotal(), vos, runnerPage.getPageSize(), runnerPage.getPageNum());
    }

    @Override
    public void updateStatus(Long runnerId, Integer status) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        Runner upd = Runner.builder().id(runnerId).status(status).build();
        runnerMapper.update(upd);
        log.info("技能者状态更新，runnerId={}, status={}", runnerId, status);
    }

    @Override
    public Long resolveUserId(Long runnerId) {
        Runner runner = runnerMapper.getById(runnerId);
        if (runner == null) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_FOUND);
        }
        return runner.getUserId();
    }

    private RunnerVO toVO(Runner runner) {
        return RunnerVO.builder()
                .id(runner.getId())
                .userId(runner.getUserId())
                .name(runner.getName())
                .phone(runner.getPhone())
                .studentNo(runner.getStudentNo())
                .campus(runner.getCampus())
                .college(runner.getCollege())
                .auditStatus(runner.getAuditStatus())
                .runnerLevel(runner.getRunnerLevel())
                .dailyOrderLimit(runner.getDailyOrderLimit())
                .completedOrders(runner.getCompletedOrders())
                .score(runner.getScore())
                .status(runner.getStatus())
                .createTime(runner.getCreateTime())
                .build();
    }

    /**
     * 通过微信 code 换取 openid；本地开发未配置微信 appid 时使用模拟 openid，便于联调
     */
    private String resolveOpenid(String code) {
        String appid = weChatProperties.getAppid();
        if (appid == null || appid.isBlank() || "placeholder".equals(appid)) {
            log.warn("未配置微信appid，使用本地模拟openid联调");
            return "campus_dev_" + code;
        }
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(WeChatConstant.PARAM_APPID, appid);
        queryParams.put(WeChatConstant.PARAM_SECRET, weChatProperties.getSecret());
        queryParams.put(WeChatConstant.PARAM_JS_CODE, code);
        queryParams.put(WeChatConstant.PARAM_GRANT_TYPE, WeChatConstant.GRANT_TYPE_AUTHORIZATION_CODE);
        String response = HttpClientUtil.doGet(WeChatConstant.WECHAT_SERVER_LOGIN_URL, queryParams);

        JSONObject jsonObject = JSON.parseObject(response);
        String openid = jsonObject.getString("openid");
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        return openid;
    }
}
