package io.github.charlie237.taiyi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.charlie237.taiyi.common.Constants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Constants.Cache.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS)
                .recordStats());
        
        // 设置缓存名称
        cacheManager.setCacheNames(java.util.List.of(
                Constants.Cache.USER_CACHE,
                Constants.Cache.NODE_CACHE,
                Constants.Cache.ROUTE_CACHE
        ));
        
        return cacheManager;
    }
}
