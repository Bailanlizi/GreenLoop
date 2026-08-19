package com.campus.trade.config;

import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 带"随机 TTL 抖动"与"空值短 TTL"的 RedisCacheWriter 装饰器。
 *
 * 解决的问题：
 * 1. 缓存雪崩——大量 key 使用相同 TTL，同一时刻批量过期后集中回源打爆 DB。
 *    这里把每次写入的 TTL 在 [base×(1-jitter), base×(1+jitter)] 内随机化
 *    （jitter=20%），使过期时刻在时间轴上均匀散开；
 * 2. 空值穿透——Spring Cache 的 RedisCache 在允许 null 时，用内置的
 *    BINARY_NULL_VALUE（NullValue.INSTANCE 的 JDK 序列化字节）落盘空值。
 *    这里识别该字节序列并把空值 TTL 缩短为 base 的 1/5（下限 30s），
 *    防止"暂无数据"的占位被长时间缓存，导致真实数据出现后客户端读不到。
 *
 * 仅影响写入路径（put / putIfAbsent），读取与删除原样委托。
 */
public class RandomJitterRedisCacheWriter implements RedisCacheWriter {

    /** TTL 抖动幅度：±20%。 */
    private static final double JITTER_RATIO = 0.2;
    /**
     * Spring RedisCache 落盘空值的内置字节序列（BINARY_NULL_VALUE）。
     * 运行时动态生成（而非硬编码魔数），与当前 spring-core 版本保持严格一致。
     */
    private static final byte[] BINARY_NULL_VALUE =
            new JdkSerializationRedisSerializer().serialize(NullValue.INSTANCE);
    /** 空值 TTL = base TTL 的 1/5，下限 30 秒。 */
    private static final double NULL_VALUE_TTL_RATIO = 0.2;
    private static final Duration NULL_VALUE_TTL_FLOOR = Duration.ofSeconds(30);

    private final RedisCacheWriter delegate;

    public RandomJitterRedisCacheWriter(RedisCacheWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name, key, value, effectiveTtl(value, ttl));
    }

    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name, key, value, effectiveTtl(value, ttl));
    }

    @Override
    public byte[] get(String name, byte[] key) {
        return delegate.get(name, key);
    }

    @Override
    public void remove(String name, byte[] key) {
        delegate.remove(name, key);
    }

    @Override
    public void clean(String name, byte[] pattern) {
        delegate.clean(name, pattern);
    }

    @Override
    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector collector) {
        return new RandomJitterRedisCacheWriter(delegate.withStatisticsCollector(collector));
    }

    @Override
    public void clearStatistics(String name) {
        delegate.clearStatistics(name);
    }

    @Override
    public CacheStatistics getCacheStatistics(String cacheName) {
        return delegate.getCacheStatistics(cacheName);
    }

    private Duration effectiveTtl(byte[] value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return ttl;
        }
        Duration base = Arrays.equals(value, BINARY_NULL_VALUE)
                ? shortened(ttl)
                : ttl;
        return jitter(base);
    }

    /** 空值 TTL 缩短为 base 的 1/5，且不低于 30 秒。 */
    private Duration shortened(Duration ttl) {
        Duration shortened = Duration.ofSeconds(Math.max(1, (long) (ttl.getSeconds() * NULL_VALUE_TTL_RATIO)));
        return shortened.compareTo(NULL_VALUE_TTL_FLOOR) < 0 ? NULL_VALUE_TTL_FLOOR : shortened;
    }

    /** 在 [base×(1-jitter), base×(1+jitter)] 内随机化，结果至少 1 秒。 */
    private Duration jitter(Duration base) {
        long baseSeconds = base.getSeconds();
        double factor = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * JITTER_RATIO;
        long target = Math.max(1, Math.round(baseSeconds * factor));
        return Duration.ofSeconds(target);
    }
}
