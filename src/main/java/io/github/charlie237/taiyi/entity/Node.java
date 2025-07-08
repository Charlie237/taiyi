package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 内网客户端节点实体类
 */
@Data
@Entity
@Table(name = "client_nodes")
@EqualsAndHashCode(callSuper = false)
public class Node {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "node_id", unique = true, nullable = false, length = 64)
    private String nodeId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "client_ip", length = 45)
    private String clientIp;
    
    @Column(name = "client_port")
    private Integer clientPort;
    
    @Column(name = "server_ip", length = 45)
    private String serverIp;
    
    @Column(name = "server_port")
    private Integer serverPort;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OFFLINE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Protocol protocol = Protocol.TCP;
    
    @Column(name = "max_connections")
    private Integer maxConnections = 10;
    
    @Column(name = "current_connections")
    private Integer currentConnections = 0;
    
    @Column(name = "total_bytes_in")
    private Long totalBytesIn = 0L;
    
    @Column(name = "total_bytes_out")
    private Long totalBytesOut = 0L;
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
     * 节点状态枚举
     */
    public enum Status {
        ONLINE,   /** 在线 */
        OFFLINE,  /** 离线 */
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
