package io.github.charlie237.taiyi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控指标配置
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 用户登录计数器
     */
    @Bean
    public Counter userLoginCounter() {
        return Counter.builder("taiyi.user.login.total")
                .description("Total user login attempts")
                .register(meterRegistry);
    }
    
    /**
     * 节点连接计数器
     */
    @Bean
    public Counter nodeConnectionCounter() {
        return Counter.builder("taiyi.node.connection.total")
                .description("Total node connections")
                .register(meterRegistry);
    }
    
    /**
     * 隧道创建计数器
     */
    @Bean
    public Counter tunnelCreationCounter() {
        return Counter.builder("taiyi.tunnel.creation.total")
                .description("Total tunnel creations")
                .register(meterRegistry);
    }
    
    /**
     * API请求计时器
     */
    @Bean
    public Timer apiRequestTimer() {
        return Timer.builder("taiyi.api.request.duration")
                .description("API request duration")
                .register(meterRegistry);
    }
    
    /**
     * 数据传输量计数器
     */
    @Bean
    public Counter dataTransferCounter() {
        return Counter.builder("taiyi.data.transfer.bytes")
                .description("Total data transfer in bytes")
                .register(meterRegistry);
    }
    
    /**
     * 活跃连接数量计量器
     */
    @Bean
    public Gauge activeConnectionsGauge() {
        return Gauge.builder("taiyi.connections.active", this, MetricsConfig::getActiveConnections)
                .description("Number of active connections")
                .register(meterRegistry);
    }

    /**
     * 在线节点数量计量器
     */
    @Bean
    public Gauge onlineNodesGauge() {
        return Gauge.builder("taiyi.nodes.online", this, MetricsConfig::getOnlineNodes)
                .description("Number of online nodes")
                .register(meterRegistry);
    }
    
    /**
     * 获取活跃连接数（示例实现）
     */
    private static double getActiveConnections(MetricsConfig config) {
        // 这里应该从实际的连接管理器获取数据
        return 0.0;
    }
    
    /**
     * 获取在线节点数（示例实现）
     */
    private static double getOnlineNodes(MetricsConfig config) {
        // 这里应该从节点服务获取数据
        return 0.0;
    }
}
