package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 节点服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {
    
    private final NodeRepository nodeRepository;
    
    /**
     * 注册新节点
     */
    @Transactional
    public Node registerNode(Node node) {
        // 生成唯一节点ID
        if (node.getNodeId() == null || node.getNodeId().isEmpty()) {
            node.setNodeId(generateNodeId());
        }
        
        // 检查节点ID是否已存在
        if (nodeRepository.findByNodeId(node.getNodeId()).isPresent()) {
            throw new RuntimeException("节点ID已存在");
        }
        
        // 设置默认状态
        node.setStatus(Node.Status.OFFLINE);
        node.setCurrentConnections(0);
        node.setTotalBytesIn(0L);
        node.setTotalBytesOut(0L);
        
        return nodeRepository.save(node);
    }
    
    /**
     * 生成节点ID
     */
    private String generateNodeId() {
        return "node_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 节点上线
     */
    @Transactional
    public void nodeOnline(String nodeId, String clientIp, Integer clientPort) {
        Node node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        node.setStatus(Node.Status.ONLINE);
        node.setClientIp(clientIp);
        node.setClientPort(clientPort);
        node.setLastHeartbeat(LocalDateTime.now());
        
        nodeRepository.save(node);
        log.info("节点上线: {}", nodeId);
    }
    
    /**
     * 节点离线
     */
    @Transactional
    public void nodeOffline(String nodeId) {
        Node node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        node.setStatus(Node.Status.OFFLINE);
        node.setCurrentConnections(0);
        
        nodeRepository.save(node);
        log.info("节点离线: {}", nodeId);
    }
    
    /**
     * 更新节点心跳
     */
    @Transactional
    public void updateHeartbeat(String nodeId) {
        Node node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        node.setLastHeartbeat(LocalDateTime.now());
        if (node.getStatus() == Node.Status.OFFLINE) {
            node.setStatus(Node.Status.ONLINE);
        }
        
        nodeRepository.save(node);
    }
    
    /**
     * 更新节点统计信息
     */
    @Transactional
    public void updateNodeStats(String nodeId, Integer connections, Long bytesIn, Long bytesOut) {
        Node node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        node.setCurrentConnections(connections);
        node.setTotalBytesIn(node.getTotalBytesIn() + bytesIn);
        node.setTotalBytesOut(node.getTotalBytesOut() + bytesOut);
        
        nodeRepository.save(node);
    }
    
    /**
     * 根据ID查找节点
     */
    public Optional<Node> findById(Long id) {
        return nodeRepository.findById(id);
    }
    
    /**
     * 根据节点ID查找节点
     */
    public Optional<Node> findByNodeId(String nodeId) {
        return nodeRepository.findByNodeId(nodeId);
    }
    
    /**
     * 根据用户查找节点
     */
    public List<Node> findByUser(User user) {
        return nodeRepository.findByUser(user);
    }
    
    /**
     * 根据用户ID查找节点
     */
    public List<Node> findByUserId(Long userId) {
        return nodeRepository.findByUserId(userId);
    }
    
    /**
     * 获取所有节点（分页）
     */
    public Page<Node> findAll(Pageable pageable) {
        return nodeRepository.findAll(pageable);
    }
    
    /**
     * 获取在线节点
     */
    public List<Node> findOnlineNodes() {
        return nodeRepository.findOnlineNodes();
    }
    
    /**
     * 根据关键字搜索节点
     */
    public List<Node> searchNodes(String keyword) {
        return nodeRepository.findByKeyword(keyword);
    }
    
    /**
     * 更新节点信息
     */
    @Transactional
    public Node updateNode(Long id, Node nodeDetails) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        if (nodeDetails.getName() != null) {
            node.setName(nodeDetails.getName());
        }
        if (nodeDetails.getDescription() != null) {
            node.setDescription(nodeDetails.getDescription());
        }
        if (nodeDetails.getMaxConnections() != null) {
            node.setMaxConnections(nodeDetails.getMaxConnections());
        }
        if (nodeDetails.getProtocol() != null) {
            node.setProtocol(nodeDetails.getProtocol());
        }
        
        return nodeRepository.save(node);
    }
    
    /**
     * 删除节点
     */
    @Transactional
    public void deleteNode(Long id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        // 如果节点在线，先设置为离线
        if (node.getStatus() == Node.Status.ONLINE) {
            node.setStatus(Node.Status.OFFLINE);
            nodeRepository.save(node);
        }
        
        nodeRepository.deleteById(id);
    }
    
    /**
     * 检查离线节点并清理
     */
    @Transactional
    public void cleanupOfflineNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5分钟无心跳视为离线
        List<Node> offlineNodes = nodeRepository.findOfflineNodesBeforeThreshold(threshold);
        
        for (Node node : offlineNodes) {
            if (node.getStatus() == Node.Status.ONLINE) {
                node.setStatus(Node.Status.OFFLINE);
                node.setCurrentConnections(0);
                nodeRepository.save(node);
                log.info("节点超时离线: {}", node.getNodeId());
            }
        }
    }
    
    /**
     * 统计节点数量
     */
    public long countNodes() {
        return nodeRepository.count();
    }
    
    /**
     * 统计在线节点数量
     */
    public long countOnlineNodes() {
        return nodeRepository.countByStatus(Node.Status.ONLINE);
    }
    
    /**
     * 统计用户的节点数量
     */
    public long countByUser(User user) {
        return nodeRepository.countByUser(user);
    }
}
