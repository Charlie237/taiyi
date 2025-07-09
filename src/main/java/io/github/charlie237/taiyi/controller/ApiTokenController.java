package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.ApiToken;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.service.ApiTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API Token控制器
 * 商业化Token管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/api-tokens")
@RequiredArgsConstructor
@Tag(name = "API Token管理", description = "商业化Token认证管理")
public class ApiTokenController {
    
    private final ApiTokenService apiTokenService;
    
    @PostMapping
    @Operation(summary = "创建API Token", description = "为当前用户创建新的API Token")
    public ApiResponse<ApiToken> createToken(
            @AuthenticationPrincipal User user,
            @RequestParam String tokenName,
            @RequestParam(defaultValue = "FREE") ApiToken.Plan plan) {
        try {
            ApiToken token = apiTokenService.createToken(user, tokenName, plan);
            return ApiResponse.success(token);
        } catch (Exception e) {
            log.error("创建API Token失败", e);
            return ApiResponse.error("创建Token失败: " + e.getMessage());
        }
    }
    
    @GetMapping
    @Operation(summary = "获取用户Token列表", description = "获取当前用户的所有API Token")
    public ApiResponse<List<ApiToken>> getUserTokens(@AuthenticationPrincipal User user) {
        try {
            List<ApiToken> tokens = apiTokenService.getUserTokens(user);
            // 清除敏感信息
            tokens.forEach(token -> token.setTokenSecret(null));
            return ApiResponse.success(tokens);
        } catch (Exception e) {
            log.error("获取用户Token列表失败", e);
            return ApiResponse.error("获取Token列表失败");
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取Token详情", description = "获取指定Token的详细信息")
    public ApiResponse<ApiToken> getToken(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            ApiToken token = apiTokenService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Token不存在"));
            
            // 检查权限
            if (!token.getUser().getId().equals(user.getId())) {
                return ApiResponse.error("无权访问此Token");
            }
            
            // 清除敏感信息
            token.setTokenSecret(null);
            return ApiResponse.success(token);
        } catch (Exception e) {
            log.error("获取Token详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤销Token", description = "撤销指定的API Token")
    public ApiResponse<String> revokeToken(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            ApiToken token = apiTokenService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Token不存在"));
            
            // 检查权限
            if (!token.getUser().getId().equals(user.getId())) {
                return ApiResponse.error("无权操作此Token");
            }
            
            boolean success = apiTokenService.revokeToken(id);
            if (success) {
                return ApiResponse.success("Token撤销成功");
            } else {
                return ApiResponse.error("Token撤销失败");
            }
        } catch (Exception e) {
            log.error("撤销Token失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/upgrade")
    @Operation(summary = "升级Token套餐", description = "升级Token到指定套餐")
    public ApiResponse<String> upgradeToken(
            @PathVariable Long id,
            @RequestParam ApiToken.Plan newPlan,
            @AuthenticationPrincipal User user) {
        try {
            ApiToken token = apiTokenService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Token不存在"));
            
            // 检查权限
            if (!token.getUser().getId().equals(user.getId())) {
                return ApiResponse.error("无权操作此Token");
            }
            
            boolean success = apiTokenService.updateTokenPlan(id, newPlan);
            if (success) {
                return ApiResponse.success("套餐升级成功");
            } else {
                return ApiResponse.error("套餐升级失败");
            }
        } catch (Exception e) {
            log.error("升级Token套餐失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/{id}/stats")
    @Operation(summary = "获取Token使用统计", description = "获取Token的使用统计信息")
    public ApiResponse<Map<String, Object>> getTokenStats(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            ApiToken token = apiTokenService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Token不存在"));
            
            // 检查权限
            if (!token.getUser().getId().equals(user.getId())) {
                return ApiResponse.error("无权访问此Token");
            }
            
            Map<String, Object> stats = Map.of(
                    "trafficUsed", token.getTrafficUsed(),
                    "trafficLimit", token.getMaxTrafficMonthly(),
                    "trafficUsageRate", token.getTrafficUsageRate(),
                    "remainingTraffic", token.getRemainingTraffic(),
                    "tunnelLimit", token.getMaxTunnels(),
                    "bandwidthLimit", token.getMaxBandwidth(),
                    "lastUsedAt", token.getLastUsedAt(),
                    "trafficResetAt", token.getTrafficResetAt(),
                    "plan", token.getPlan().getDisplayName()
            );
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取Token统计失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    // 管理员接口
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有Token", description = "管理员获取所有Token列表")
    public ApiResponse<Page<ApiToken>> getAllTokens(Pageable pageable) {
        try {
            Page<ApiToken> tokens = apiTokenService.getTokens(pageable);
            // 清除敏感信息
            tokens.getContent().forEach(token -> token.setTokenSecret(null));
            return ApiResponse.success(tokens);
        } catch (Exception e) {
            log.error("获取所有Token失败", e);
            return ApiResponse.error("获取Token列表失败");
        }
    }
    
    @PostMapping("/admin/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员撤销Token", description = "管理员撤销指定Token")
    public ApiResponse<String> adminRevokeToken(@PathVariable Long id) {
        try {
            boolean success = apiTokenService.revokeToken(id);
            if (success) {
                return ApiResponse.success("Token撤销成功");
            } else {
                return ApiResponse.error("Token撤销失败");
            }
        } catch (Exception e) {
            log.error("管理员撤销Token失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取Token统计", description = "获取系统Token使用统计")
    public ApiResponse<Map<String, Object>> getSystemTokenStats() {
        try {
            long activeTokens = apiTokenService.countActiveTokens();
            
            Map<String, Object> stats = Map.of(
                    "activeTokens", activeTokens,
                    "totalTokens", apiTokenService.getTokens(Pageable.unpaged()).getTotalElements()
            );
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取系统Token统计失败", e);
            return ApiResponse.error("获取统计失败");
        }
    }
}
