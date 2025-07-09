package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.service.ZrokIntegrationService.ZrokTunnelResponse;
import io.github.charlie237.taiyi.service.ZrokIntegrationService.ZrokTunnelStatus;
import io.github.charlie237.taiyi.service.ZrokIntegrationService.ZrokTrafficStats;
import io.github.charlie237.taiyi.websocket.NodeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 隧道管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunnelService {

    private final ZrokIntegrationService zrokService;
    private final RouteService routeService;
    private final NodeWebSocketHandler nodeWebSocketHandler;

    // 存储路由ID到zrok隧道ID的映射
    private final Map<Long, String> routeToTunnelMap = new ConcurrentHashMap<>();
    
    /**
     * 启动隧道
     */
    public void startTunnel(Route route) {
        try {
            // 检查是否已存在隧道
            if (routeToTunnelMap.containsKey(route.getId())) {
                log.warn("隧道已存在: {}", route.getId());
                return;
            }

            // 使用zrok创建隧道
            String userId = route.getNode().getUser().getId().toString();
            ZrokTunnelResponse response = zrokService.createTunnel(
                    userId,
                    route.getLocalIp(),
                    route.getLocalPort(),
                    route.getProtocol().name()
            );

            if (response != null && response.getTunnelId() != null) {
                // 保存隧道映射
                routeToTunnelMap.put(route.getId(), response.getTunnelId());

                // 更新路由状态和公网URL
                route.setStatus(Route.Status.ACTIVE);
                // 如果Route实体有publicUrl字段，可以设置：
                // route.setPublicUrl(response.getPublicUrl());
                routeService.updateRoute(route.getId(), route);

                log.info("zrok隧道启动成功: {} -> {}:{}, 公网地址: {}",
                        route.getRemotePort(), route.getLocalIp(), route.getLocalPort(),
                        response.getPublicUrl());
            } else {
                throw new RuntimeException("zrok隧道创建失败");
            }

        } catch (Exception e) {
            log.error("启动隧道失败: {}", e.getMessage());
            throw new RuntimeException("启动隧道失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止隧道
     */
    public void stopTunnel(Long routeId) {
        try {
            String tunnelId = routeToTunnelMap.remove(routeId);
            if (tunnelId != null) {
                // 使用zrok删除隧道
                boolean success = zrokService.deleteTunnel(tunnelId);

                if (success) {
                    // 停用路由
                    routeService.deactivateRoute(routeId);
                    log.info("zrok隧道停止成功: {}", routeId);
                } else {
                    log.error("zrok隧道停止失败: {}", routeId);
                    // 重新添加映射，以便后续重试
                    routeToTunnelMap.put(routeId, tunnelId);
                }
            } else {
                log.warn("未找到隧道映射: {}", routeId);
            }
        } catch (Exception e) {
            log.error("停止隧道失败: {}", e.getMessage());
        }
    }
    

    

    
    /**
     * 通知节点停止隧道
     */
    private void notifyNodeTunnelStop(Route route) {
        try {
            String nodeId = route.getNode().getNodeId();
            Map<String, Object> data = Map.of(
                    "routeId", route.getId(),
                    "remotePort", route.getRemotePort()
            );
            
            nodeWebSocketHandler.sendMessageToNode(nodeId, "tunnel_stop", "停止隧道", data);
        } catch (Exception e) {
            log.error("通知节点停止隧道失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取活跃隧道数量
     */
    public int getActiveTunnelCount() {
        return routeToTunnelMap.size();
    }

    /**
     * 检查隧道是否活跃
     */
    public boolean isTunnelActive(Long routeId) {
        String tunnelId = routeToTunnelMap.get(routeId);
        if (tunnelId != null) {
            ZrokTunnelStatus status = zrokService.getTunnelStatus(tunnelId);
            return status != null && "active".equalsIgnoreCase(status.getStatus());
        }
        return false;
    }

    /**
     * 停止所有隧道
     */
    public void stopAllTunnels() {
        log.info("停止所有zrok隧道");
        routeToTunnelMap.keySet().forEach(this::stopTunnel);
    }

    /**
     * 获取隧道流量统计
     */
    public ZrokTrafficStats getTunnelTrafficStats(Long routeId) {
        String tunnelId = routeToTunnelMap.get(routeId);
        if (tunnelId != null) {
            return zrokService.getTrafficStats(tunnelId);
        }
        return null;
    }

    /**
     * 获取隧道状态
     */
    public ZrokTunnelStatus getTunnelStatus(Long routeId) {
        String tunnelId = routeToTunnelMap.get(routeId);
        if (tunnelId != null) {
            return zrokService.getTunnelStatus(tunnelId);
        }
        return null;
    }
}
