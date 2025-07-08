package io.github.charlie237.taiyi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置
 */
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 创建API限流桶
     */
    public Bucket createApiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * 创建登录限流桶
     */
    public Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * 创建WebSocket连接限流桶
     */
    public Bucket createWebSocketBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * 获取或创建用户限流桶
     */
    public Bucket resolveBucket(String key, BucketType type) {
        return buckets.computeIfAbsent(key, k -> {
            switch (type) {
                case API:
                    return createApiBucket();
                case LOGIN:
                    return createLoginBucket();
                case WEBSOCKET:
                    return createWebSocketBucket();
                default:
                    return createApiBucket();
            }
        });
    }
    
    /**
     * 限流桶类型
     */
    public enum BucketType {
        API,
        LOGIN,
        WEBSOCKET
    }
}
