package io.github.charlie237.taiyi.security;

import io.github.charlie237.taiyi.entity.ApiToken;
import io.github.charlie237.taiyi.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * API Token认证过滤器
 * 支持商业化Token认证，类似V2Board的订阅认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private final ApiTokenService apiTokenService;
    
    private static final String TOKEN_HEADER = "X-API-Token";
    private static final String TOKEN_PARAM = "token";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractToken(request);
            
            if (StringUtils.hasText(token)) {
                Optional<ApiToken> apiTokenOpt = apiTokenService.validateToken(token);
                
                if (apiTokenOpt.isPresent()) {
                    ApiToken apiToken = apiTokenOpt.get();
                    
                    // 检查Token是否有效
                    if (apiToken.isValid()) {
                        // 创建认证对象
                        ApiTokenAuthentication authentication = new ApiTokenAuthentication(apiToken);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("API Token认证成功: {}", apiToken.getTokenName());
                    } else {
                        log.warn("API Token无效或已过期: {}", token);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("API Token认证失败", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 从Header中获取
        String headerToken = request.getHeader(TOKEN_HEADER);
        if (StringUtils.hasText(headerToken)) {
            if (headerToken.startsWith(BEARER_PREFIX)) {
                return headerToken.substring(BEARER_PREFIX.length());
            }
            return headerToken;
        }
        
        // 2. 从Authorization Header中获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            // 检查是否是API Token格式
            if (token.startsWith("taiyi_")) {
                return token;
            }
        }
        
        // 3. 从查询参数中获取
        String paramToken = request.getParameter(TOKEN_PARAM);
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        
        return null;
    }
    
    /**
     * API Token认证对象
     */
    public static class ApiTokenAuthentication extends UsernamePasswordAuthenticationToken {
        
        private final ApiToken apiToken;
        
        public ApiTokenAuthentication(ApiToken apiToken) {
            super(
                apiToken.getUser().getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"))
            );
            this.apiToken = apiToken;
            setAuthenticated(true);
        }
        
        public ApiToken getApiToken() {
            return apiToken;
        }
        
        /**
         * 检查权限
         */
        public boolean hasPermission(String permission, Object... params) {
            switch (permission) {
                case "CREATE_TUNNEL":
                    int currentTunnels = (Integer) params[0];
                    return apiToken.canCreateMoreTunnels(currentTunnels);
                    
                case "USE_BANDWIDTH":
                    long requestedBandwidth = (Long) params[0];
                    return requestedBandwidth <= apiToken.getMaxBandwidth();
                    
                case "USE_TRAFFIC":
                    return !apiToken.isTrafficExceeded();
                    
                default:
                    return false;
            }
        }
        
        /**
         * 获取剩余隧道数量
         */
        public int getRemainingTunnels(int currentTunnels) {
            return Math.max(0, apiToken.getMaxTunnels() - currentTunnels);
        }
        
        /**
         * 获取剩余流量
         */
        public long getRemainingTraffic() {
            return apiToken.getRemainingTraffic();
        }
        
        /**
         * 记录流量使用
         */
        public void recordTrafficUsage(long bytes) {
            apiToken.addTrafficUsage(bytes);
        }
    }
}
