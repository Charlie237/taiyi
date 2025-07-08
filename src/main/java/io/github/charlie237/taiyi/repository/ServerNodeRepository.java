package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.ServerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 服务器节点数据访问接口
 */
@Repository
public interface ServerNodeRepository extends JpaRepository<ServerNode, Long> {
    
    /**
     * 根据服务器ID查找节点
     */
    Optional<ServerNode> findByServerId(String serverId);
    
    /**
     * 检查公网IP是否存在
     */
    boolean existsByPublicIp(String publicIp);
    
    /**
     * 根据状态查找服务器节点
     */
    List<ServerNode> findByStatus(ServerNode.Status status);
    
    /**
     * 根据地区查找服务器节点
     */
    List<ServerNode> findByRegion(String region);
    
    /**
     * 根据节点类型查找服务器节点
     */
    List<ServerNode> findByNodeType(ServerNode.NodeType nodeType);
    
    /**
     * 根据地区和状态查找服务器节点，按负载评分排序
     */
    List<ServerNode> findByRegionAndStatusOrderByLoadScoreAsc(String region, ServerNode.Status status);
    
    /**
     * 根据状态查找服务器节点，按负载评分排序
     */
    List<ServerNode> findByStatusOrderByLoadScoreAsc(ServerNode.Status status);
    
    /**
     * 查找负载较低的服务器节点
     */
    @Query("SELECT s FROM ServerNode s WHERE s.status = 'ONLINE' AND s.loadScore < :maxLoadScore ORDER BY s.loadScore ASC")
    List<ServerNode> findLowLoadServerNodes(@Param("maxLoadScore") Double maxLoadScore);
    
    /**
     * 查找指定时间前有心跳的离线节点
     */
    @Query("SELECT s FROM ServerNode s WHERE s.status = 'OFFLINE' AND s.lastHeartbeat < :threshold")
    List<ServerNode> findOfflineNodesBeforeThreshold(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 统计各状态的服务器节点数量
     */
    long countByStatus(ServerNode.Status status);
    
    /**
     * 统计各地区的服务器节点数量
     */
    @Query("SELECT s.region, COUNT(s) FROM ServerNode s GROUP BY s.region")
    List<Object[]> countByRegion();
    
    /**
     * 查找高负载的服务器节点
     */
    @Query("SELECT s FROM ServerNode s WHERE s.status = 'ONLINE' AND s.loadScore > :threshold")
    List<ServerNode> findHighLoadServerNodes(@Param("threshold") Double threshold);
    
    /**
     * 根据ISP查找服务器节点
     */
    List<ServerNode> findByIsp(String isp);
    
    /**
     * 查找可用容量最大的服务器节点
     */
    @Query("SELECT s FROM ServerNode s WHERE s.status = 'ONLINE' ORDER BY (s.maxConnections - s.currentConnections) DESC")
    List<ServerNode> findByAvailableCapacityDesc();
}
