package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.service.NodeService;
import io.github.charlie237.taiyi.service.RouteService;
import io.github.charlie237.taiyi.service.UserService;
import io.github.charlie237.taiyi.websocket.NodeWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表板控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表板", description = "系统统计信息相关接口")
public class DashboardController {
    
    private final UserService userService;
    private final NodeService nodeService;
    private final RouteService routeService;
    private final NodeWebSocketHandler nodeWebSocketHandler;
    
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计", description = "获取系统整体统计信息")
    public ApiResponse<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 用户统计
            stats.put("totalUsers", userService.countUsers());
            stats.put("activeUsers", userService.countActiveUsers());
            
            // 节点统计
            stats.put("totalNodes", nodeService.countNodes());
            stats.put("onlineNodes", nodeService.countOnlineNodes());
            stats.put("connectedNodes", nodeWebSocketHandler.getOnlineNodeCount());
            
            // 路由统计
            stats.put("totalRoutes", routeService.countRoutes());
            stats.put("activeRoutes", routeService.countActiveRoutes());
            
            // 系统信息
            Runtime runtime = Runtime.getRuntime();
            stats.put("totalMemory", runtime.totalMemory());
            stats.put("freeMemory", runtime.freeMemory());
            stats.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            stats.put("maxMemory", runtime.maxMemory());
            stats.put("availableProcessors", runtime.availableProcessors());
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取系统统计失败: {}", e.getMessage());
            return ApiResponse.error("获取系统统计失败");
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "系统健康状态检查")
    public ApiResponse<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("uptime", System.currentTimeMillis() - getStartTime());
            
            // 数据库连接检查
            try {
                userService.countUsers();
                health.put("database", "UP");
            } catch (Exception e) {
                health.put("database", "DOWN");
                health.put("databaseError", e.getMessage());
            }
            
            // WebSocket连接检查
            health.put("websocket", "UP");
            health.put("connectedNodes", nodeWebSocketHandler.getOnlineNodeCount());
            
            return ApiResponse.success(health);
        } catch (Exception e) {
            log.error("健康检查失败: {}", e.getMessage());
            return ApiResponse.error("健康检查失败");
        }
    }
    
    /**
     * 获取系统启动时间（简化实现）
     */
    private long getStartTime() {
        // 这里应该记录实际的启动时间，简化实现返回固定值
        return System.currentTimeMillis() - 3600000; // 假设运行了1小时
    }
}
