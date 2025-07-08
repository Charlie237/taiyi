package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志数据访问接口
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * 根据用户ID查找审计日志
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据操作类型查找审计日志
     */
    Page<AuditLog> findByOperationType(AuditLog.OperationType operationType, Pageable pageable);
    
    /**
     * 根据时间范围查找审计日志
     */
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据IP地址查找审计日志
     */
    List<AuditLog> findByIpAddress(String ipAddress);
    
    /**
     * 根据资源类型查找审计日志
     */
    List<AuditLog> findByResourceType(String resourceType);
    
    /**
     * 删除指定时间前的审计日志
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :beforeDate")
    long deleteByCreatedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 统计操作类型数量
     */
    @Query("SELECT a.operationType, COUNT(a) FROM AuditLog a GROUP BY a.operationType")
    List<Object[]> countByOperationType();
    
    /**
     * 统计每日操作数量
     */
    @Query("SELECT DATE(a.createdAt), COUNT(a) FROM AuditLog a WHERE a.createdAt >= :startDate GROUP BY DATE(a.createdAt) ORDER BY DATE(a.createdAt)")
    List<Object[]> countDailyOperations(@Param("startDate") LocalDateTime startDate);
}
