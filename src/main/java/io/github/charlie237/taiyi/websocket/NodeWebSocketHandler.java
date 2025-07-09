package io.github.charlie237.taiyi.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.charlie237.taiyi.common.Constants;
import io.github.charlie237.taiyi.service.NodeService;
import io.github.charlie237.taiyi.service.NodeStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点WebSocket处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeWebSocketHandler implements WebSocketHandler {

    private final NodeService nodeService;
    private final NodeStatusService nodeStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储节点连接
    private final Map<String, WebSocketSession> nodeSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());
        
        // 从查询参数中获取节点ID
        String nodeId = getNodeIdFromSession(session);
        if (nodeId != null) {
            nodeSessions.put(nodeId, session);
            log.info("节点连接成功: {}", nodeId);
            
            // 发送连接成功消息
            sendMessage(session, createMessage(Constants.MessageType.INFO, "连接成功", null));
        } else {
            log.warn("无效的节点连接，缺少nodeId参数");
            session.close(CloseStatus.BAD_DATA);
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String nodeId = getNodeIdFromSession(session);
        if (nodeId == null) {
            return;
        }
        
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> messageData = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            String messageType = (String) messageData.get("type");
            
            log.debug("收到节点消息: {} - {}", nodeId, messageType);
            
            switch (messageType) {
                case Constants.MessageType.HEARTBEAT:
                    handleHeartbeat(nodeId, messageData);
                    break;
                case Constants.MessageType.STATUS_UPDATE:
                    handleStatusUpdate(nodeId, messageData);
                    break;
                case "hardware_status":
                    handleHardwareStatus(nodeId, messageData);
                    break;
                case "data_response":
                    handleDataResponse(nodeId, messageData);
                    break;
                case "connection_response":
                    handleConnectionResponse(nodeId, messageData);
                    break;
                default:
                    log.warn("未知消息类型: {}", messageType);
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage());
            sendMessage(session, createMessage(Constants.MessageType.ERROR, "消息处理失败", null));
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String nodeId = getNodeIdFromSession(session);
        log.error("WebSocket传输错误: {} - {}", nodeId, exception.getMessage());
        
        if (nodeId != null) {
            nodeSessions.remove(nodeId);
            // 标记节点离线
            try {
                nodeService.nodeOffline(nodeId);
            } catch (Exception e) {
                log.error("标记节点离线失败: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String nodeId = getNodeIdFromSession(session);
        log.info("WebSocket连接关闭: {} - {}", nodeId, closeStatus);
        
        if (nodeId != null) {
            nodeSessions.remove(nodeId);
            // 标记节点离线
            try {
                nodeService.nodeOffline(nodeId);
            } catch (Exception e) {
                log.error("标记节点离线失败: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(String nodeId, Map<String, Object> messageData) {
        try {
            nodeService.updateHeartbeat(nodeId);
            
            // 回复心跳确认
            WebSocketSession session = nodeSessions.get(nodeId);
            if (session != null && session.isOpen()) {
                sendMessage(session, createMessage(Constants.MessageType.HEARTBEAT, "心跳确认", null));
            }
        } catch (Exception e) {
            log.error("处理心跳失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理状态更新消息
     */
    private void handleStatusUpdate(String nodeId, Map<String, Object> messageData) {
        try {
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                Integer connections = (Integer) data.get("connections");
                Long bytesIn = ((Number) data.get("bytesIn")).longValue();
                Long bytesOut = ((Number) data.get("bytesOut")).longValue();

                nodeService.updateNodeStats(nodeId, connections, bytesIn, bytesOut);
            }
        } catch (Exception e) {
            log.error("处理状态更新失败: {}", e.getMessage());
        }
    }

    /**
     * 处理硬件状态消息
     */
    private void handleHardwareStatus(String nodeId, Map<String, Object> messageData) {
        try {
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                // 记录节点硬件状态
                nodeStatusService.recordNodeStatus(nodeId, data);

                // 发送确认消息
                WebSocketSession session = nodeSessions.get(nodeId);
                if (session != null && session.isOpen()) {
                    sendMessage(session, createMessage("hardware_status_ack", "硬件状态已记录", null));
                }
            }
        } catch (Exception e) {
            log.error("处理硬件状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送消息到节点
     */
    public void sendMessageToNode(String nodeId, String messageType, String message, Object data) {
        WebSocketSession session = nodeSessions.get(nodeId);
        if (session != null && session.isOpen()) {
            sendMessage(session, createMessage(messageType, message, data));
        } else {
            log.warn("节点不在线或连接已断开: {}", nodeId);
        }
    }
    
    /**
     * 广播消息到所有在线节点
     */
    public void broadcastMessage(String messageType, String message, Object data) {
        String messageJson = createMessage(messageType, message, data);
        nodeSessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, messageJson);
            }
        });
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 创建消息
     */
    private String createMessage(String type, String message, Object data) {
        try {
            Map<String, Object> messageMap = Map.of(
                    "type", type,
                    "message", message,
                    "data", data != null ? data : Map.of(),
                    "timestamp", System.currentTimeMillis()
            );
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            log.error("创建消息失败: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * 从会话中获取节点ID
     */
    private String getNodeIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "nodeId".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }
    
    /**
     * 处理数据响应（从内网节点返回的数据）
     */
    private void handleDataResponse(String nodeId, Map<String, Object> messageData) {
        try {
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                String connectionId = (String) data.get("connectionId");
                byte[] responseData = (byte[]) data.get("data");

                if (connectionId != null && responseData != null) {
                    log.debug("处理数据响应: 连接ID={}, 数据长度={}", connectionId, responseData.length);
                    // zrok会自动处理数据转发，这里只需要记录日志
                }
            }
        } catch (Exception e) {
            log.error("处理数据响应失败: {}", e.getMessage());
        }
    }

    /**
     * 处理连接响应
     */
    private void handleConnectionResponse(String nodeId, Map<String, Object> messageData) {
        try {
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                String connectionId = (String) data.get("connectionId");
                String action = (String) data.get("action");

                if ("close".equals(action) && connectionId != null) {
                    log.debug("处理连接关闭响应: 连接ID={}", connectionId);
                    // zrok会自动处理连接关闭，这里只需要记录日志
                }
            }
        } catch (Exception e) {
            log.error("处理连接响应失败: {}", e.getMessage());
        }
    }

    /**
     * 获取在线节点数量
     */
    public int getOnlineNodeCount() {
        return nodeSessions.size();
    }
    
    /**
     * 检查节点是否在线
     */
    public boolean isNodeOnline(String nodeId) {
        WebSocketSession session = nodeSessions.get(nodeId);
        return session != null && session.isOpen();
    }
}
