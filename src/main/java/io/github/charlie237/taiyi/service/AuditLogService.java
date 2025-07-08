package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.AuditLog;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * 记录审计日志（异步）
     */
    @Async
    public void logAsync(User user, String action, AuditLog.OperationType operationType, 
                        String resourceType, Long resourceId, AuditLog.OperationResult result) {
        try {
            AuditLog auditLog = createAuditLog(user, action, operationType, resourceType, resourceId, result);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }
    
    /**
     * 记录登录日志
     */
    public void logLogin(User user, boolean success, String errorMessage) {
        AuditLog auditLog = createAuditLog(user, "用户登录", AuditLog.OperationType.LOGIN, 
                "USER", user != null ? user.getId() : null, 
                success ? AuditLog.OperationResult.SUCCESS : AuditLog.OperationResult.FAILURE);
        
        if (!success && errorMessage != null) {
            auditLog.setErrorMessage(errorMessage);
        }
        
        auditLogRepository.save(auditLog);
    }
    
    /**
     * 创建审计日志对象
     */
    private AuditLog createAuditLog(User user, String action, AuditLog.OperationType operationType, 
                                   String resourceType, Long resourceId, AuditLog.OperationResult result) {
        AuditLog auditLog = new AuditLog();
        
        if (user != null) {
            auditLog.setUserId(user.getId());
            auditLog.setUsername(user.getUsername());
        }
        
        auditLog.setAction(action);
        auditLog.setOperationType(operationType);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResult(result);
        
        // 获取请求信息
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUri(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }
        
        return auditLog;
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 查询审计日志
     */
    public Page<AuditLog> findAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
