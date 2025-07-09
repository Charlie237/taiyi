package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务套餐实体
 */
@Data
@Entity
@Table(name = "service_plans")
@EqualsAndHashCode(callSuper = false)
public class ServicePlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;
    
    @Column(name = "max_tunnels")
    private Integer maxTunnels = 5;
    
    @Column(name = "max_bandwidth")
    private Long maxBandwidth = 1048576L; // 1MB/s
    
    @Column(name = "max_traffic")
    private Long maxTraffic = 107374182400L; // 100GB/月
    
    @Column(name = "max_connections")
    private Integer maxConnections = 100;
    
    @Column(columnDefinition = "JSON")
    private String features;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    
    @Column(name = "created_at", nullable = false, updatable = false)
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
    
    public enum Status {
        ACTIVE, INACTIVE
    }
    
    /**
     * 检查是否为免费套餐
     */
    public boolean isFree() {
        return price.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 获取流量限制（GB）
     */
    public double getTrafficLimitGB() {
        return maxTraffic / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * 获取带宽限制（MB/s）
     */
    public double getBandwidthLimitMB() {
        return maxBandwidth / (1024.0 * 1024.0);
    }
}
