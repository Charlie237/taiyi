package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 路由服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    
    private final RouteRepository routeRepository;
    
    @Value("${tunnel.port-range.start:10000}")
    private Integer portRangeStart;
    
    @Value("${tunnel.port-range.end:20000}")
    private Integer portRangeEnd;
    
    /**
     * 创建路由
     */
    @Transactional
    public Route createRoute(Route route) {
        // 检查本地IP和端口是否已被使用
        List<Route> existingRoutes = routeRepository.findByLocalIpAndLocalPort(
                route.getLocalIp(), route.getLocalPort());
        if (!existingRoutes.isEmpty()) {
            throw new RuntimeException("本地IP和端口已被使用");
        }
        
        // 分配远程端口
        if (route.getRemotePort() == null) {
            route.setRemotePort(allocateRemotePort());
        } else {
            // 检查指定的远程端口是否可用
            if (routeRepository.existsByRemotePort(route.getRemotePort())) {
                throw new RuntimeException("远程端口已被占用");
            }
        }
        
        // 设置默认值
        if (route.getStatus() == null) {
            route.setStatus(Route.Status.INACTIVE);
        }
        if (route.getMaxConnections() == null) {
            route.setMaxConnections(10);
        }
        if (route.getCurrentConnections() == null) {
            route.setCurrentConnections(0);
        }
        if (route.getTotalBytesIn() == null) {
            route.setTotalBytesIn(0L);
        }
        if (route.getTotalBytesOut() == null) {
            route.setTotalBytesOut(0L);
        }
        
        return routeRepository.save(route);
    }
    
    /**
     * 分配可用的远程端口
     */
    private Integer allocateRemotePort() {
        Random random = new Random();
        int maxAttempts = 100;
        
        for (int i = 0; i < maxAttempts; i++) {
            int port = portRangeStart + random.nextInt(portRangeEnd - portRangeStart + 1);
            if (!routeRepository.existsByRemotePort(port)) {
                return port;
            }
        }
        
        // 如果随机分配失败，尝试顺序分配
        for (int port = portRangeStart; port <= portRangeEnd; port++) {
            if (!routeRepository.existsByRemotePort(port)) {
                return port;
            }
        }
        
        throw new RuntimeException("无可用端口");
    }
    
    /**
     * 激活路由
     */
    @Transactional
    public void activateRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("路由不存在"));
        
        route.setStatus(Route.Status.ACTIVE);
        routeRepository.save(route);
        
        log.info("路由激活: {} -> {}", route.getLocalIp() + ":" + route.getLocalPort(), route.getRemotePort());
    }
    
    /**
     * 停用路由
     */
    @Transactional
    public void deactivateRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("路由不存在"));
        
        route.setStatus(Route.Status.INACTIVE);
        route.setCurrentConnections(0);
        routeRepository.save(route);
        
        log.info("路由停用: {} -> {}", route.getLocalIp() + ":" + route.getLocalPort(), route.getRemotePort());
    }
    
    /**
     * 更新路由统计信息
     */
    @Transactional
    public void updateRouteStats(Long id, Integer connections, Long bytesIn, Long bytesOut) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("路由不存在"));
        
        route.setCurrentConnections(connections);
        route.setTotalBytesIn(route.getTotalBytesIn() + bytesIn);
        route.setTotalBytesOut(route.getTotalBytesOut() + bytesOut);
        route.setLastUsedAt(LocalDateTime.now());
        
        routeRepository.save(route);
    }
    
    /**
     * 根据ID查找路由
     */
    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }
    
    /**
     * 根据节点查找路由
     */
    public List<Route> findByNode(Node node) {
        return routeRepository.findByNode(node);
    }
    
    /**
     * 根据节点ID查找路由
     */
    public List<Route> findByNodeId(Long nodeId) {
        return routeRepository.findByNodeId(nodeId);
    }
    
    /**
     * 获取所有路由（分页）
     */
    public Page<Route> findAll(Pageable pageable) {
        return routeRepository.findAll(pageable);
    }
    
    /**
     * 获取激活的路由
     */
    public List<Route> findActiveRoutes() {
        return routeRepository.findActiveRoutes();
    }
    
    /**
     * 根据关键字搜索路由
     */
    public List<Route> searchRoutes(String keyword) {
        return routeRepository.findByKeyword(keyword);
    }
    
    /**
     * 更新路由信息
     */
    @Transactional
    public Route updateRoute(Long id, Route routeDetails) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("路由不存在"));
        
        if (routeDetails.getName() != null) {
            route.setName(routeDetails.getName());
        }
        if (routeDetails.getDescription() != null) {
            route.setDescription(routeDetails.getDescription());
        }
        if (routeDetails.getMaxConnections() != null) {
            route.setMaxConnections(routeDetails.getMaxConnections());
        }
        if (routeDetails.getBandwidthLimit() != null) {
            route.setBandwidthLimit(routeDetails.getBandwidthLimit());
        }
        if (routeDetails.getCompressionEnabled() != null) {
            route.setCompressionEnabled(routeDetails.getCompressionEnabled());
        }
        if (routeDetails.getEncryptionEnabled() != null) {
            route.setEncryptionEnabled(routeDetails.getEncryptionEnabled());
        }
        
        return routeRepository.save(route);
    }
    
    /**
     * 删除路由
     */
    @Transactional
    public void deleteRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("路由不存在"));
        
        // 如果路由是激活状态，先停用
        if (route.getStatus() == Route.Status.ACTIVE) {
            route.setStatus(Route.Status.INACTIVE);
            routeRepository.save(route);
        }
        
        routeRepository.deleteById(id);
    }
    
    /**
     * 统计路由数量
     */
    public long countRoutes() {
        return routeRepository.count();
    }
    
    /**
     * 统计激活的路由数量
     */
    public long countActiveRoutes() {
        return routeRepository.countByStatus(Route.Status.ACTIVE);
    }
    
    /**
     * 统计节点的路由数量
     */
    public long countByNode(Node node) {
        return routeRepository.countByNode(node);
    }
}
