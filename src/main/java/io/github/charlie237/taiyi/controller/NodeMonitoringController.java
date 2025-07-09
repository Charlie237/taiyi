package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.NodeStatus;
import io.github.charlie237.taiyi.service.NodeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 节点监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/node-monitoring")
@RequiredArgsConstructor
@Tag(name = "节点监控", description = "节点硬件状态监控相关接口")
public class NodeMonitoringController {
    
    private final NodeStatusService nodeStatusService;
    
    @GetMapping("/{nodeId}/latest")
    @Operation(summary = "获取节点最新状态", description = "获取指定节点的最新硬件状态信息")
    public ApiResponse<NodeStatus> getLatestNodeStatus(@PathVariable String nodeId) {
        try {
            Optional<NodeStatus> status = nodeStatusService.getLatestNodeStatus(nodeId);
            if (status.isPresent()) {
                return ApiResponse.success(status.get());
            } else {
                return ApiResponse.error("未找到节点状态信息");
            }
        } catch (Exception e) {
            log.error("获取节点最新状态失败: {}", e.getMessage());
            return ApiResponse.error("获取节点状态失败");
        }
    }
    
    @GetMapping("/{nodeId}/history")
    @Operation(summary = "获取节点状态历史", description = "获取指定节点的状态历史记录")
    public ApiResponse<Page<NodeStatus>> getNodeStatusHistory(
            @PathVariable String nodeId,
            Pageable pageable) {
        try {
            Page<NodeStatus> history = nodeStatusService.getNodeStatusHistory(nodeId, pageable);
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("获取节点状态历史失败: {}", e.getMessage());
            return ApiResponse.error("获取状态历史失败");
        }
    }
    
    @GetMapping("/{nodeId}/stats")
    @Operation(summary = "获取节点状态统计", description = "获取指定时间范围内的节点状态统计信息")
    public ApiResponse<Map<String, Object>> getNodeStatusStats(
            @PathVariable String nodeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> stats = nodeStatusService.getNodeStatusStats(nodeId, startTime, endTime);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取节点状态统计失败: {}", e.getMessage());
            return ApiResponse.error("获取状态统计失败");
        }
    }
    
    @GetMapping("/{nodeId}/stats/24h")
    @Operation(summary = "获取24小时状态统计", description = "获取节点最近24小时的状态统计")
    public ApiResponse<Map<String, Object>> get24HourStats(@PathVariable String nodeId) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);
            Map<String, Object> stats = nodeStatusService.getNodeStatusStats(nodeId, startTime, endTime);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取24小时状态统计失败: {}", e.getMessage());
            return ApiResponse.error("获取状态统计失败");
        }
    }
    
    @GetMapping("/{nodeId}/stats/7d")
    @Operation(summary = "获取7天状态统计", description = "获取节点最近7天的状态统计")
    public ApiResponse<Map<String, Object>> get7DayStats(@PathVariable String nodeId) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(7);
            Map<String, Object> stats = nodeStatusService.getNodeStatusStats(nodeId, startTime, endTime);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取7天状态统计失败: {}", e.getMessage());
            return ApiResponse.error("获取状态统计失败");
        }
    }
    
    @PostMapping("/{nodeId}/status")
    @Operation(summary = "上报节点状态", description = "客户端上报节点硬件状态信息")
    public ApiResponse<String> reportNodeStatus(
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> statusData) {
        try {
            nodeStatusService.recordNodeStatus(nodeId, statusData);
            return ApiResponse.success("状态上报成功");
        } catch (Exception e) {
            log.error("节点状态上报失败: {}", e.getMessage());
            return ApiResponse.error("状态上报失败: " + e.getMessage());
        }
    }
    
    // 管理员接口
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "清理旧状态记录", description = "清理指定天数之前的状态记录")
    public ApiResponse<String> cleanupOldRecords(@RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            nodeStatusService.cleanupOldStatusRecords(daysToKeep);
            return ApiResponse.success("清理完成");
        } catch (Exception e) {
            log.error("清理旧状态记录失败: {}", e.getMessage());
            return ApiResponse.error("清理失败");
        }
    }
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "监控仪表板数据", description = "获取监控仪表板所需的汇总数据")
    public ApiResponse<Map<String, Object>> getDashboardData() {
        try {
            // TODO: 实现仪表板数据聚合
            Map<String, Object> dashboardData = Map.of(
                    "message", "仪表板数据功能待实现",
                    "timestamp", LocalDateTime.now()
            );
            return ApiResponse.success(dashboardData);
        } catch (Exception e) {
            log.error("获取仪表板数据失败: {}", e.getMessage());
            return ApiResponse.error("获取仪表板数据失败");
        }
    }
}
