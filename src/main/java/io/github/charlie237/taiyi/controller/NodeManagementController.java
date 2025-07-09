package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.NodeStatus;
import io.github.charlie237.taiyi.service.NodeService;
import io.github.charlie237.taiyi.service.NodeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节点管理控制器
 * 用于管理zrok边缘节点的注册、状态监控等
 */
@Slf4j
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
@Tag(name = "节点管理", description = "zrok边缘节点管理相关接口")
public class NodeManagementController {
    
    private final NodeService nodeService;
    private final NodeStatusService nodeStatusService;
    
    @PostMapping("/register")
    @Operation(summary = "注册边缘节点", description = "边缘节点向控制中心注册")
    public ApiResponse<String> registerNode(@RequestBody Map<String, Object> nodeInfo) {
        try {
            String nodeId = (String) nodeInfo.get("nodeId");
            String nodeName = (String) nodeInfo.get("nodeName");
            String serverIp = (String) nodeInfo.get("serverIp");
            
            if (nodeId == null || nodeName == null) {
                return ApiResponse.error("节点ID和名称不能为空");
            }
            
            // 检查节点是否已存在
            Optional<Node> existingNode = nodeService.findByNodeId(nodeId);
            if (existingNode.isPresent()) {
                // 更新现有节点信息
                Node node = existingNode.get();
                node.setName(nodeName);
                node.setServerIp(serverIp);
                node.setStatus(Node.Status.ONLINE);
                node.setLastHeartbeat(LocalDateTime.now());
                nodeService.updateNode(node);
                
                log.info("节点重新上线: {} ({})", nodeName, nodeId);
                return ApiResponse.success("节点重新上线成功");
            } else {
                // 创建新节点
                Node node = new Node();
                node.setNodeId(nodeId);
                node.setName(nodeName);
                node.setServerIp(serverIp);
                node.setStatus(Node.Status.ONLINE);
                node.setDescription("边缘节点: " + nodeName);
                
                // 设置系统信息
                if (nodeInfo.containsKey("osName")) {
                    node.setDescription(node.getDescription() + 
                            " | " + nodeInfo.get("osName") + 
                            " " + nodeInfo.get("osVersion") + 
                            " (" + nodeInfo.get("osArch") + ")");
                }
                
                nodeService.registerNode(node);
                log.info("新节点注册成功: {} ({})", nodeName, nodeId);
                return ApiResponse.success("节点注册成功");
            }
            
        } catch (Exception e) {
            log.error("节点注册失败", e);
            return ApiResponse.error("节点注册失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/unregister")
    @Operation(summary = "注销边缘节点", description = "边缘节点从控制中心注销")
    public ApiResponse<String> unregisterNode(@RequestBody Map<String, Object> data) {
        try {
            String nodeId = (String) data.get("nodeId");
            if (nodeId == null) {
                return ApiResponse.error("节点ID不能为空");
            }
            
            nodeService.nodeOffline(nodeId);
            log.info("节点注销成功: {}", nodeId);
            return ApiResponse.success("节点注销成功");
            
        } catch (Exception e) {
            log.error("节点注销失败", e);
            return ApiResponse.error("节点注销失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/heartbeat")
    @Operation(summary = "节点心跳", description = "边缘节点发送心跳保活")
    public ApiResponse<String> nodeHeartbeat(@RequestBody Map<String, Object> heartbeat) {
        try {
            String nodeId = (String) heartbeat.get("nodeId");
            if (nodeId == null) {
                return ApiResponse.error("节点ID不能为空");
            }
            
            nodeService.updateHeartbeat(nodeId);
            log.debug("收到节点心跳: {}", nodeId);
            return ApiResponse.success("心跳接收成功");
            
        } catch (Exception e) {
            log.error("处理节点心跳失败", e);
            return ApiResponse.error("心跳处理失败: " + e.getMessage());
        }
    }
    
    // 管理员接口
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有节点", description = "管理员获取所有边缘节点列表")
    public ApiResponse<Page<Node>> getAllNodes(Pageable pageable) {
        try {
            Page<Node> nodes = nodeService.getAllNodes(pageable);
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("获取节点列表失败", e);
            return ApiResponse.error("获取节点列表失败");
        }
    }
    
    @GetMapping("/{nodeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取节点详情", description = "获取指定节点的详细信息")
    public ApiResponse<Node> getNodeDetails(@PathVariable String nodeId) {
        try {
            Optional<Node> node = nodeService.findByNodeId(nodeId);
            if (node.isPresent()) {
                return ApiResponse.success(node.get());
            } else {
                return ApiResponse.error("节点不存在");
            }
        } catch (Exception e) {
            log.error("获取节点详情失败", e);
            return ApiResponse.error("获取节点详情失败");
        }
    }
    
    @GetMapping("/online")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取在线节点", description = "获取所有在线的边缘节点")
    public ApiResponse<List<Node>> getOnlineNodes() {
        try {
            List<Node> onlineNodes = nodeService.getOnlineNodes();
            return ApiResponse.success(onlineNodes);
        } catch (Exception e) {
            log.error("获取在线节点失败", e);
            return ApiResponse.error("获取在线节点失败");
        }
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取节点统计", description = "获取节点整体统计信息")
    public ApiResponse<Map<String, Object>> getNodeStats() {
        try {
            long totalNodes = nodeService.countNodes();
            long onlineNodes = nodeService.countOnlineNodes();
            long offlineNodes = totalNodes - onlineNodes;
            
            Map<String, Object> stats = Map.of(
                    "totalNodes", totalNodes,
                    "onlineNodes", onlineNodes,
                    "offlineNodes", offlineNodes,
                    "onlineRate", totalNodes > 0 ? (double) onlineNodes / totalNodes * 100 : 0.0,
                    "timestamp", LocalDateTime.now()
            );
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取节点统计失败", e);
            return ApiResponse.error("获取节点统计失败");
        }
    }
    
    @PostMapping("/{nodeId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "启用节点", description = "管理员启用指定节点")
    public ApiResponse<String> enableNode(@PathVariable String nodeId) {
        try {
            Optional<Node> nodeOpt = nodeService.findByNodeId(nodeId);
            if (nodeOpt.isEmpty()) {
                return ApiResponse.error("节点不存在");
            }
            
            Node node = nodeOpt.get();
            node.setStatus(Node.Status.ONLINE);
            nodeService.updateNode(node);
            
            log.info("管理员启用节点: {}", nodeId);
            return ApiResponse.success("节点启用成功");
            
        } catch (Exception e) {
            log.error("启用节点失败", e);
            return ApiResponse.error("启用节点失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{nodeId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "禁用节点", description = "管理员禁用指定节点")
    public ApiResponse<String> disableNode(@PathVariable String nodeId) {
        try {
            Optional<Node> nodeOpt = nodeService.findByNodeId(nodeId);
            if (nodeOpt.isEmpty()) {
                return ApiResponse.error("节点不存在");
            }
            
            Node node = nodeOpt.get();
            node.setStatus(Node.Status.OFFLINE);
            nodeService.updateNode(node);
            
            log.info("管理员禁用节点: {}", nodeId);
            return ApiResponse.success("节点禁用成功");
            
        } catch (Exception e) {
            log.error("禁用节点失败", e);
            return ApiResponse.error("禁用节点失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{nodeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除节点", description = "管理员删除指定节点")
    public ApiResponse<String> deleteNode(@PathVariable String nodeId) {
        try {
            boolean success = nodeService.deleteNode(nodeId);
            if (success) {
                log.info("管理员删除节点: {}", nodeId);
                return ApiResponse.success("节点删除成功");
            } else {
                return ApiResponse.error("节点不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("删除节点失败", e);
            return ApiResponse.error("删除节点失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{nodeId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取节点监控状态", description = "获取节点的最新监控状态")
    public ApiResponse<NodeStatus> getNodeStatus(@PathVariable String nodeId) {
        try {
            Optional<NodeStatus> status = nodeStatusService.getLatestNodeStatus(nodeId);
            if (status.isPresent()) {
                return ApiResponse.success(status.get());
            } else {
                return ApiResponse.error("未找到节点监控数据");
            }
        } catch (Exception e) {
            log.error("获取节点监控状态失败", e);
            return ApiResponse.error("获取监控状态失败");
        }
    }
}
