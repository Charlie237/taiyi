package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 */
@Data
@Entity
@Table(name = "audit_logs")
@EqualsAndHashCode(callSuper = false)
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username", length = 50)
    private String username;
    
    @Column(name = "action", nullable = false, length = 100)
    private String action;
    
    @Column(name = "resource_type", length = 50)
    private String resourceType;
    
    @Column(name = "resource_id")
    private Long resourceId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "request_uri", length = 500)
    private String requestUri;
    
    @Column(name = "request_method", length = 10)
    private String requestMethod;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "execution_time")
    private Long executionTime; // 执行时间（毫秒）
    
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private OperationResult result;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        LOGIN,          /** 登录 */
        LOGOUT,         /** 登出 */
        CREATE,         /** 创建 */
        UPDATE,         /** 更新 */
        DELETE,         /** 删除 */
        VIEW,           /** 查看 */
        EXPORT,         /** 导出 */
        IMPORT,         /** 导入 */
        CONNECT,        /** 连接 */
        DISCONNECT,     /** 断开连接 */
        START,          /** 启动 */
        STOP,           /** 停止 */
        CONFIG_CHANGE   /** 配置变更 */
    }
    
    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS,    /** 成功 */
        FAILURE,    /** 失败 */
        PARTIAL     /** 部分成功 */
    }
}
