package com.campus.runner.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存开关：campus.cache.enabled=false 时完全关闭 Spring Cache（A/B 压测对比用），
 * 默认开启。@EnableCaching 从主类迁移至此，保证开关能真正生效。
 */
@Configuration
@ConditionalOnProperty(prefix = "campus.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableCaching
public class CacheToggleConfig {
}
