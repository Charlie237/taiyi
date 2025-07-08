package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.Node;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节点控制器
 */
@Slf4j
@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
@Tag(name = "节点管理", description = "内网节点管理相关接口")
public class NodeController {
    
    private final NodeService nodeService;
    
    @GetMapping
    @Operation(summary = "获取节点列表", description = "获取当前用户的节点列表")
    public ApiResponse<List<Node>> getNodes(@AuthenticationPrincipal User user) {
        try {
            List<Node> nodes = nodeService.findByUser(user);
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("获取节点列表失败: {}", e.getMessage());
            return ApiResponse.error("获取节点列表失败");
        }
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页获取节点列表", description = "分页获取当前用户的节点列表")
    public ApiResponse<Page<Node>> getNodesPage(@AuthenticationPrincipal User user, Pageable pageable) {
        try {
            // 这里需要实现按用户分页查询，暂时返回所有节点
            Page<Node> nodes = nodeService.findAll(pageable);
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("分页获取节点列表失败: {}", e.getMessage());
            return ApiResponse.error("分页获取节点列表失败");
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取节点详情", description = "根据ID获取节点详情")
    public ApiResponse<Node> getNode(@PathVariable Long id) {
        try {
            Node node = nodeService.findById(id)
                    .orElseThrow(() -> new RuntimeException("节点不存在"));
            return ApiResponse.success(node);
        } catch (Exception e) {
            log.error("获取节点详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping
    @Operation(summary = "注册节点", description = "注册新的内网节点")
    public ApiResponse<Node> registerNode(@RequestBody Node node, @AuthenticationPrincipal User user) {
        try {
            node.setUser(user);
            Node savedNode = nodeService.registerNode(node);
            log.info("节点注册成功: {}", savedNode.getNodeId());
            return ApiResponse.success("节点注册成功", savedNode);
        } catch (Exception e) {
            log.error("节点注册失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新节点", description = "更新节点信息")
    public ApiResponse<Node> updateNode(@PathVariable Long id, @RequestBody Node node) {
        try {
            Node updatedNode = nodeService.updateNode(id, node);
            log.info("节点更新成功: {}", updatedNode.getNodeId());
            return ApiResponse.success("节点更新成功", updatedNode);
        } catch (Exception e) {
            log.error("节点更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除节点", description = "删除指定节点")
    public ApiResponse<String> deleteNode(@PathVariable Long id) {
        try {
            nodeService.deleteNode(id);
            log.info("节点删除成功: {}", id);
            return ApiResponse.success("节点删除成功");
        } catch (Exception e) {
            log.error("节点删除失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{nodeId}/online")
    @Operation(summary = "节点上线", description = "标记节点为在线状态")
    public ApiResponse<String> nodeOnline(@PathVariable String nodeId, 
                                         @RequestParam String clientIp,
                                         @RequestParam Integer clientPort) {
        try {
            nodeService.nodeOnline(nodeId, clientIp, clientPort);
            return ApiResponse.success("节点上线成功");
        } catch (Exception e) {
            log.error("节点上线失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{nodeId}/offline")
    @Operation(summary = "节点离线", description = "标记节点为离线状态")
    public ApiResponse<String> nodeOffline(@PathVariable String nodeId) {
        try {
            nodeService.nodeOffline(nodeId);
            return ApiResponse.success("节点离线成功");
        } catch (Exception e) {
            log.error("节点离线失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{nodeId}/heartbeat")
    @Operation(summary = "节点心跳", description = "更新节点心跳时间")
    public ApiResponse<String> updateHeartbeat(@PathVariable String nodeId) {
        try {
            nodeService.updateHeartbeat(nodeId);
            return ApiResponse.success("心跳更新成功");
        } catch (Exception e) {
            log.error("心跳更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/online")
    @Operation(summary = "获取在线节点", description = "获取所有在线节点列表")
    public ApiResponse<List<Node>> getOnlineNodes() {
        try {
            List<Node> nodes = nodeService.findOnlineNodes();
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("获取在线节点失败: {}", e.getMessage());
            return ApiResponse.error("获取在线节点失败");
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索节点", description = "根据关键字搜索节点")
    public ApiResponse<List<Node>> searchNodes(@RequestParam String keyword) {
        try {
            List<Node> nodes = nodeService.searchNodes(keyword);
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("搜索节点失败: {}", e.getMessage());
            return ApiResponse.error("搜索节点失败");
        }
    }
}
