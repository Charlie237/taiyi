package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 路由数据访问接口
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    /**
     * 根据节点查找路由
     */
    List<Route> findByNode(Node node);
    
    /**
     * 根据节点ID查找路由
     */
    List<Route> findByNodeId(Long nodeId);
    
    /**
     * 根据状态查找路由
     */
    List<Route> findByStatus(Route.Status status);
    
    /**
     * 根据协议查找路由
     */
    List<Route> findByProtocol(Route.Protocol protocol);
    
    /**
     * 根据远程端口查找路由
     */
    Optional<Route> findByRemotePort(Integer remotePort);
    
    /**
     * 检查远程端口是否被占用
     */
    boolean existsByRemotePort(Integer remotePort);
    
    /**
     * 根据本地IP和端口查找路由
     */
    List<Route> findByLocalIpAndLocalPort(String localIp, Integer localPort);
    
    /**
     * 查找激活的路由
     */
    @Query("SELECT r FROM Route r WHERE r.status = 'ACTIVE'")
    List<Route> findActiveRoutes();
    
    /**
     * 根据节点和状态查找路由
     */
    List<Route> findByNodeAndStatus(Node node, Route.Status status);
    
    /**
     * 根据名称模糊查询路由
     */
    @Query("SELECT r FROM Route r WHERE r.name LIKE %:keyword% OR r.description LIKE %:keyword%")
    List<Route> findByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计节点的路由数量
     */
    long countByNode(Node node);
    
    /**
     * 统计指定状态的路由数量
     */
    long countByStatus(Route.Status status);
    
    /**
     * 查找指定时间后使用的路由
     */
    List<Route> findByLastUsedAtAfter(LocalDateTime dateTime);
    
    /**
     * 查找可用的端口范围内的路由
     */
    @Query("SELECT r FROM Route r WHERE r.remotePort BETWEEN :startPort AND :endPort")
    List<Route> findByPortRange(@Param("startPort") Integer startPort, @Param("endPort") Integer endPort);
    
    /**
     * 查找下一个可用的远程端口
     */
    @Query("SELECT MIN(r.remotePort + 1) FROM Route r WHERE r.remotePort + 1 NOT IN (SELECT r2.remotePort FROM Route r2) AND r.remotePort BETWEEN :startPort AND :endPort")
    Integer findNextAvailablePort(@Param("startPort") Integer startPort, @Param("endPort") Integer endPort);
}
