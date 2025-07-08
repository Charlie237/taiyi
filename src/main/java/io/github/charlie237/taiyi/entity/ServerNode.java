package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公网服务器节点实体类
 */
@Data
@Entity
@Table(name = "server_nodes")
@EqualsAndHashCode(callSuper = false)
public class ServerNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "server_id", unique = true, nullable = false, length = 64)
    private String serverId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "public_ip", nullable = false, length = 45)
    private String publicIp;
    
    @Column(name = "public_port")
    private Integer publicPort;
    
    @Column(name = "region", length = 50)
    private String region; // 地理区域：如 "华东", "华北", "美西"
    
    @Column(name = "isp", length = 50)
    private String isp; // 运营商：如 "阿里云", "腾讯云", "AWS"
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OFFLINE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType = NodeType.RELAY;
    
    @Column(name = "max_bandwidth")
    private Long maxBandwidth; // 最大带宽 (Mbps)
    
    @Column(name = "current_bandwidth")
    private Long currentBandwidth = 0L; // 当前使用带宽
    
    @Column(name = "max_connections")
    private Integer maxConnections = 1000;
    
    @Column(name = "current_connections")
    private Integer currentConnections = 0;
    
    @Column(name = "total_bytes_in")
    private Long totalBytesIn = 0L;
    
    @Column(name = "total_bytes_out")
    private Long totalBytesOut = 0L;
    
    @Column(name = "cpu_usage")
    private Double cpuUsage = 0.0; // CPU使用率
    
    @Column(name = "memory_usage")
    private Double memoryUsage = 0.0; // 内存使用率
    
    @Column(name = "disk_usage")
    private Double diskUsage = 0.0; // 磁盘使用率
    
    @Column(name = "network_latency")
    private Integer networkLatency = 0; // 网络延迟 (ms)
    
    @Column(name = "load_score")
    private Double loadScore = 0.0; // 负载评分 (0-100)
    
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
     * 服务器节点状态枚举
     */
    public enum Status {
        ONLINE,     /** 在线 */
        OFFLINE,    /** 离线 */
        BUSY,       /** 繁忙 */
        MAINTENANCE /** 维护中 */
    }

    /**
     * 节点类型枚举
     */
    public enum NodeType {
        RELAY,      /** 中继节点 */
        EDGE,       /** 边缘节点 */
        GATEWAY,    /** 网关节点 */
        BACKUP      /** 备份节点 */
    }
}
