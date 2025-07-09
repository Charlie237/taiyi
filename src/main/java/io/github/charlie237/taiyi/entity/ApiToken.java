package io.github.charlie237.taiyi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API Token实体
 * 商业化认证Token，类似V2Board的订阅链接
 */
@Data
@Entity
@Table(name = "api_tokens")
@EqualsAndHashCode(callSuper = false)
public class ApiToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;
    
    @Column(name = "token_name", length = 100)
    private String tokenName;
    
    @Column(name = "token_secret", nullable = false, length = 64)
    private String tokenSecret;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.FREE;
    
    // 权限限制
    @Column(name = "max_tunnels")
    private Integer maxTunnels = 2;
    
    @Column(name = "max_bandwidth")
    private Long maxBandwidth = 1048576L; // 1MB/s
    
    @Column(name = "max_traffic_monthly")
    private Long maxTrafficMonthly = 1073741824L; // 1GB/月
    
    @Column(name = "max_connections")
    private Integer maxConnections = 10;
    
    // 使用统计
    @Column(name = "traffic_used")
    private Long trafficUsed = 0L;
    
    @Column(name = "traffic_reset_at")
    private LocalDateTime trafficResetAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (trafficResetAt == null) {
            trafficResetAt = LocalDateTime.now().plusMonths(1);
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMonths(1);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Status {
        ACTIVE,     // 活跃
        EXPIRED,    // 已过期
        SUSPENDED,  // 已暂停
        REVOKED     // 已撤销
    }
    
    public enum Plan {
        FREE("免费版", 2, 1048576L, 1073741824L, 10),
        BASIC("基础版", 5, 2097152L, 10737418240L, 50),
        PRO("专业版", 20, 10485760L, 107374182400L, 200),
        ENTERPRISE("企业版", 100, 52428800L, 1073741824000L, 1000);
        
        private final String displayName;
        private final int maxTunnels;
        private final long maxBandwidth;
        private final long maxTrafficMonthly;
        private final int maxConnections;
        
        Plan(String displayName, int maxTunnels, long maxBandwidth, 
             long maxTrafficMonthly, int maxConnections) {
            this.displayName = displayName;
            this.maxTunnels = maxTunnels;
            this.maxBandwidth = maxBandwidth;
            this.maxTrafficMonthly = maxTrafficMonthly;
            this.maxConnections = maxConnections;
        }
        
        public String getDisplayName() { return displayName; }
        public int getMaxTunnels() { return maxTunnels; }
        public long getMaxBandwidth() { return maxBandwidth; }
        public long getMaxTrafficMonthly() { return maxTrafficMonthly; }
        public int getMaxConnections() { return maxConnections; }
    }
    
    /**
     * 检查Token是否有效
     */
    public boolean isValid() {
        return status == Status.ACTIVE && 
               (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
    
    /**
     * 检查是否超出流量限制
     */
    public boolean isTrafficExceeded() {
        return trafficUsed >= maxTrafficMonthly;
    }
    
    /**
     * 获取剩余流量（字节）
     */
    public long getRemainingTraffic() {
        return Math.max(0, maxTrafficMonthly - trafficUsed);
    }
    
    /**
     * 获取流量使用率
     */
    public double getTrafficUsageRate() {
        if (maxTrafficMonthly == 0) return 0.0;
        return (double) trafficUsed / maxTrafficMonthly;
    }
    
    /**
     * 检查是否需要重置流量
     */
    public boolean needsTrafficReset() {
        return trafficResetAt != null && 
               LocalDateTime.now().isAfter(trafficResetAt);
    }
    
    /**
     * 重置月流量
     */
    public void resetMonthlyTraffic() {
        this.trafficUsed = 0L;
        this.trafficResetAt = LocalDateTime.now().plusMonths(1);
    }
    
    /**
     * 增加使用流量
     */
    public void addTrafficUsage(long bytes) {
        this.trafficUsed += bytes;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    /**
     * 检查是否可以创建更多隧道
     */
    public boolean canCreateMoreTunnels(int currentTunnels) {
        return currentTunnels < maxTunnels;
    }
    
    /**
     * 应用套餐限制
     */
    public void applyPlanLimits() {
        if (plan != null) {
            this.maxTunnels = plan.getMaxTunnels();
            this.maxBandwidth = plan.getMaxBandwidth();
            this.maxTrafficMonthly = plan.getMaxTrafficMonthly();
            this.maxConnections = plan.getMaxConnections();
        }
    }
    
    /**
     * 获取流量限制描述
     */
    public String getTrafficLimitDescription() {
        double gb = maxTrafficMonthly / (1024.0 * 1024.0 * 1024.0);
        return String.format("%.1fGB/月", gb);
    }
    
    /**
     * 获取带宽限制描述
     */
    public String getBandwidthLimitDescription() {
        double mb = maxBandwidth / (1024.0 * 1024.0);
        return String.format("%.1fMB/s", mb);
    }
}
