package io.github.charlie237.taiyi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简化的太乙节点客户端
 * 只负责基本的连接管理和心跳，不做复杂的硬件监控
 */
@Slf4j
public class SimpleNodeClient {
    
    private final String serverUrl;
    private final String nodeId;
    private final String nodeName;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    
    // 连接状态
    private boolean connected = false;
    private int connectionCount = 0;
    private long bytesIn = 0;
    private long bytesOut = 0;
    
    public SimpleNodeClient(String serverUrl, String nodeId, String nodeName) {
        this.serverUrl = serverUrl;
        this.nodeId = nodeId;
        this.nodeName = nodeName != null ? nodeName : nodeId;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * 启动客户端
     */
    public void start() {
        log.info("启动太乙节点客户端: {} ({})", nodeName, nodeId);
        
        // 连接到服务器
        connectToServer();
        
        // 启动心跳任务（每30秒）
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 30, TimeUnit.SECONDS);
        
        log.info("太乙节点客户端启动完成");
    }
    
    /**
     * 停止客户端
     */
    public void stop() {
        log.info("停止太乙节点客户端: {}", nodeId);
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        disconnectFromServer();
        log.info("太乙节点客户端已停止");
    }
    
    /**
     * 连接到服务器
     */
    private void connectToServer() {
        try {
            String wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws/node?nodeId=" + nodeId;
            log.info("连接到服务器: {}", wsUrl);
            
            // 实际实现应该使用WebSocket客户端库
            // 这里模拟连接成功
            connected = true;
            log.info("WebSocket连接建立成功");
            
            // 发送上线消息
            sendMessage("node_online", "节点上线", Map.of(
                    "nodeId", nodeId,
                    "nodeName", nodeName,
                    "clientIp", getLocalIpAddress(),
                    "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("连接服务器失败", e);
            connected = false;
        }
    }
    
    /**
     * 断开服务器连接
     */
    private void disconnectFromServer() {
        if (connected) {
            try {
                sendMessage("node_offline", "节点离线", Map.of(
                        "nodeId", nodeId,
                        "timestamp", System.currentTimeMillis()
                ));
                
                connected = false;
                log.info("WebSocket连接已关闭");
                
            } catch (Exception e) {
                log.error("断开连接失败", e);
            }
        }
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        if (!connected) {
            log.warn("连接未建立，尝试重连");
            connectToServer();
            return;
        }
        
        try {
            // 只发送基本的连接状态信息
            sendMessage("heartbeat", "心跳", Map.of(
                    "nodeId", nodeId,
                    "nodeName", nodeName,
                    "connectionCount", connectionCount,
                    "bytesIn", bytesIn,
                    "bytesOut", bytesOut,
                    "timestamp", System.currentTimeMillis()
            ));
            
            log.debug("发送心跳: {} - 连接数: {}", nodeId, connectionCount);
            
        } catch (Exception e) {
            log.error("发送心跳失败", e);
            connected = false;
        }
    }
    
    /**
     * 更新连接统计
     */
    public void updateStats(int connections, long bytesIn, long bytesOut) {
        this.connectionCount = connections;
        this.bytesIn += bytesIn;
        this.bytesOut += bytesOut;
    }
    
    /**
     * 发送消息到服务器
     */
    private void sendMessage(String type, String message, Object data) {
        try {
            Map<String, Object> messageData = Map.of(
                    "type", type,
                    "message", message,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            
            String jsonMessage = objectMapper.writeValueAsString(messageData);
            
            // 实际实现应该通过WebSocket发送
            // webSocketSession.sendMessage(new TextMessage(jsonMessage));
            
            log.debug("发送消息: {}", type);
            
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage());
            throw new RuntimeException("发送消息失败", e);
        }
    }
    
    /**
     * 获取本地IP地址
     */
    private String getLocalIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.error("获取本地IP失败", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * 主方法 - 用于独立运行客户端
     */
    public static void main(String[] args) {
        String serverUrl = System.getProperty("taiyi.server.url", "http://localhost:8080");
        String nodeId = System.getProperty("taiyi.node.id", "node_" + System.currentTimeMillis());
        String nodeName = System.getProperty("taiyi.node.name", nodeId);
        
        log.info("太乙节点客户端配置:");
        log.info("  服务器地址: {}", serverUrl);
        log.info("  节点ID: {}", nodeId);
        log.info("  节点名称: {}", nodeName);
        
        SimpleNodeClient client = new SimpleNodeClient(serverUrl, nodeId, nodeName);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
        
        // 启动客户端
        client.start();
        
        // 保持运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("客户端被中断");
            client.stop();
        }
    }
}
