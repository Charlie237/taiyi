package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 路由配置实体类
 */
@Data
@Entity
@Table(name = "routes")
@EqualsAndHashCode(callSuper = false)
public class Route {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;
    
    @Column(name = "local_ip", nullable = false, length = 45)
    private String localIp;
    
    @Column(name = "local_port", nullable = false)
    private Integer localPort;
    
    @Column(name = "remote_port", nullable = false)
    private Integer remotePort;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Protocol protocol = Protocol.TCP;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.INACTIVE;
    
    @Column(name = "max_connections")
    private Integer maxConnections = 10;
    
    @Column(name = "current_connections")
    private Integer currentConnections = 0;
    
    @Column(name = "total_bytes_in")
    private Long totalBytesIn = 0L;
    
    @Column(name = "total_bytes_out")
    private Long totalBytesOut = 0L;
    
    @Column(name = "bandwidth_limit")
    private Long bandwidthLimit; // KB/s
    
    @Column(name = "compression_enabled")
    private Boolean compressionEnabled = false;
    
    @Column(name = "encryption_enabled")
    private Boolean encryptionEnabled = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 路由状态枚举
     */
    public enum Status {
        ACTIVE,   /** 激活 */
        INACTIVE, /** 未激活 */
        ERROR     /** 错误 */
    }

    /**
     * 协议类型枚举
     */
    public enum Protocol {
        TCP,
        UDP,
        HTTP,
        HTTPS
    }
}
