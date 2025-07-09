package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.ApiToken;
import io.github.charlie237.taiyi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * API Token数据访问层
 */
@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    
    /**
     * 根据Token值查找
     */
    Optional<ApiToken> findByToken(String token);
    
    /**
     * 根据Token值和状态查找
     */
    Optional<ApiToken> findByTokenAndStatus(String token, ApiToken.Status status);
    
    /**
     * 根据用户查找所有Token
     */
    List<ApiToken> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 根据用户和状态查找Token
     */
    List<ApiToken> findByUserAndStatus(User user, ApiToken.Status status);
    
    /**
     * 统计指定状态的Token数量
     */
    long countByStatus(ApiToken.Status status);
    
    /**
     * 统计用户的Token数量
     */
    long countByUser(User user);
    
    /**
     * 查找过期的Token
     */
    @Query("SELECT t FROM ApiToken t WHERE t.status = 'ACTIVE' AND t.expiresAt < :now")
    List<ApiToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 查找需要重置流量的Token
     */
    @Query("SELECT t FROM ApiToken t WHERE t.status = 'ACTIVE' AND t.trafficResetAt < :now")
    List<ApiToken> findTokensNeedTrafficReset(@Param("now") LocalDateTime now);
    
    /**
     * 查找流量超限的Token
     */
    @Query("SELECT t FROM ApiToken t WHERE t.status = 'ACTIVE' AND t.trafficUsed >= t.maxTrafficMonthly")
    List<ApiToken> findTrafficExceededTokens();
    
    /**
     * 根据套餐查找Token
     */
    List<ApiToken> findByPlan(ApiToken.Plan plan);
    
    /**
     * 查找最近使用的Token
     */
    @Query("SELECT t FROM ApiToken t WHERE t.status = 'ACTIVE' AND t.lastUsedAt > :since ORDER BY t.lastUsedAt DESC")
    List<ApiToken> findRecentlyUsedTokens(@Param("since") LocalDateTime since);
}
