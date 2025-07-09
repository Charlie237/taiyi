import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 太乙内网穿透客户端
 * 使用Java 11+ 原生WebSocket API
 */
public class TaiyiClient {
    
    private static final String SERVER_URL = "ws://localhost:8080/api/ws/nodes";
    private static final String NODE_ID = "node_test123456789"; // 从服务器注册获取
    
    private WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Socket> localConnections = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        TaiyiClient client = new TaiyiClient();
        client.start();
        
        // 保持程序运行
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            client.stop();
        }
    }
    
    public void start() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(SERVER_URL), new WebSocketListener())
                    .join();
            
            System.out.println("太乙客户端启动成功");
            
        } catch (Exception e) {
            System.err.println("启动客户端失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stop() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "客户端关闭");
        }
        // 关闭所有本地连接
        localConnections.values().forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略
            }
        });
    }
    
    /**
     * WebSocket监听器
     */
    private class WebSocketListener implements WebSocket.Listener {
        
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("连接到太乙服务器成功");
            sendNodeRegister();
            WebSocket.Listener.super.onOpen(webSocket);
        }
        
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            handleServerMessage(data.toString());
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("与服务器连接断开: " + reason);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("WebSocket错误: " + error.getMessage());
            error.printStackTrace();
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
    
    /**
     * 发送节点注册消息
     */
    private void sendNodeRegister() {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "register");
            message.put("nodeId", NODE_ID);
            message.put("timestamp", System.currentTimeMillis());
            
            String json = objectMapper.writeValueAsString(message);
            webSocket.sendText(json, true);
            
            System.out.println("发送节点注册消息: " + NODE_ID);
        } catch (Exception e) {
            System.err.println("发送注册消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理服务器消息
     */
    private void handleServerMessage(String message) {
        try {
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String messageType = (String) messageData.get("type");
            
            System.out.println("收到服务器消息: " + messageType);
            
            switch (messageType) {
                case "tunnel_start":
                    handleTunnelStart(messageData);
                    break;
                case "tunnel_stop":
                    handleTunnelStop(messageData);
                    break;
                case "new_connection":
                    handleNewConnection(messageData);
                    break;
                case "data_forward":
                    handleDataForward(messageData);
                    break;
                case "connection_closed":
                    handleConnectionClosed(messageData);
                    break;
                case "heartbeat":
                    handleHeartbeat();
                    break;
                default:
                    System.out.println("未知消息类型: " + messageType);
            }
            
        } catch (Exception e) {
            System.err.println("处理服务器消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理隧道启动
     */
    private void handleTunnelStart(Map<String, Object> messageData) {
        Map<String, Object> data = (Map<String, Object>) messageData.get("data");
        if (data != null) {
            String localIp = (String) data.get("localIp");
            Integer localPort = (Integer) data.get("localPort");
            Integer remotePort = (Integer) data.get("remotePort");
            
            System.out.println(String.format("隧道启动: %s:%d -> 远程端口:%d", localIp, localPort, remotePort));
        }
    }
    
    /**
     * 处理隧道停止
     */
    private void handleTunnelStop(Map<String, Object> messageData) {
        System.out.println("隧道停止");
    }
    
    /**
     * 处理新连接
     */
    private void handleNewConnection(Map<String, Object> messageData) {
        Map<String, Object> data = (Map<String, Object>) messageData.get("data");
        if (data != null) {
            String connectionId = (String) data.get("connectionId");
            String localIp = (String) data.get("localIp");
            Integer localPort = (Integer) data.get("localPort");
            
            System.out.println(String.format("新连接: %s -> %s:%d", connectionId, localIp, localPort));
            
            // 连接到本地服务
            connectToLocalService(connectionId, localIp, localPort);
        }
    }
    
    /**
     * 处理数据转发
     */
    private void handleDataForward(Map<String, Object> messageData) {
        Map<String, Object> data = (Map<String, Object>) messageData.get("data");
        if (data != null) {
            String connectionId = (String) data.get("connectionId");
            byte[] forwardData = (byte[]) data.get("data");
            
            // 转发数据到本地服务
            forwardDataToLocal(connectionId, forwardData);
        }
    }
    
    /**
     * 处理连接关闭
     */
    private void handleConnectionClosed(Map<String, Object> messageData) {
        Map<String, Object> data = (Map<String, Object>) messageData.get("data");
        if (data != null) {
            String connectionId = (String) data.get("connectionId");
            closeLocalConnection(connectionId);
        }
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat() {
        sendHeartbeatResponse();
    }
    
    /**
     * 连接到本地服务
     */
    private void connectToLocalService(String connectionId, String localIp, int localPort) {
        try {
            Socket localSocket = new Socket(localIp, localPort);
            localConnections.put(connectionId, localSocket);
            
            // 启动数据读取线程
            new Thread(() -> readFromLocalService(connectionId, localSocket)).start();
            
            System.out.println("连接到本地服务成功: " + connectionId);
            
        } catch (IOException e) {
            System.err.println("连接到本地服务失败: " + e.getMessage());
            sendConnectionResponse(connectionId, "close");
        }
    }
    
    /**
     * 从本地服务读取数据
     */
    private void readFromLocalService(String connectionId, Socket localSocket) {
        try (InputStream inputStream = localSocket.getInputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);
                
                // 发送数据到服务器
                sendDataResponse(connectionId, data);
            }
            
        } catch (IOException e) {
            System.err.println("读取本地服务数据失败: " + e.getMessage());
        } finally {
            closeLocalConnection(connectionId);
        }
    }
    
    /**
     * 转发数据到本地服务
     */
    private void forwardDataToLocal(String connectionId, byte[] data) {
        Socket localSocket = localConnections.get(connectionId);
        if (localSocket != null && !localSocket.isClosed()) {
            try {
                localSocket.getOutputStream().write(data);
                localSocket.getOutputStream().flush();
                System.out.println("转发数据到本地服务: " + data.length + " bytes");
            } catch (IOException e) {
                System.err.println("转发数据到本地服务失败: " + e.getMessage());
                closeLocalConnection(connectionId);
            }
        }
    }
    
    /**
     * 关闭本地连接
     */
    private void closeLocalConnection(String connectionId) {
        Socket localSocket = localConnections.remove(connectionId);
        if (localSocket != null) {
            try {
                localSocket.close();
                System.out.println("关闭本地连接: " + connectionId);
            } catch (IOException e) {
                // 忽略
            }
        }
    }
    
    /**
     * 发送数据响应
     */
    private void sendDataResponse(String connectionId, byte[] data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "data_response");
            message.put("data", Map.of(
                    "connectionId", connectionId,
                    "data", data
            ));
            
            String json = objectMapper.writeValueAsString(message);
            webSocket.sendText(json, true);
            
        } catch (Exception e) {
            System.err.println("发送数据响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送连接响应
     */
    private void sendConnectionResponse(String connectionId, String action) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "connection_response");
            message.put("data", Map.of(
                    "connectionId", connectionId,
                    "action", action
            ));
            
            String json = objectMapper.writeValueAsString(message);
            webSocket.sendText(json, true);
            
        } catch (Exception e) {
            System.err.println("发送连接响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送心跳响应
     */
    private void sendHeartbeatResponse() {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "heartbeat");
            message.put("timestamp", System.currentTimeMillis());
            
            String json = objectMapper.writeValueAsString(message);
            webSocket.sendText(json, true);
            
        } catch (Exception e) {
            System.err.println("发送心跳响应失败: " + e.getMessage());
        }
    }
}
