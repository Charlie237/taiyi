package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.ApiToken;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.repository.ApiTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * API Token服务类
 * 商业化Token认证管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTokenService {
    
    private final ApiTokenRepository apiTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 创建新的API Token
     */
    @Transactional
    public ApiToken createToken(User user, String tokenName, ApiToken.Plan plan) {
        // 检查用户Token数量限制
        long userTokenCount = apiTokenRepository.countByUser(user);
        if (userTokenCount >= 10) { // 每个用户最多10个Token
            throw new RuntimeException("Token数量已达上限");
        }
        
        ApiToken token = new ApiToken();
        token.setUser(user);
        token.setTokenName(tokenName);
        token.setToken(generateToken());
        token.setTokenSecret(generateSecret());
        token.setPlan(plan);
        token.setStatus(ApiToken.Status.ACTIVE);
        
        // 根据套餐设置限制
        setPlanLimits(token, plan);
        
        ApiToken savedToken = apiTokenRepository.save(token);
        log.info("创建API Token成功: {} - {}", user.getUsername(), tokenName);
        return savedToken;
    }
    
    /**
     * 验证Token
     */
    public Optional<ApiToken> validateToken(String tokenValue) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findByTokenAndStatus(
                tokenValue, ApiToken.Status.ACTIVE);
        
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            
            // 检查是否过期
            if (!token.isValid()) {
                token.setStatus(ApiToken.Status.EXPIRED);
                apiTokenRepository.save(token);
                return Optional.empty();
            }
            
            // 检查是否需要重置流量
            if (token.needsTrafficReset()) {
                token.resetMonthlyTraffic();
                apiTokenRepository.save(token);
            }
            
            // 更新最后使用时间
            token.setLastUsedAt(LocalDateTime.now());
            apiTokenRepository.save(token);
            
            return Optional.of(token);
        }
        
        return Optional.empty();
    }
    
    /**
     * 根据ID查找Token
     */
    public Optional<ApiToken> findById(Long id) {
        return apiTokenRepository.findById(id);
    }
    
    /**
     * 获取用户的所有Token
     */
    public List<ApiToken> getUserTokens(User user) {
        return apiTokenRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * 获取所有Token（分页）
     */
    public Page<ApiToken> getTokens(Pageable pageable) {
        return apiTokenRepository.findAll(pageable);
    }
    
    /**
     * 删除Token
     */
    @Transactional
    public boolean deleteToken(Long tokenId, User user) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            if (token.getUser().getId().equals(user.getId())) {
                apiTokenRepository.delete(token);
                log.info("删除API Token: {} - {}", user.getUsername(), token.getTokenName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * 更新Token套餐
     */
    @Transactional
    public boolean updateTokenPlan(Long tokenId, ApiToken.Plan newPlan) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            token.setPlan(newPlan);
            setPlanLimits(token, newPlan);
            apiTokenRepository.save(token);
            log.info("更新Token套餐: {} -> {}", token.getTokenName(), newPlan);
            return true;
        }
        return false;
    }
    
    /**
     * 暂停Token
     */
    @Transactional
    public boolean suspendToken(Long tokenId) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            token.setStatus(ApiToken.Status.SUSPENDED);
            apiTokenRepository.save(token);
            log.info("暂停Token: {}", token.getTokenName());
            return true;
        }
        return false;
    }
    
    /**
     * 激活Token
     */
    @Transactional
    public boolean activateToken(Long tokenId) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            token.setStatus(ApiToken.Status.ACTIVE);
            apiTokenRepository.save(token);
            log.info("激活Token: {}", token.getTokenName());
            return true;
        }
        return false;
    }

    /**
     * 撤销Token
     */
    @Transactional
    public boolean revokeToken(Long tokenId) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findById(tokenId);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            token.setStatus(ApiToken.Status.REVOKED);
            apiTokenRepository.save(token);
            log.info("撤销Token: {}", token.getTokenName());
            return true;
        }
        return false;
    }
    
    /**
     * 清理过期Token
     */
    @Transactional
    public void cleanupExpiredTokens() {
        List<ApiToken> expiredTokens = apiTokenRepository.findExpiredTokens(LocalDateTime.now());
        for (ApiToken token : expiredTokens) {
            token.setStatus(ApiToken.Status.EXPIRED);
        }
        apiTokenRepository.saveAll(expiredTokens);
        log.info("清理过期Token数量: {}", expiredTokens.size());
    }
    
    /**
     * 重置所有Token的月流量
     */
    @Transactional
    public void resetAllMonthlyTraffic() {
        List<ApiToken> tokensNeedReset = apiTokenRepository.findTokensNeedTrafficReset(LocalDateTime.now());
        for (ApiToken token : tokensNeedReset) {
            token.resetMonthlyTraffic();
        }
        apiTokenRepository.saveAll(tokensNeedReset);
        log.info("重置月流量Token数量: {}", tokensNeedReset.size());
    }
    
    /**
     * 统计活跃Token数量
     */
    public long countActiveTokens() {
        return apiTokenRepository.countByStatus(ApiToken.Status.ACTIVE);
    }
    
    /**
     * 记录Token流量使用
     */
    @Transactional
    public void recordTrafficUsage(String tokenValue, long bytes) {
        Optional<ApiToken> tokenOpt = apiTokenRepository.findByToken(tokenValue);
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            token.addTrafficUsage(bytes);
            apiTokenRepository.save(token);
        }
    }
    
    /**
     * 生成Token值
     */
    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return "taiyi_" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * 生成Token密钥
     */
    private String generateSecret() {
        byte[] secretBytes = new byte[24];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }
    
    /**
     * 根据套餐设置限制
     */
    private void setPlanLimits(ApiToken token, ApiToken.Plan plan) {
        token.setMaxTunnels(plan.getMaxTunnels());
        token.setMaxBandwidth(plan.getMaxBandwidth());
        token.setMaxTrafficMonthly(plan.getMaxTrafficMonthly());
        token.setMaxConnections(plan.getMaxConnections());
    }
}
