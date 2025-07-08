import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 太乙内网穿透客户端示例
 * 
 * 这是一个简单的Java客户端示例，展示如何连接到太乙服务器
 * 实际的客户端应该根据具体需求进行更复杂的实现
 */
public class TaiyiClient {
    
    private static final String SERVER_URL = "http://localhost:8080/api";
    private static final String WS_URL = "ws://localhost:8080/api/ws/node";
    
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private WebSocket webSocket;
    private String authToken;
    private String nodeId;
    
    public TaiyiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 用户登录
     */
    public boolean login(String username, String password) {
        try {
            String loginJson = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}", 
                username, password
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // 简化的JSON解析，实际应该使用JSON库
                String body = response.body();
                if (body.contains("\"code\":200")) {
                    // 提取token（简化实现）
                    int tokenStart = body.indexOf("\"token\":\"") + 9;
                    int tokenEnd = body.indexOf("\"", tokenStart);
                    this.authToken = body.substring(tokenStart, tokenEnd);
                    System.out.println("登录成功，Token: " + authToken);
                    return true;
                }
            }
            
            System.out.println("登录失败: " + response.body());
            return false;
            
        } catch (Exception e) {
            System.err.println("登录异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 注册节点
     */
    public boolean registerNode(String nodeName, String description) {
        try {
            String nodeJson = String.format(
                "{\"name\":\"%s\",\"description\":\"%s\",\"protocol\":\"TCP\"}", 
                nodeName, description
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/nodes"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(nodeJson))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                if (body.contains("\"code\":200")) {
                    // 提取nodeId（简化实现）
                    int nodeIdStart = body.indexOf("\"nodeId\":\"") + 10;
                    int nodeIdEnd = body.indexOf("\"", nodeIdStart);
                    this.nodeId = body.substring(nodeIdStart, nodeIdEnd);
                    System.out.println("节点注册成功，NodeId: " + nodeId);
                    return true;
                }
            }
            
            System.out.println("节点注册失败: " + response.body());
            return false;
            
        } catch (Exception e) {
            System.err.println("节点注册异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 连接WebSocket
     */
    public boolean connectWebSocket() {
        try {
            URI wsUri = URI.create(WS_URL + "?nodeId=" + nodeId);
            
            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    System.out.println("WebSocket连接已建立");
                    TaiyiClient.this.webSocket = webSocket;
                    
                    // 启动心跳
                    startHeartbeat();
                }
                
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    System.out.println("收到服务器消息: " + data);
                    return null;
                }
                
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    System.out.println("WebSocket连接已关闭: " + reason);
                    return null;
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    System.err.println("WebSocket错误: " + error.getMessage());
                }
            };
            
            HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(wsUri, listener)
                    .join();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("WebSocket连接异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (webSocket != null) {
                String heartbeat = String.format(
                    "{\"type\":\"heartbeat\",\"message\":\"心跳\",\"data\":{},\"timestamp\":%d}",
                    System.currentTimeMillis()
                );
                webSocket.sendText(heartbeat, true);
                System.out.println("发送心跳");
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 发送状态更新
     */
    public void sendStatusUpdate(int connections, long bytesIn, long bytesOut) {
        if (webSocket != null) {
            String statusUpdate = String.format(
                "{\"type\":\"status_update\",\"message\":\"状态更新\",\"data\":{\"connections\":%d,\"bytesIn\":%d,\"bytesOut\":%d},\"timestamp\":%d}",
                connections, bytesIn, bytesOut, System.currentTimeMillis()
            );
            webSocket.sendText(statusUpdate, true);
            System.out.println("发送状态更新");
        }
    }
    
    /**
     * 关闭连接
     */
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "客户端关闭");
        }
        scheduler.shutdown();
        httpClient.close();
    }
    
    /**
     * 主方法 - 示例用法
     */
    public static void main(String[] args) {
        TaiyiClient client = new TaiyiClient();
        
        try {
            // 1. 登录
            if (!client.login("admin", "admin123")) {
                System.exit(1);
            }
            
            // 2. 注册节点
            if (!client.registerNode("测试节点", "这是一个测试节点")) {
                System.exit(1);
            }
            
            // 3. 连接WebSocket
            if (!client.connectWebSocket()) {
                System.exit(1);
            }
            
            // 4. 模拟运行
            Thread.sleep(5000);
            client.sendStatusUpdate(3, 1024, 2048);
            
            // 5. 保持运行
            System.out.println("客户端运行中，按Enter键退出...");
            System.in.read();
            
        } catch (Exception e) {
            System.err.println("客户端运行异常: " + e.getMessage());
        } finally {
            client.close();
        }
    }
}
