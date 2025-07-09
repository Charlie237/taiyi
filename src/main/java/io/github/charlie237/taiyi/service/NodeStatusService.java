package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.NodeStatus;
import io.github.charlie237.taiyi.repository.NodeRepository;
import io.github.charlie237.taiyi.repository.NodeStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节点状态监控服务
 * 负责节点硬件信息收集、状态监控和告警处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeStatusService {
    
    private final NodeRepository nodeRepository;
    private final NodeStatusRepository nodeStatusRepository;
    private final AlertService alertService;
    
    // 告警阈值配置
    private static final double CPU_WARNING_THRESHOLD = 80.0;
    private static final double CPU_CRITICAL_THRESHOLD = 95.0;
    private static final double MEMORY_WARNING_THRESHOLD = 85.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 95.0;
    private static final double DISK_WARNING_THRESHOLD = 90.0;
    private static final double DISK_CRITICAL_THRESHOLD = 98.0;
    private static final int PING_WARNING_THRESHOLD = 200; // 毫秒
    private static final int PING_CRITICAL_THRESHOLD = 500; // 毫秒
    
    /**
     * 记录节点状态信息
     */
    @Transactional
    public void recordNodeStatus(String nodeId, Map<String, Object> statusData) {
        try {
            Optional<Node> nodeOpt = nodeRepository.findByNodeId(nodeId);
            if (nodeOpt.isEmpty()) {
                log.warn("节点不存在: {}", nodeId);
                return;
            }
            
            Node node = nodeOpt.get();
            NodeStatus status = new NodeStatus();
            status.setNode(node);
            
            // 解析状态数据
            parseStatusData(status, statusData);
            
            // 保存状态记录
            nodeStatusRepository.save(status);
            
            // 检查告警条件
            checkAlerts(node, status);
            
            log.debug("记录节点状态: {} - CPU: {}%, 内存: {}%, 磁盘: {}%", 
                    nodeId, status.getCpuUsage(), status.getMemoryUsage(), status.getDiskUsage());
                    
        } catch (Exception e) {
            log.error("记录节点状态失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解析状态数据
     */
    private void parseStatusData(NodeStatus status, Map<String, Object> data) {
        // CPU使用率
        if (data.containsKey("cpuUsage")) {
            status.setCpuUsage(((Number) data.get("cpuUsage")).doubleValue());
        }
        
        // 内存使用率
        if (data.containsKey("memoryUsage")) {
            status.setMemoryUsage(((Number) data.get("memoryUsage")).doubleValue());
        }
        
        // 磁盘使用率
        if (data.containsKey("diskUsage")) {
            status.setDiskUsage(((Number) data.get("diskUsage")).doubleValue());
        }
        
        // 网络流量
        if (data.containsKey("networkIn")) {
            status.setNetworkIn(((Number) data.get("networkIn")).longValue());
        }
        if (data.containsKey("networkOut")) {
            status.setNetworkOut(((Number) data.get("networkOut")).longValue());
        }
        
        // 连接数
        if (data.containsKey("connectionCount")) {
            status.setConnectionCount(((Number) data.get("connectionCount")).intValue());
        }
        
        // 运行时间
        if (data.containsKey("uptime")) {
            status.setUptime(((Number) data.get("uptime")).longValue());
        }
        
        // 系统负载
        if (data.containsKey("loadAverage")) {
            status.setLoadAverage(((Number) data.get("loadAverage")).doubleValue());
        }
        
        // 温度
        if (data.containsKey("temperature")) {
            status.setTemperature(((Number) data.get("temperature")).doubleValue());
        }
        
        // 延迟
        if (data.containsKey("pingLatency")) {
            status.setPingLatency(((Number) data.get("pingLatency")).intValue());
        }
        
        // 带宽
        if (data.containsKey("bandwidthIn")) {
            status.setBandwidthIn(((Number) data.get("bandwidthIn")).longValue());
        }
        if (data.containsKey("bandwidthOut")) {
            status.setBandwidthOut(((Number) data.get("bandwidthOut")).longValue());
        }
    }
    
    /**
     * 检查告警条件
     */
    private void checkAlerts(Node node, NodeStatus status) {
        String nodeId = node.getNodeId();
        
        // CPU告警检查
        if (status.getCpuUsage() != null) {
            if (status.getCpuUsage() >= CPU_CRITICAL_THRESHOLD) {
                alertService.sendAlert(nodeId, "CPU", "CRITICAL", 
                        String.format("CPU使用率达到 %.1f%%", status.getCpuUsage()));
                status.setErrorCount(status.getErrorCount() + 1);
            } else if (status.getCpuUsage() >= CPU_WARNING_THRESHOLD) {
                alertService.sendAlert(nodeId, "CPU", "WARNING", 
                        String.format("CPU使用率达到 %.1f%%", status.getCpuUsage()));
                status.setWarningCount(status.getWarningCount() + 1);
            }
        }
        
        // 内存告警检查
        if (status.getMemoryUsage() != null) {
            if (status.getMemoryUsage() >= MEMORY_CRITICAL_THRESHOLD) {
                alertService.sendAlert(nodeId, "MEMORY", "CRITICAL", 
                        String.format("内存使用率达到 %.1f%%", status.getMemoryUsage()));
                status.setErrorCount(status.getErrorCount() + 1);
            } else if (status.getMemoryUsage() >= MEMORY_WARNING_THRESHOLD) {
                alertService.sendAlert(nodeId, "MEMORY", "WARNING", 
                        String.format("内存使用率达到 %.1f%%", status.getMemoryUsage()));
                status.setWarningCount(status.getWarningCount() + 1);
            }
        }
        
        // 磁盘告警检查
        if (status.getDiskUsage() != null) {
            if (status.getDiskUsage() >= DISK_CRITICAL_THRESHOLD) {
                alertService.sendAlert(nodeId, "DISK", "CRITICAL", 
                        String.format("磁盘使用率达到 %.1f%%", status.getDiskUsage()));
                status.setErrorCount(status.getErrorCount() + 1);
            } else if (status.getDiskUsage() >= DISK_WARNING_THRESHOLD) {
                alertService.sendAlert(nodeId, "DISK", "WARNING", 
                        String.format("磁盘使用率达到 %.1f%%", status.getDiskUsage()));
                status.setWarningCount(status.getWarningCount() + 1);
            }
        }
        
        // 网络延迟告警检查
        if (status.getPingLatency() != null) {
            if (status.getPingLatency() >= PING_CRITICAL_THRESHOLD) {
                alertService.sendAlert(nodeId, "NETWORK", "CRITICAL", 
                        String.format("网络延迟达到 %d ms", status.getPingLatency()));
                status.setErrorCount(status.getErrorCount() + 1);
            } else if (status.getPingLatency() >= PING_WARNING_THRESHOLD) {
                alertService.sendAlert(nodeId, "NETWORK", "WARNING", 
                        String.format("网络延迟达到 %d ms", status.getPingLatency()));
                status.setWarningCount(status.getWarningCount() + 1);
            }
        }
    }
    
    /**
     * 获取节点最新状态
     */
    public Optional<NodeStatus> getLatestNodeStatus(String nodeId) {
        Optional<Node> nodeOpt = nodeRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            return nodeStatusRepository.findLatestByNode(nodeOpt.get());
        }
        return Optional.empty();
    }
    
    /**
     * 获取节点状态历史记录
     */
    public Page<NodeStatus> getNodeStatusHistory(String nodeId, Pageable pageable) {
        Optional<Node> nodeOpt = nodeRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            return nodeStatusRepository.findByNodeOrderByRecordedAtDesc(nodeOpt.get(), pageable);
        }
        return Page.empty();
    }
    
    /**
     * 获取节点状态统计
     */
    public Map<String, Object> getNodeStatusStats(String nodeId, LocalDateTime startTime, LocalDateTime endTime) {
        Optional<Node> nodeOpt = nodeRepository.findByNodeId(nodeId);
        if (nodeOpt.isEmpty()) {
            return Map.of();
        }
        
        Node node = nodeOpt.get();
        List<NodeStatus> statusList = nodeStatusRepository.findByNodeAndRecordedAtBetween(
                node, startTime, endTime);
        
        if (statusList.isEmpty()) {
            return Map.of();
        }
        
        // 计算统计数据
        double avgCpu = statusList.stream()
                .filter(s -> s.getCpuUsage() != null)
                .mapToDouble(NodeStatus::getCpuUsage)
                .average().orElse(0.0);
                
        double avgMemory = statusList.stream()
                .filter(s -> s.getMemoryUsage() != null)
                .mapToDouble(NodeStatus::getMemoryUsage)
                .average().orElse(0.0);
                
        double avgDisk = statusList.stream()
                .filter(s -> s.getDiskUsage() != null)
                .mapToDouble(NodeStatus::getDiskUsage)
                .average().orElse(0.0);
                
        double maxCpu = statusList.stream()
                .filter(s -> s.getCpuUsage() != null)
                .mapToDouble(NodeStatus::getCpuUsage)
                .max().orElse(0.0);
                
        double maxMemory = statusList.stream()
                .filter(s -> s.getMemoryUsage() != null)
                .mapToDouble(NodeStatus::getMemoryUsage)
                .max().orElse(0.0);
                
        int totalErrors = statusList.stream()
                .mapToInt(s -> s.getErrorCount() != null ? s.getErrorCount() : 0)
                .sum();
                
        int totalWarnings = statusList.stream()
                .mapToInt(s -> s.getWarningCount() != null ? s.getWarningCount() : 0)
                .sum();
        
        return Map.of(
                "avgCpuUsage", Math.round(avgCpu * 100.0) / 100.0,
                "avgMemoryUsage", Math.round(avgMemory * 100.0) / 100.0,
                "avgDiskUsage", Math.round(avgDisk * 100.0) / 100.0,
                "maxCpuUsage", Math.round(maxCpu * 100.0) / 100.0,
                "maxMemoryUsage", Math.round(maxMemory * 100.0) / 100.0,
                "totalErrors", totalErrors,
                "totalWarnings", totalWarnings,
                "recordCount", statusList.size(),
                "startTime", startTime,
                "endTime", endTime
        );
    }
    
    /**
     * 清理旧的状态记录
     */
    @Transactional
    public void cleanupOldStatusRecords(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        int deletedCount = nodeStatusRepository.deleteByRecordedAtBefore(cutoffTime);
        log.info("清理旧状态记录: {} 条", deletedCount);
    }
}
