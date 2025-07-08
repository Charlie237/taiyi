package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.websocket.NodeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 隧道管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunnelService {
    
    private final NodeWebSocketHandler nodeWebSocketHandler;
    private final RouteService routeService;
    
    // 存储活跃的隧道连接
    private final Map<Long, ServerSocket> activeTunnels = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 启动隧道
     */
    public void startTunnel(Route route) {
        if (activeTunnels.containsKey(route.getId())) {
            log.warn("隧道已存在: {}", route.getId());
            return;
        }
        
        try {
            ServerSocket serverSocket = new ServerSocket(route.getRemotePort());
            activeTunnels.put(route.getId(), serverSocket);
            
            // 激活路由
            routeService.activateRoute(route.getId());
            
            // 启动监听线程
            executorService.submit(() -> {
                log.info("隧道启动成功: {} -> {}:{}", 
                        route.getRemotePort(), route.getLocalIp(), route.getLocalPort());
                
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // 处理客户端连接
                        executorService.submit(() -> handleClientConnection(route, clientSocket));
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            log.error("接受客户端连接失败: {}", e.getMessage());
                        }
                        break;
                    }
                }
            });
            
            // 通知节点启动隧道
            notifyNodeTunnelStart(route);
            
        } catch (IOException e) {
            log.error("启动隧道失败: {}", e.getMessage());
            throw new RuntimeException("启动隧道失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止隧道
     */
    public void stopTunnel(Long routeId) {
        ServerSocket serverSocket = activeTunnels.remove(routeId);
        if (serverSocket != null) {
            try {
                serverSocket.close();
                
                // 停用路由
                routeService.deactivateRoute(routeId);
                
                log.info("隧道停止成功: {}", routeId);
                
                // 通知节点停止隧道
                Route route = routeService.findById(routeId).orElse(null);
                if (route != null) {
                    notifyNodeTunnelStop(route);
                }
                
            } catch (IOException e) {
                log.error("停止隧道失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 处理客户端连接
     */
    private void handleClientConnection(Route route, Socket clientSocket) {
        try {
            log.debug("新的客户端连接: {} -> {}:{}", 
                    clientSocket.getRemoteSocketAddress(), route.getLocalIp(), route.getLocalPort());
            
            // 这里应该实现实际的数据转发逻辑
            // 简化实现：直接关闭连接
            clientSocket.close();
            
        } catch (IOException e) {
            log.error("处理客户端连接失败: {}", e.getMessage());
        }
    }
    
    /**
     * 通知节点启动隧道
     */
    private void notifyNodeTunnelStart(Route route) {
        try {
            String nodeId = route.getNode().getNodeId();
            Map<String, Object> data = Map.of(
                    "routeId", route.getId(),
                    "localIp", route.getLocalIp(),
                    "localPort", route.getLocalPort(),
                    "remotePort", route.getRemotePort(),
                    "protocol", route.getProtocol().name()
            );
            
            nodeWebSocketHandler.sendMessageToNode(nodeId, "tunnel_start", "启动隧道", data);
        } catch (Exception e) {
            log.error("通知节点启动隧道失败: {}", e.getMessage());
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
        return activeTunnels.size();
    }
    
    /**
     * 检查隧道是否活跃
     */
    public boolean isTunnelActive(Long routeId) {
        ServerSocket serverSocket = activeTunnels.get(routeId);
        return serverSocket != null && !serverSocket.isClosed();
    }
    
    /**
     * 停止所有隧道
     */
    public void stopAllTunnels() {
        log.info("停止所有隧道");
        activeTunnels.keySet().forEach(this::stopTunnel);
    }
}
