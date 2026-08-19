package com.campus.trade.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RandomJitterRedisCacheWriterTest {

    private static final byte[] NORMAL_VALUE = "{\"@class\":\"com.campus.trade.entity.Product\",\"id\":\"22\"}"
            .getBytes(StandardCharsets.UTF_8);
    /** 与 RedisCache 内置 BINARY_NULL_VALUE 完全一致的字节（同一 JVM 动态生成）。 */
    private static final byte[] BINARY_NULL_VALUE =
            new JdkSerializationRedisSerializer().serialize(NullValue.INSTANCE);

    private final RedisCacheWriter delegate = mock(RedisCacheWriter.class);
    private final RandomJitterRedisCacheWriter writer = new RandomJitterRedisCacheWriter(delegate);
    private final byte[] key = "k".getBytes(StandardCharsets.UTF_8);

    @Test
    void normalValueTtlStaysWithinJitterRange() {
        Duration base = Duration.ofMinutes(10); // 600s
        writer.put("cache", key, NORMAL_VALUE, base);

        org.mockito.ArgumentCaptor<Duration> captor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq("cache"), eq(key), eq(NORMAL_VALUE), captor.capture());
        Duration actual = captor.getValue();
        assertNotNull(actual);
        // ±20% 抖动：应在 [480s, 720s]
        assertTrue(actual.getSeconds() >= 480, "TTL 低于抖动下限: " + actual);
        assertTrue(actual.getSeconds() <= 720, "TTL 高于抖动上限: " + actual);
    }

    @Test
    void nullValueTtlIsShortenedToOneFifthWithFloor() {
        // base 10min(600s) → 空值 1/5 = 120s 附近（±20% 抖动 → [96s, 144s]）
        writer.putIfAbsent("cache", key, BINARY_NULL_VALUE, Duration.ofMinutes(10));

        org.mockito.ArgumentCaptor<Duration> captor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(delegate).putIfAbsent(eq("cache"), eq(key), eq(BINARY_NULL_VALUE), captor.capture());
        Duration actual = captor.getValue();
        assertNotNull(actual);
        assertTrue(actual.getSeconds() <= 150, "空值 TTL 未缩短: " + actual);
        assertTrue(actual.getSeconds() >= 30, "空值 TTL 低于下限: " + actual);
    }

    @Test
    void shortBaseTtlNullValueRespectsFloor() {
        // base 60s → 空值 1/5 = 12s < 30s 下限 → 应取 30s 附近（±20% → [24s, 36s]）
        writer.put("cache", key, BINARY_NULL_VALUE, Duration.ofSeconds(60));

        org.mockito.ArgumentCaptor<Duration> captor =
                org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(delegate).put(eq("cache"), eq(key), eq(BINARY_NULL_VALUE), captor.capture());
        Duration actual = captor.getValue();
        assertTrue(actual.getSeconds() >= 24 && actual.getSeconds() <= 36,
                "空值 TTL 未应用下限: " + actual);
    }

    @Test
    void nullTtlPassesThroughWithoutJitter() {
        writer.put("cache", key, NORMAL_VALUE, null);
        verify(delegate).put(eq("cache"), eq(key), eq(NORMAL_VALUE), eq(null));
    }

    @Test
    void getRemoveCleanDelegateThrough() {
        writer.get("cache", key);
        verify(delegate).get("cache", key);

        writer.remove("cache", key);
        verify(delegate).remove("cache", key);

        byte[] pattern = "*".getBytes(StandardCharsets.UTF_8);
        writer.clean("cache", pattern);
        verify(delegate).clean("cache", pattern);
    }
}
