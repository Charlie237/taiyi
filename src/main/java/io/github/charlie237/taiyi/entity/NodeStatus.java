package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 节点状态记录实体类
 */
@Data
@Entity
@Table(name = "node_status")
@EqualsAndHashCode(callSuper = false)
public class NodeStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;
    
    @Column(name = "cpu_usage")
    private Double cpuUsage; // CPU使用率 (0-100)
    
    @Column(name = "memory_usage")
    private Double memoryUsage; // 内存使用率 (0-100)
    
    @Column(name = "disk_usage")
    private Double diskUsage; // 磁盘使用率 (0-100)
    
    @Column(name = "network_in")
    private Long networkIn; // 网络入流量 (bytes)
    
    @Column(name = "network_out")
    private Long networkOut; // 网络出流量 (bytes)
    
    @Column(name = "connection_count")
    private Integer connectionCount; // 当前连接数
    
    @Column(name = "uptime")
    private Long uptime; // 运行时间 (秒)
    
    @Column(name = "load_average")
    private Double loadAverage; // 系统负载
    
    @Column(name = "temperature")
    private Double temperature; // 温度 (摄氏度)
    
    @Column(name = "ping_latency")
    private Integer pingLatency; // 延迟 (毫秒)
    
    @Column(name = "bandwidth_in")
    private Long bandwidthIn; // 入带宽 (bps)
    
    @Column(name = "bandwidth_out")
    private Long bandwidthOut; // 出带宽 (bps)
    
    @Column(name = "error_count")
    private Integer errorCount = 0; // 错误计数
    
    @Column(name = "warning_count")
    private Integer warningCount = 0; // 警告计数
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
