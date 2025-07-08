package io.github.charlie237.taiyi.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单的限流器实现
 */
@Slf4j
public class SimpleRateLimiter {
    
    private final int maxRequests;
    private final long windowSizeMs;
    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();
    
    public SimpleRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
    }
    
    /**
     * 尝试获取许可
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        RequestWindow window = windows.computeIfAbsent(key, k -> new RequestWindow());
        
        synchronized (window) {
            // 检查是否需要重置窗口
            if (now - window.windowStart >= windowSizeMs) {
                window.windowStart = now;
                window.requestCount.set(0);
            }
            
            // 检查是否超过限制
            if (window.requestCount.get() >= maxRequests) {
                return false;
            }
            
            // 增加请求计数
            window.requestCount.incrementAndGet();
            return true;
        }
    }
    
    /**
     * 获取剩余许可数
     */
    public int getRemainingPermits(String key) {
        RequestWindow window = windows.get(key);
        if (window == null) {
            return maxRequests;
        }
        
        long now = System.currentTimeMillis();
        synchronized (window) {
            // 检查是否需要重置窗口
            if (now - window.windowStart >= windowSizeMs) {
                return maxRequests;
            }
            
            return Math.max(0, maxRequests - window.requestCount.get());
        }
    }
    
    /**
     * 清理过期的窗口
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> {
            RequestWindow window = entry.getValue();
            return now - window.windowStart > windowSizeMs * 2;
        });
    }
    
    /**
     * 请求窗口
     */
    private static class RequestWindow {
        private volatile long windowStart = System.currentTimeMillis();
        private final AtomicInteger requestCount = new AtomicInteger(0);
    }
}
