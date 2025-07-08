package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节点数据访问接口
 */
@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    
    /**
     * 根据节点ID查找节点
     */
    Optional<Node> findByNodeId(String nodeId);
    
    /**
     * 根据用户查找节点
     */
    List<Node> findByUser(User user);
    
    /**
     * 根据用户ID查找节点
     */
    List<Node> findByUserId(Long userId);
    
    /**
     * 根据状态查找节点
     */
    List<Node> findByStatus(Node.Status status);
    
    /**
     * 根据协议查找节点
     */
    List<Node> findByProtocol(Node.Protocol protocol);
    
    /**
     * 查找在线节点
     */
    @Query("SELECT n FROM Node n WHERE n.status = 'ONLINE'")
    List<Node> findOnlineNodes();
    
    /**
     * 查找指定时间后有心跳的节点
     */
    List<Node> findByLastHeartbeatAfter(LocalDateTime dateTime);
    
    /**
     * 根据用户和状态查找节点
     */
    List<Node> findByUserAndStatus(User user, Node.Status status);
    
    /**
     * 根据名称模糊查询节点
     */
    @Query("SELECT n FROM Node n WHERE n.name LIKE %:keyword% OR n.description LIKE %:keyword%")
    List<Node> findByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计用户的节点数量
     */
    long countByUser(User user);
    
    /**
     * 统计指定状态的节点数量
     */
    long countByStatus(Node.Status status);
    
    /**
     * 查找需要清理的离线节点
     */
    @Query("SELECT n FROM Node n WHERE n.status = 'OFFLINE' AND n.lastHeartbeat < :threshold")
    List<Node> findOfflineNodesBeforeThreshold(@Param("threshold") LocalDateTime threshold);
}
