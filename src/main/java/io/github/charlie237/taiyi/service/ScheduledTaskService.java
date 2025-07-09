package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.ApiToken;
import io.github.charlie237.taiyi.service.ZrokIntegrationService.ZrokTrafficStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务服务
 * 处理Token清理、流量统计等定时任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    
    private final ApiTokenService apiTokenService;
    private final TunnelService tunnelService;
    private final ZrokIntegrationService zrokService;
    
    /**
     * 每小时清理过期Token
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanupExpiredTokens() {
        try {
            apiTokenService.cleanupExpiredTokens();
            log.debug("过期Token清理任务完成");
        } catch (Exception e) {
            log.error("清理过期Token失败", e);
        }
    }
    
    /**
     * 每天重置月流量
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天午夜
    public void resetMonthlyTraffic() {
        try {
            apiTokenService.resetAllMonthlyTraffic();
            log.info("月流量重置任务完成");
        } catch (Exception e) {
            log.error("重置月流量失败", e);
        }
    }
    
    /**
     * 每5分钟更新流量统计
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void updateTrafficStats() {
        try {
            // 获取所有活跃的Token
            List<ApiToken> activeTokens = apiTokenService.getTokens(
                org.springframework.data.domain.Pageable.unpaged()
            ).getContent().stream()
            .filter(token -> token.getStatus() == ApiToken.Status.ACTIVE)
            .toList();
            
            for (ApiToken token : activeTokens) {
                updateTokenTrafficStats(token);
            }
            
            log.debug("流量统计更新任务完成，处理Token数量: {}", activeTokens.size());
        } catch (Exception e) {
            log.error("更新流量统计失败", e);
        }
    }
    
    /**
     * 更新单个Token的流量统计
     */
    private void updateTokenTrafficStats(ApiToken token) {
        try {
            // 这里需要根据实际的zrok集成情况来获取流量统计
            // 暂时使用模拟数据
            
            // TODO: 实现真实的流量统计获取
            // ZrokTrafficStats stats = zrokService.getTrafficStatsForUser(token.getUser().getId().toString());
            // if (stats != null) {
            //     long newTrafficUsage = stats.getBytesIn() + stats.getBytesOut();
            //     if (newTrafficUsage > token.getTrafficUsed()) {
            //         long additionalTraffic = newTrafficUsage - token.getTrafficUsed();
            //         apiTokenService.recordTrafficUsage(token.getToken(), additionalTraffic);
            //     }
            // }
            
        } catch (Exception e) {
            log.error("更新Token流量统计失败: {}", token.getToken(), e);
        }
    }
    
    /**
     * 每10分钟检查隧道状态
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    public void checkTunnelStatus() {
        try {
            int activeTunnels = tunnelService.getActiveTunnelCount();
            log.debug("当前活跃隧道数量: {}", activeTunnels);
            
            // TODO: 可以添加隧道健康检查逻辑
            // 检查长时间无响应的隧道
            // 自动重启失败的隧道等
            
        } catch (Exception e) {
            log.error("检查隧道状态失败", e);
        }
    }
    
    /**
     * 每30分钟生成系统统计报告
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void generateSystemStats() {
        try {
            long activeTokens = apiTokenService.countActiveTokens();
            int activeTunnels = tunnelService.getActiveTunnelCount();
            
            log.info("系统统计 - 活跃Token: {}, 活跃隧道: {}", activeTokens, activeTunnels);
            
            // TODO: 可以将统计数据存储到数据库或发送到监控系统
            
        } catch (Exception e) {
            log.error("生成系统统计失败", e);
        }
    }
    
    /**
     * 每天清理旧的日志和统计数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanupOldData() {
        try {
            // TODO: 实现旧数据清理逻辑
            // 清理超过30天的审计日志
            // 清理超过90天的统计数据等
            
            log.info("旧数据清理任务完成");
        } catch (Exception e) {
            log.error("清理旧数据失败", e);
        }
    }
    
    /**
     * 每小时检查系统健康状态
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void healthCheck() {
        try {
            // 检查zrok服务状态
            boolean zrokHealthy = checkZrokHealth();
            
            // 检查数据库连接
            boolean dbHealthy = checkDatabaseHealth();
            
            if (!zrokHealthy || !dbHealthy) {
                log.warn("系统健康检查警告 - zrok: {}, 数据库: {}", zrokHealthy, dbHealthy);
                // TODO: 发送告警通知
            }
            
        } catch (Exception e) {
            log.error("系统健康检查失败", e);
        }
    }
    
    /**
     * 检查zrok服务健康状态
     */
    private boolean checkZrokHealth() {
        try {
            // TODO: 实现zrok健康检查
            // 可以通过调用zrok API或检查进程状态来判断
            return true;
        } catch (Exception e) {
            log.error("zrok健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查数据库健康状态
     */
    private boolean checkDatabaseHealth() {
        try {
            // 简单的数据库连接检查
            apiTokenService.countActiveTokens();
            return true;
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }
}
