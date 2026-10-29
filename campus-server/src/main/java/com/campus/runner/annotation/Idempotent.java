package com.campus.runner.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等提交保护：客户端为每次"用户动作"生成唯一键（UUID），随请求头 {@code X-Idempotency-Key} 携带。
 *
 * <p>语义（Stripe 风格幂等键）：
 * <ul>
 *   <li>同键首次请求：正常执行，成功后将键标记为已完成（保留 10 分钟）；</li>
 *   <li>同键并发请求：若首笔仍在处理，返回"请求正在处理中"；若首笔已成功，返回"请勿重复提交"；</li>
 *   <li>同键重试（首笔业务失败）：键自动释放，重试放行——只防重复提交，不防合法重试；</li>
 *   <li>未携带键：直接放行（兼容旧客户端），不启用幂等。</li>
 * </ul>
 * 典型防护场景：支付按钮双击、网络超时重发、刷新重放，防止重复扣款/重复冻结。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 业务域命名空间（同一键在不同业务域互不冲突），如 order-pay / wallet-recharge / wallet-withdraw
     */
    String scope();

    /**
     * 重复提交（首笔已成功）提示
     */
    String duplicateMessage() default "请勿重复提交";

    /**
     * 并发提交（首笔处理中）提示
     */
    String processingMessage() default "请求正在处理中，请稍候";
}
