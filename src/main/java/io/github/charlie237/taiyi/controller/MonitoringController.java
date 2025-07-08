package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.service.AuditLogService;
import io.github.charlie237.taiyi.service.NodeService;
import io.github.charlie237.taiyi.service.RouteService;
import io.github.charlie237.taiyi.service.UserService;
import io.github.charlie237.taiyi.websocket.NodeWebSocketHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Tag(name = "系统监控", description = "系统监控和统计相关接口")
public class MonitoringController {
    
    private final UserService userService;
    private final NodeService nodeService;
    private final RouteService routeService;
    private final AuditLogService auditLogService;
    private final NodeWebSocketHandler nodeWebSocketHandler;
    private final MeterRegistry meterRegistry;
    
    @GetMapping("/system/info")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取系统信息", description = "获取详细的系统运行信息")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = new HashMap<>();
            
            // JVM信息
            Runtime runtime = Runtime.getRuntime();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            Map<String, Object> jvmInfo = new HashMap<>();
            jvmInfo.put("totalMemory", runtime.totalMemory());
            jvmInfo.put("freeMemory", runtime.freeMemory());
            jvmInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmInfo.put("maxMemory", runtime.maxMemory());
            jvmInfo.put("availableProcessors", runtime.availableProcessors());
            jvmInfo.put("heapMemoryUsed", memoryBean.getHeapMemoryUsage().getUsed());
            jvmInfo.put("heapMemoryMax", memoryBean.getHeapMemoryUsage().getMax());
            jvmInfo.put("nonHeapMemoryUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            
            systemInfo.put("jvm", jvmInfo);
            
            // 操作系统信息
            Map<String, Object> osInfo = new HashMap<>();
            osInfo.put("name", osBean.getName());
            osInfo.put("version", osBean.getVersion());
            osInfo.put("arch", osBean.getArch());
            osInfo.put("availableProcessors", osBean.getAvailableProcessors());
            osInfo.put("systemLoadAverage", osBean.getSystemLoadAverage());
            
            systemInfo.put("os", osInfo);
            
            // 应用信息
            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("startTime", ManagementFactory.getRuntimeMXBean().getStartTime());
            appInfo.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
            appInfo.put("javaVersion", System.getProperty("java.version"));
            appInfo.put("javaVendor", System.getProperty("java.vendor"));
            
            systemInfo.put("application", appInfo);
            
            return ApiResponse.success(systemInfo);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return ApiResponse.error("获取系统信息失败");
        }
    }
    
    @GetMapping("/metrics/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取指标摘要", description = "获取系统关键指标摘要")
    public ApiResponse<Map<String, Object>> getMetricsSummary() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // 业务指标
            metrics.put("totalUsers", userService.countUsers());
            metrics.put("activeUsers", userService.countActiveUsers());
            metrics.put("totalNodes", nodeService.countNodes());
            metrics.put("onlineNodes", nodeService.countOnlineNodes());
            metrics.put("totalRoutes", routeService.countRoutes());
            metrics.put("activeRoutes", routeService.countActiveRoutes());
            metrics.put("connectedWebSockets", nodeWebSocketHandler.getOnlineNodeCount());
            
            // 性能指标
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
            metrics.put("memoryUsagePercent", Math.round(memoryUsage * 100.0) / 100.0);
            
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
            metrics.put("availableProcessors", osBean.getAvailableProcessors());
            
            // 时间信息
            metrics.put("currentTime", LocalDateTime.now());
            metrics.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
            
            return ApiResponse.success(metrics);
        } catch (Exception e) {
            log.error("获取指标摘要失败", e);
            return ApiResponse.error("获取指标摘要失败");
        }
    }
    
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能指标", description = "获取详细的性能监控指标")
    public ApiResponse<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> performance = new HashMap<>();
            
            // 内存指标
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("total", runtime.totalMemory());
            memory.put("free", runtime.freeMemory());
            memory.put("used", runtime.totalMemory() - runtime.freeMemory());
            memory.put("max", runtime.maxMemory());
            memory.put("usagePercent", (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100);
            
            performance.put("memory", memory);
            
            // GC信息
            Map<String, Object> gc = new HashMap<>();
            ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
                Map<String, Object> gcInfo = new HashMap<>();
                gcInfo.put("collectionCount", gcBean.getCollectionCount());
                gcInfo.put("collectionTime", gcBean.getCollectionTime());
                gc.put(gcBean.getName(), gcInfo);
            });
            
            performance.put("gc", gc);
            
            // 线程信息
            Map<String, Object> threads = new HashMap<>();
            threads.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
            threads.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            threads.put("daemonThreadCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
            
            performance.put("threads", threads);
            
            return ApiResponse.success(performance);
        } catch (Exception e) {
            log.error("获取性能指标失败", e);
            return ApiResponse.error("获取性能指标失败");
        }
    }
    
    @PostMapping("/gc")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "触发垃圾回收", description = "手动触发JVM垃圾回收")
    public ApiResponse<String> triggerGC() {
        try {
            long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.gc();
            Thread.sleep(1000); // 等待GC完成
            long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freedMemory = beforeMemory - afterMemory;
            
            log.info("手动触发GC，释放内存: {} bytes", freedMemory);
            return ApiResponse.success(String.format("GC完成，释放内存: %d bytes", freedMemory));
        } catch (Exception e) {
            log.error("触发GC失败", e);
            return ApiResponse.error("触发GC失败");
        }
    }
    
    @GetMapping("/health/detailed")
    @Operation(summary = "详细健康检查", description = "获取详细的系统健康状态")
    public ApiResponse<Map<String, Object>> getDetailedHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 数据库健康检查
            try {
                userService.countUsers();
                health.put("database", Map.of("status", "UP", "message", "数据库连接正常"));
            } catch (Exception e) {
                health.put("database", Map.of("status", "DOWN", "message", "数据库连接异常: " + e.getMessage()));
            }
            
            // WebSocket健康检查
            health.put("websocket", Map.of(
                    "status", "UP",
                    "connectedNodes", nodeWebSocketHandler.getOnlineNodeCount(),
                    "message", "WebSocket服务正常"
            ));
            
            // 内存健康检查
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100;
            String memoryStatus = memoryUsage > 90 ? "WARN" : "UP";
            health.put("memory", Map.of(
                    "status", memoryStatus,
                    "usagePercent", memoryUsage,
                    "message", memoryUsage > 90 ? "内存使用率过高" : "内存使用正常"
            ));
            
            // 整体状态
            boolean allHealthy = health.values().stream()
                    .allMatch(component -> {
                        if (component instanceof Map) {
                            return "UP".equals(((Map<?, ?>) component).get("status"));
                        }
                        return true;
                    });
            
            health.put("overall", Map.of(
                    "status", allHealthy ? "UP" : "DOWN",
                    "timestamp", LocalDateTime.now()
            ));
            
            return ApiResponse.success(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ApiResponse.error("健康检查失败");
        }
    }
}
