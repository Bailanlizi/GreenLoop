package com.campus.trade.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置。
 *
 * 取代 Spring Boot 自动配置的默认 RedisCacheManager，显式控制三件事：
 * 1. 统一 key 前缀 greenloop:{cache}:{key}，避免与其它项目共用同一个 db 时冲突；
 * 2. 按 cache 名分别设置 TTL，列表类短、字典类长，杜绝"永不过期靠重启清"的旧补丁；
 * 3. value 用 GenericJackson2JsonRedisSerializer（带 @class 类型信息），
 *    可读、可跨类型反序列化，且不再依赖 JDK 序列化。
 *
 * 不再定义自定义 RedisTemplate<String,Object>：业务侧只使用 Spring Boot
 * 自动配置的 StringRedisTemplate，多余的 bean 只会带来默认类型开启的风险。
 */
@Configuration
public class RedisConfig {

    public static final String CACHE_KEY_PREFIX = "greenloop:";

    /** 默认 TTL，未被单独配置的 cache 名兜底。 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .computePrefixWith(name -> CACHE_KEY_PREFIX + name + ":")
                .serializeKeysWith(SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        // 商品列表/详情：变动相对频繁，10 分钟
        perCache.put("products", base.entryTtl(Duration.ofMinutes(10)));
        perCache.put("product", base.entryTtl(Duration.ofMinutes(10)));
        // 推荐结果：计算成本高、可容忍一定延迟，30 分钟
        perCache.put("recommendations", base.entryTtl(Duration.ofMinutes(30)));
        // 订单详情：状态变更即 evict，15 分钟兜底防泄漏
        perCache.put("order", base.entryTtl(Duration.ofMinutes(15)));
        // 字典类：极少变更，2 小时
        perCache.put("meetup_locations", base.entryTtl(Duration.ofHours(2)));
        perCache.put("categories", base.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
