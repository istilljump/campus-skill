package com.campus.runner.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // key采用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value采用Jackson序列化
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();

        // 注册Java8时间模块
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用默认的类型信息序列化，支持多态反序列化
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 禁用写入日期为timestamps，改为ISO-8601字符串日期格式
        om.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

        jacksonSerializer.setObjectMapper(om);

        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        //缓存值序列化器：Jackson + JavaTimeModule（实体含 LocalDateTime）+ 类型信息（多态反序列化）
        ObjectMapper cacheMapper = new ObjectMapper();
        cacheMapper.registerModule(new JavaTimeModule());
        cacheMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        cacheMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        cacheMapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        //热点读缓存分级 TTL：类目/榜单变化低频可长缓存，货架与详情需要较高新鲜度
        java.time.Duration categoryTtl = java.time.Duration.ofMinutes(10);
        java.time.Duration rankTtl = java.time.Duration.ofMinutes(5);
        java.time.Duration shelfTtl = java.time.Duration.ofMinutes(2);
        java.time.Duration detailTtl = java.time.Duration.ofMinutes(5);

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withCacheConfiguration(com.campus.runner.constant.CacheNames.SKILL_CATEGORY, defaults.entryTtl(categoryTtl))
                .withCacheConfiguration(com.campus.runner.constant.CacheNames.CREDIT_RANK, defaults.entryTtl(rankTtl))
                .withCacheConfiguration(com.campus.runner.constant.CacheNames.MARKET_SHELF, defaults.entryTtl(shelfTtl))
                .withCacheConfiguration(com.campus.runner.constant.CacheNames.SERVICE_DETAIL, defaults.entryTtl(detailTtl))
                .build();
    }
}
