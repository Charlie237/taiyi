package io.github.charlie237.taiyi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限流过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        
        // 确定限流类型
        RateLimitConfig.BucketType bucketType = determineBucketType(requestUri);

        // 获取限流桶
        Bucket bucket = rateLimitConfig.resolveBucket(clientIp, bucketType);

        // 尝试消费令牌
        if (bucket.tryConsume(1)) {
            // 令牌消费成功，继续处理请求
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            // 限流触发
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestUri);
            handleRateLimitExceeded(response);
        }
    }
    
    /**
     * 确定限流类型
     */
    private RateLimitConfig.BucketType determineBucketType(String requestUri) {
        if (requestUri.contains("/auth/login")) {
            return RateLimitConfig.BucketType.LOGIN;
        } else if (requestUri.contains("/ws/")) {
            return RateLimitConfig.BucketType.WEBSOCKET;
        } else {
            return RateLimitConfig.BucketType.API;
        }
    }
    
    /**
     * 处理限流超出
     */
    private void handleRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 设置等待时间为60秒
        long waitTimeSeconds = 60;

        ApiResponse<String> apiResponse = ApiResponse.error(429,
                String.format("请求过于频繁，请在 %d 秒后重试", waitTimeSeconds));

        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));
        response.addHeader("X-Rate-Limit-Remaining", "0");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 跳过静态资源和健康检查
        return path.startsWith("/actuator/health") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs");
    }
}
