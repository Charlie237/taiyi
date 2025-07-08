package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.ServerNode;
import io.github.charlie237.taiyi.repository.ServerNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 公网服务器节点服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerNodeService {
    
    private final ServerNodeRepository serverNodeRepository;
    
    /**
     * 注册服务器节点
     */
    @Transactional
    public ServerNode registerServerNode(ServerNode serverNode) {
        // 生成唯一服务器ID
        if (serverNode.getServerId() == null || serverNode.getServerId().isEmpty()) {
            serverNode.setServerId(generateServerId());
        }
        
        // 检查服务器ID是否已存在
        if (serverNodeRepository.findByServerId(serverNode.getServerId()).isPresent()) {
            throw new RuntimeException("服务器ID已存在");
        }
        
        // 检查公网IP是否已被注册
        if (serverNodeRepository.existsByPublicIp(serverNode.getPublicIp())) {
            throw new RuntimeException("该公网IP已被注册");
        }
        
        // 设置默认值
        serverNode.setStatus(ServerNode.Status.OFFLINE);
        serverNode.setCurrentConnections(0);
        serverNode.setCurrentBandwidth(0L);
        serverNode.setTotalBytesIn(0L);
        serverNode.setTotalBytesOut(0L);
        
        return serverNodeRepository.save(serverNode);
    }
    
    /**
     * 生成服务器ID
     */
    private String generateServerId() {
        return "server_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 服务器节点上线
     */
    @Transactional
    public void serverNodeOnline(String serverId, String publicIp, Integer publicPort) {
        ServerNode serverNode = serverNodeRepository.findByServerId(serverId)
                .orElseThrow(() -> new RuntimeException("服务器节点不存在"));
        
        serverNode.setStatus(ServerNode.Status.ONLINE);
        serverNode.setPublicIp(publicIp);
        serverNode.setPublicPort(publicPort);
        serverNode.setLastHeartbeat(LocalDateTime.now());
        
        serverNodeRepository.save(serverNode);
        log.info("服务器节点上线: {} ({}:{})", serverId, publicIp, publicPort);
    }
    
    /**
     * 服务器节点离线
     */
    @Transactional
    public void serverNodeOffline(String serverId) {
        ServerNode serverNode = serverNodeRepository.findByServerId(serverId)
                .orElseThrow(() -> new RuntimeException("服务器节点不存在"));
        
        serverNode.setStatus(ServerNode.Status.OFFLINE);
        serverNode.setCurrentConnections(0);
        serverNode.setCurrentBandwidth(0L);
        
        serverNodeRepository.save(serverNode);
        log.info("服务器节点离线: {}", serverId);
    }
    
    /**
     * 更新服务器节点状态
     */
    @Transactional
    public void updateServerNodeStatus(String serverId, ServerNodeStatusUpdate statusUpdate) {
        ServerNode serverNode = serverNodeRepository.findByServerId(serverId)
                .orElseThrow(() -> new RuntimeException("服务器节点不存在"));
        
        serverNode.setCurrentConnections(statusUpdate.getCurrentConnections());
        serverNode.setCurrentBandwidth(statusUpdate.getCurrentBandwidth());
        serverNode.setCpuUsage(statusUpdate.getCpuUsage());
        serverNode.setMemoryUsage(statusUpdate.getMemoryUsage());
        serverNode.setDiskUsage(statusUpdate.getDiskUsage());
        serverNode.setNetworkLatency(statusUpdate.getNetworkLatency());
        serverNode.setLastHeartbeat(LocalDateTime.now());
        
        // 计算负载评分
        double loadScore = calculateLoadScore(statusUpdate);
        serverNode.setLoadScore(loadScore);
        
        // 根据负载情况更新状态
        if (loadScore > 90) {
            serverNode.setStatus(ServerNode.Status.BUSY);
        } else if (serverNode.getStatus() == ServerNode.Status.BUSY && loadScore < 70) {
            serverNode.setStatus(ServerNode.Status.ONLINE);
        }
        
        serverNodeRepository.save(serverNode);
    }
    
    /**
     * 计算负载评分
     */
    private double calculateLoadScore(ServerNodeStatusUpdate statusUpdate) {
        double cpuWeight = 0.4;
        double memoryWeight = 0.3;
        double diskWeight = 0.2;
        double connectionWeight = 0.1;
        
        double cpuScore = statusUpdate.getCpuUsage() != null ? statusUpdate.getCpuUsage() : 0;
        double memoryScore = statusUpdate.getMemoryUsage() != null ? statusUpdate.getMemoryUsage() : 0;
        double diskScore = statusUpdate.getDiskUsage() != null ? statusUpdate.getDiskUsage() : 0;
        double connectionScore = statusUpdate.getCurrentConnections() != null ? 
                (statusUpdate.getCurrentConnections() / 1000.0) * 100 : 0;
        
        return cpuScore * cpuWeight + memoryScore * memoryWeight + 
               diskScore * diskWeight + connectionScore * connectionWeight;
    }
    
    /**
     * 选择最佳服务器节点
     */
    public Optional<ServerNode> selectBestServerNode(String region) {
        List<ServerNode> availableNodes;
        
        if (region != null && !region.isEmpty()) {
            // 优先选择同地区的节点
            availableNodes = serverNodeRepository.findByRegionAndStatusOrderByLoadScoreAsc(
                    region, ServerNode.Status.ONLINE);
            
            if (availableNodes.isEmpty()) {
                // 如果同地区没有可用节点，选择其他地区的
                availableNodes = serverNodeRepository.findByStatusOrderByLoadScoreAsc(
                        ServerNode.Status.ONLINE);
            }
        } else {
            availableNodes = serverNodeRepository.findByStatusOrderByLoadScoreAsc(
                    ServerNode.Status.ONLINE);
        }
        
        return availableNodes.stream()
                .filter(node -> node.getCurrentConnections() < node.getMaxConnections())
                .filter(node -> node.getLoadScore() < 80) // 负载不超过80%
                .findFirst();
    }
    
    /**
     * 获取所有在线服务器节点
     */
    public List<ServerNode> getOnlineServerNodes() {
        return serverNodeRepository.findByStatus(ServerNode.Status.ONLINE);
    }
    
    /**
     * 根据地区获取服务器节点
     */
    public List<ServerNode> getServerNodesByRegion(String region) {
        return serverNodeRepository.findByRegion(region);
    }
    
    /**
     * 清理离线服务器节点
     */
    @Transactional
    public void cleanupOfflineServerNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<ServerNode> offlineNodes = serverNodeRepository.findOfflineNodesBeforeThreshold(threshold);
        
        for (ServerNode node : offlineNodes) {
            if (node.getStatus() == ServerNode.Status.ONLINE) {
                node.setStatus(ServerNode.Status.OFFLINE);
                node.setCurrentConnections(0);
                node.setCurrentBandwidth(0L);
                serverNodeRepository.save(node);
                log.info("服务器节点超时离线: {}", node.getServerId());
            }
        }
    }
    
    /**
     * 服务器节点状态更新DTO
     */
    public static class ServerNodeStatusUpdate {
        private Integer currentConnections;
        private Long currentBandwidth;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double diskUsage;
        private Integer networkLatency;
        
        // Getters and Setters
        public Integer getCurrentConnections() { return currentConnections; }
        public void setCurrentConnections(Integer currentConnections) { this.currentConnections = currentConnections; }
        
        public Long getCurrentBandwidth() { return currentBandwidth; }
        public void setCurrentBandwidth(Long currentBandwidth) { this.currentBandwidth = currentBandwidth; }
        
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public Double getDiskUsage() { return diskUsage; }
        public void setDiskUsage(Double diskUsage) { this.diskUsage = diskUsage; }
        
        public Integer getNetworkLatency() { return networkLatency; }
        public void setNetworkLatency(Integer networkLatency) { this.networkLatency = networkLatency; }
    }
}
