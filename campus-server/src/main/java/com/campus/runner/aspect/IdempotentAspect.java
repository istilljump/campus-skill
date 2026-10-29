package com.campus.runner.aspect;

import com.campus.runner.annotation.Idempotent;
import com.campus.runner.constant.RedisConstant;
import com.campus.runner.context.BaseContext;
import com.campus.runner.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * 幂等切面：拦截标注 {@link Idempotent} 的资金类接口，
 * 以 Redis SETNX 实现「同键仅成功一次」的提交去重（键结构见 {@link RedisConstant#IDEMPOTENT_KEY}）。
 *
 * <p>与抢单的 Redis 锁同源不同用：抢单锁防"多 winners"，幂等键防"同一笔提交被处理多次"。
 * 首笔业务异常时主动释放键，保证失败后可合法重试；成功后键保留 10 分钟拦截重放。
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    public static final String HEADER_KEY = "X-Idempotency-Key";

    //首笔处理中键值，TTL 为等待上限；成功后改写为 DONE 并延长，供后续重放识别
    private static final String VALUE_PROCESSING = "P";
    private static final String VALUE_DONE = "D";
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);
    private static final Duration DONE_TTL = Duration.ofMinutes(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        Long userId = BaseContext.getCurrentId();
        String token = resolveToken();
        //未登录上下文或未携带幂等键：直接放行（兼容旧客户端与内部调用）
        if (userId == null || token == null || token.isBlank()) {
            return pjp.proceed();
        }
        String key = RedisConstant.IDEMPOTENT_KEY + idempotent.scope() + ":" + userId + ":" + token;
        Boolean claimed = stringRedisTemplate.opsForValue().setIfAbsent(key, VALUE_PROCESSING, PROCESSING_TTL);
        if (!Boolean.TRUE.equals(claimed)) {
            //占位失败：要么首笔处理中，要么首笔已成功
            String existing = stringRedisTemplate.opsForValue().get(key);
            boolean done = VALUE_DONE.equals(existing);
            log.info("[幂等拦截]scope={}, userId={}, token={}, 首笔状态={}",
                    idempotent.scope(), userId, token, done ? "已完成" : "处理中");
            throw new BusinessException(done ? idempotent.duplicateMessage() : idempotent.processingMessage());
        }
        try {
            Object result = pjp.proceed();
            //成功：标记完成并延长保留期，后续同键重放全部拦截
            stringRedisTemplate.opsForValue().set(key, VALUE_DONE, DONE_TTL);
            return result;
        } catch (Throwable e) {
            //业务失败：释放键，允许用户修正后携带同键重试
            stringRedisTemplate.delete(key);
            throw e;
        }
    }

    /**
     * 幂等键取自当前 HTTP 请求头
     */
    private String resolveToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(HEADER_KEY);
    }
}
