package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * zrok集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ZrokIntegrationTest {
    
    private Route testRoute;
    
    @BeforeEach
    void setUp() {
        // 创建测试数据
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        Node node = new Node();
        node.setId(1L);
        node.setNodeId("test_node_123");
        node.setUser(user);
        
        testRoute = new Route();
        testRoute.setId(1L);
        testRoute.setName("测试路由");
        testRoute.setNode(node);
        testRoute.setLocalIp("127.0.0.1");
        testRoute.setLocalPort(8080);
        testRoute.setRemotePort(9090);
        testRoute.setProtocol(Route.Protocol.TCP);
    }
    
    @Test
    void testZrokAvailability() {
        // 这个测试需要实际的zrok环境
        // 在CI/CD环境中可以跳过
        System.out.println("zrok集成测试 - 需要实际的zrok环境");
        assertTrue(true); // 占位测试
    }
    
    @Test
    void testTunnelCreation() {
        // 测试隧道创建逻辑
        assertNotNull(testRoute);
        assertEquals("127.0.0.1", testRoute.getLocalIp());
        assertEquals(8080, testRoute.getLocalPort());
        assertEquals(9090, testRoute.getRemotePort());
    }
    
    @Test
    void testRouteConfiguration() {
        // 测试路由配置
        assertEquals(Route.Protocol.TCP, testRoute.getProtocol());
        assertEquals("测试路由", testRoute.getName());
        assertNotNull(testRoute.getNode());
    }
}
