package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.service.UserService;
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
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的个人信息")
    public ApiResponse<User> getUserProfile(@AuthenticationPrincipal User user) {
        try {
            User currentUser = userService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            // 清除敏感信息
            currentUser.setPassword(null);
            return ApiResponse.success(currentUser);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的个人信息")
    public ApiResponse<User> updateUserProfile(@AuthenticationPrincipal User user, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(user.getId(), userDetails);
            // 清除敏感信息
            updatedUser.setPassword(null);
            log.info("用户信息更新成功: {}", user.getUsername());
            return ApiResponse.success("用户信息更新成功", updatedUser);
        } catch (Exception e) {
            log.error("用户信息更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public ApiResponse<String> changePassword(@AuthenticationPrincipal User user, 
                                             @RequestBody Map<String, String> passwordData) {
        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ApiResponse.badRequest("原密码和新密码不能为空");
            }
            
            userService.changePassword(user.getId(), oldPassword, newPassword);
            log.info("用户密码修改成功: {}", user.getUsername());
            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    // 管理员接口
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户列表", description = "获取所有用户列表（管理员）")
    public ApiResponse<Page<User>> getUsers(Pageable pageable) {
        try {
            Page<User> users = userService.findAll(pageable);
            // 清除敏感信息
            users.getContent().forEach(u -> u.setPassword(null));
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage());
            return ApiResponse.error("获取用户列表失败");
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详情（管理员）")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            // 清除敏感信息
            user.setPassword(null);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("获取用户详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户", description = "更新用户信息（管理员）")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            // 清除敏感信息
            updatedUser.setPassword(null);
            log.info("用户更新成功: {}", updatedUser.getUsername());
            return ApiResponse.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            log.error("用户更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除用户", description = "删除指定用户（管理员）")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            log.info("用户删除成功: {}", id);
            return ApiResponse.success("用户删除成功");
        } catch (Exception e) {
            log.error("用户删除失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "重置密码", description = "重置用户密码（管理员）")
    public ApiResponse<String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> passwordData) {
        try {
            String newPassword = passwordData.get("newPassword");
            if (newPassword == null) {
                return ApiResponse.badRequest("新密码不能为空");
            }
            
            userService.resetPassword(id, newPassword);
            log.info("用户密码重置成功: {}", id);
            return ApiResponse.success("密码重置成功");
        } catch (Exception e) {
            log.error("密码重置失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户状态", description = "更新用户状态（管理员）")
    public ApiResponse<String> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> statusData) {
        try {
            String status = statusData.get("status");
            if (status == null) {
                return ApiResponse.badRequest("状态不能为空");
            }
            
            User.Status userStatus = User.Status.valueOf(status.toUpperCase());
            userService.updateUserStatus(id, userStatus);
            log.info("用户状态更新成功: {} -> {}", id, status);
            return ApiResponse.success("用户状态更新成功");
        } catch (Exception e) {
            log.error("用户状态更新失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "搜索用户", description = "根据关键字搜索用户（管理员）")
    public ApiResponse<List<User>> searchUsers(@RequestParam String keyword) {
        try {
            List<User> users = userService.searchUsers(keyword);
            // 清除敏感信息
            users.forEach(u -> u.setPassword(null));
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("搜索用户失败: {}", e.getMessage());
            return ApiResponse.error("搜索用户失败");
        }
    }
}
