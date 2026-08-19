package com.campus.trade.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置。
 *
 * 只引入 redisson 核心库（非 spring-boot-starter），由本类显式装配：
 * 1. 复用与 Spring Data Redis 完全相同的连接参数（spring.redis.*），
 *    保证分布式锁与缓存落在同一个 Redis 实例/库，key 前缀策略一致；
 * 2. 避免 redisson-spring-boot-starter 自动配置接管 RedisConnectionFactory，
 *    与现有 Spring Cache 的序列化/连接管理解耦，互不干扰；
 * 3. 连接池按需收缩（最小 2、最大 8），锁场景并发度远低于数据访问。
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.database:0}") int database,
            @Value("${spring.redis.password:}") String password) {
        Config config = new Config();
        // watch dog 默认续期 30s，锁业务必须在 watchdog 周期内完成或显式设置 leaseTime
        config.setLockWatchdogTimeout(30_000L);
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(password == null || password.isBlank() ? null : password)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8)
                .setConnectTimeout(3000)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}
