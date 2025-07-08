package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.Route;
import io.github.charlie237.taiyi.service.NodeService;
import io.github.charlie237.taiyi.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 路由控制器
 */
@Slf4j
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Tag(name = "路由管理", description = "端口映射路由管理相关接口")
public class RouteController {
    
    private final RouteService routeService;
    private final NodeService nodeService;
    
    @GetMapping
    @Operation(summary = "获取路由列表", description = "获取所有路由列表")
    public ApiResponse<Page<Route>> getRoutes(Pageable pageable) {
        try {
            Page<Route> routes = routeService.findAll(pageable);
            return ApiResponse.success(routes);
        } catch (Exception e) {
            log.error("获取路由列表失败: {}", e.getMessage());
            return ApiResponse.error("获取路由列表失败");
        }
    }
    
    @GetMapping("/node/{nodeId}")
    @Operation(summary = "获取节点路由", description = "获取指定节点的路由列表")
    public ApiResponse<List<Route>> getRoutesByNode(@PathVariable Long nodeId) {
        try {
            List<Route> routes = routeService.findByNodeId(nodeId);
            return ApiResponse.success(routes);
        } catch (Exception e) {
            log.error("获取节点路由失败: {}", e.getMessage());
            return ApiResponse.error("获取节点路由失败");
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取路由详情", description = "根据ID获取路由详情")
    public ApiResponse<Route> getRoute(@PathVariable Long id) {
        try {
            Route route = routeService.findById(id)
                    .orElseThrow(() -> new RuntimeException("路由不存在"));
            return ApiResponse.success(route);
        } catch (Exception e) {
            log.error("获取路由详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping
    @Operation(summary = "创建路由", description = "创建新的端口映射路由")
    public ApiResponse<Route> createRoute(@RequestBody Route route) {
        try {
            // 验证节点是否存在
            Node node = nodeService.findById(route.getNode().getId())
                    .orElseThrow(() -> new RuntimeException("节点不存在"));
            route.setNode(node);
            
            Route savedRoute = routeService.createRoute(route);
            log.info("路由创建成功: {} -> {}", 
                    savedRoute.getLocalIp() + ":" + savedRoute.getLocalPort(), 
                    savedRoute.getRemotePort());
            return ApiResponse.success("路由创建成功", savedRoute);
        } catch (Exception e) {
            log.error("路由创建失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新路由", description = "更新路由信息")
    public ApiResponse<Route> updateRoute(@PathVariable Long id, @RequestBody Route route) {
        try {
            Route updatedRoute = routeService.updateRoute(id, route);
            log.info("路由更新成功: {}", updatedRoute.getId());
            return ApiResponse.success("路由更新成功", updatedRoute);
        } catch (Exception e) {
            log.error("路由更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除路由", description = "删除指定路由")
    public ApiResponse<String> deleteRoute(@PathVariable Long id) {
        try {
            routeService.deleteRoute(id);
            log.info("路由删除成功: {}", id);
            return ApiResponse.success("路由删除成功");
        } catch (Exception e) {
            log.error("路由删除失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/activate")
    @Operation(summary = "激活路由", description = "激活指定路由")
    public ApiResponse<String> activateRoute(@PathVariable Long id) {
        try {
            routeService.activateRoute(id);
            log.info("路由激活成功: {}", id);
            return ApiResponse.success("路由激活成功");
        } catch (Exception e) {
            log.error("路由激活失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用路由", description = "停用指定路由")
    public ApiResponse<String> deactivateRoute(@PathVariable Long id) {
        try {
            routeService.deactivateRoute(id);
            log.info("路由停用成功: {}", id);
            return ApiResponse.success("路由停用成功");
        } catch (Exception e) {
            log.error("路由停用失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/active")
    @Operation(summary = "获取激活路由", description = "获取所有激活状态的路由")
    public ApiResponse<List<Route>> getActiveRoutes() {
        try {
            List<Route> routes = routeService.findActiveRoutes();
            return ApiResponse.success(routes);
        } catch (Exception e) {
            log.error("获取激活路由失败: {}", e.getMessage());
            return ApiResponse.error("获取激活路由失败");
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索路由", description = "根据关键字搜索路由")
    public ApiResponse<List<Route>> searchRoutes(@RequestParam String keyword) {
        try {
            List<Route> routes = routeService.searchRoutes(keyword);
            return ApiResponse.success(routes);
        } catch (Exception e) {
            log.error("搜索路由失败: {}", e.getMessage());
            return ApiResponse.error("搜索路由失败");
        }
    }
}
