package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.NodeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节点状态数据访问接口
 */
@Repository
public interface NodeStatusRepository extends JpaRepository<NodeStatus, Long> {
    
    /**
     * 根据节点查找状态记录
     */
    List<NodeStatus> findByNode(Node node);
    
    /**
     * 根据节点ID查找状态记录
     */
    List<NodeStatus> findByNodeId(Long nodeId);
    
    /**
     * 查找节点的最新状态记录
     */
    Optional<NodeStatus> findTopByNodeOrderByRecordedAtDesc(Node node);

    /**
     * 根据节点查找最新状态（用于NodeStatusService）
     */
    @Query("SELECT ns FROM NodeStatus ns WHERE ns.node = :node ORDER BY ns.recordedAt DESC LIMIT 1")
    Optional<NodeStatus> findLatestByNode(@Param("node") Node node);

    /**
     * 根据节点查找状态历史（按时间倒序，分页）
     */
    Page<NodeStatus> findByNodeOrderByRecordedAtDesc(Node node, Pageable pageable);
    
    /**
     * 查找指定时间范围内的状态记录
     */
    List<NodeStatus> findByNodeAndRecordedAtBetween(Node node, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找指定时间后的状态记录
     */
    List<NodeStatus> findByRecordedAtAfter(LocalDateTime dateTime);
    
    /**
     * 查找指定时间前的状态记录
     */
    List<NodeStatus> findByRecordedAtBefore(LocalDateTime dateTime);
    
    /**
     * 根据CPU使用率查找状态记录
     */
    @Query("SELECT ns FROM NodeStatus ns WHERE ns.cpuUsage > :threshold")
    List<NodeStatus> findByCpuUsageGreaterThan(@Param("threshold") Double threshold);
    
    /**
     * 根据内存使用率查找状态记录
     */
    @Query("SELECT ns FROM NodeStatus ns WHERE ns.memoryUsage > :threshold")
    List<NodeStatus> findByMemoryUsageGreaterThan(@Param("threshold") Double threshold);
    
    /**
     * 查找有错误的状态记录
     */
    @Query("SELECT ns FROM NodeStatus ns WHERE ns.errorCount > 0")
    List<NodeStatus> findWithErrors();
    
    /**
     * 查找有警告的状态记录
     */
    @Query("SELECT ns FROM NodeStatus ns WHERE ns.warningCount > 0")
    List<NodeStatus> findWithWarnings();
    
    /**
     * 统计节点的状态记录数量
     */
    long countByNode(Node node);
    
    /**
     * 删除指定时间前的状态记录
     */
    @Modifying
    @Query("DELETE FROM NodeStatus ns WHERE ns.recordedAt < :cutoffTime")
    int deleteByRecordedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 查找节点的平均CPU使用率
     */
    @Query("SELECT AVG(ns.cpuUsage) FROM NodeStatus ns WHERE ns.node = :node AND ns.recordedAt BETWEEN :startTime AND :endTime")
    Double findAverageCpuUsage(@Param("node") Node node, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找节点的平均内存使用率
     */
    @Query("SELECT AVG(ns.memoryUsage) FROM NodeStatus ns WHERE ns.node = :node AND ns.recordedAt BETWEEN :startTime AND :endTime")
    Double findAverageMemoryUsage(@Param("node") Node node, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
