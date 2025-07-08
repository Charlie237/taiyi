package io.github.charlie237.taiyi.controller;

import io.github.charlie237.taiyi.common.ApiResponse;
import io.github.charlie237.taiyi.dto.JwtResponse;
import io.github.charlie237.taiyi.dto.LoginRequest;
import io.github.charlie237.taiyi.dto.RegisterRequest;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.security.JwtTokenProvider;
import io.github.charlie237.taiyi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取JWT Token")
    public ApiResponse<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 验证用户凭据
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 生成JWT Token
            String jwt = tokenProvider.generateToken(authentication);
            
            // 获取用户信息
            User user = (User) authentication.getPrincipal();
            
            // 更新最后登录时间
            userService.updateLastLoginTime(user.getUsername());
            
            JwtResponse jwtResponse = new JwtResponse(
                    jwt,
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );
            
            log.info("用户登录成功: {}", user.getUsername());
            return ApiResponse.success("登录成功", jwtResponse);
            
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage());
            return ApiResponse.error("用户名或密码错误");
        }
    }
    
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(registerRequest.getPassword());
            user.setEmail(registerRequest.getEmail());
            user.setPhone(registerRequest.getPhone());
            user.setRealName(registerRequest.getRealName());
            
            userService.register(user);
            
            log.info("用户注册成功: {}", user.getUsername());
            return ApiResponse.success("注册成功");
            
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "刷新JWT Token")
    public ApiResponse<JwtResponse> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String refreshedToken = tokenProvider.refreshToken(token);
            if (refreshedToken != null) {
                String username = tokenProvider.getUsernameFromToken(refreshedToken);
                User user = userService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                
                JwtResponse jwtResponse = new JwtResponse(
                        refreshedToken,
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name()
                );
                
                return ApiResponse.success("Token刷新成功", jwtResponse);
            } else {
                return ApiResponse.error("Token刷新失败");
            }
            
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return ApiResponse.error("Token刷新失败");
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出")
    public ApiResponse<String> logout() {
        SecurityContextHolder.clearContext();
        return ApiResponse.success("登出成功");
    }
}
