package io.github.charlie237.taiyi.config;

import io.github.charlie237.taiyi.websocket.NodeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final NodeWebSocketHandler nodeWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册节点WebSocket处理器
        registry.addHandler(nodeWebSocketHandler, "/ws/node")
                .setAllowedOrigins("*"); // 生产环境中应该限制允许的源
        
        // 可以添加更多的WebSocket处理器
        // registry.addHandler(adminWebSocketHandler, "/ws/admin")
        //         .setAllowedOrigins("*");
    }
}
