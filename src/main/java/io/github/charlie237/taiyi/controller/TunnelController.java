package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.service.RouteService;
import io.github.charlie237.taiyi.service.TunnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 隧道控制器
 */
@Slf4j
@RestController
@RequestMapping("/tunnels")
@RequiredArgsConstructor
@Tag(name = "隧道管理", description = "内网穿透隧道管理相关接口")
public class TunnelController {
    
    private final TunnelService tunnelService;
    private final RouteService routeService;
    
    @PostMapping("/start/{routeId}")
    @Operation(summary = "启动隧道", description = "启动指定路由的隧道")
    public ApiResponse<String> startTunnel(@PathVariable Long routeId) {
        try {
            Route route = routeService.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("路由不存在"));
            
            tunnelService.startTunnel(route);
            log.info("隧道启动成功: {}", routeId);
            return ApiResponse.success("隧道启动成功");
        } catch (Exception e) {
            log.error("隧道启动失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/stop/{routeId}")
    @Operation(summary = "停止隧道", description = "停止指定路由的隧道")
    public ApiResponse<String> stopTunnel(@PathVariable Long routeId) {
        try {
            tunnelService.stopTunnel(routeId);
            log.info("隧道停止成功: {}", routeId);
            return ApiResponse.success("隧道停止成功");
        } catch (Exception e) {
            log.error("隧道停止失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/status/{routeId}")
    @Operation(summary = "获取隧道状态", description = "获取指定路由的隧道状态")
    public ApiResponse<Map<String, Object>> getTunnelStatus(@PathVariable Long routeId) {
        try {
            boolean isActive = tunnelService.isTunnelActive(routeId);
            Route route = routeService.findById(routeId).orElse(null);
            
            Map<String, Object> status = Map.of(
                    "routeId", routeId,
                    "isActive", isActive,
                    "routeStatus", route != null ? route.getStatus().name() : "UNKNOWN",
                    "currentConnections", route != null ? route.getCurrentConnections() : 0
            );
            
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("获取隧道状态失败: {}", e.getMessage());
            return ApiResponse.error("获取隧道状态失败");
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "获取隧道统计", description = "获取隧道整体统计信息")
    public ApiResponse<Map<String, Object>> getTunnelStats() {
        try {
            Map<String, Object> stats = Map.of(
                    "activeTunnels", tunnelService.getActiveTunnelCount(),
                    "totalRoutes", routeService.countRoutes(),
                    "activeRoutes", routeService.countActiveRoutes()
            );
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取隧道统计失败: {}", e.getMessage());
            return ApiResponse.error("获取隧道统计失败");
        }
    }
}
