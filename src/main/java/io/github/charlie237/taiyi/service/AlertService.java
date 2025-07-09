package io.github.charlie237.taiyi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警服务
 * 负责系统告警的发送和管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    
    @Value("${alert.enabled:true}")
    private boolean alertEnabled;
    
    @Value("${alert.cooldown.minutes:5}")
    private int alertCooldownMinutes;
    
    // 告警冷却时间管理，防止重复告警
    private final Map<String, LocalDateTime> alertCooldowns = new ConcurrentHashMap<>();
    
    /**
     * 发送告警
     */
    public void sendAlert(String nodeId, String alertType, String level, String message) {
        if (!alertEnabled) {
            return;
        }
        
        String alertKey = nodeId + ":" + alertType + ":" + level;
        
        // 检查冷却时间
        if (isInCooldown(alertKey)) {
            log.debug("告警在冷却期内，跳过: {}", alertKey);
            return;
        }
        
        // 记录冷却时间
        alertCooldowns.put(alertKey, LocalDateTime.now());
        
        // 发送告警
        try {
            String alertMessage = formatAlertMessage(nodeId, alertType, level, message);
            
            // 根据告警级别选择不同的处理方式
            switch (level.toUpperCase()) {
                case "CRITICAL":
                    sendCriticalAlert(alertMessage);
                    break;
                case "WARNING":
                    sendWarningAlert(alertMessage);
                    break;
                case "INFO":
                    sendInfoAlert(alertMessage);
                    break;
                default:
                    log.warn("未知告警级别: {}", level);
            }
            
            log.info("发送告警: {} - {} - {}", nodeId, alertType, message);
            
        } catch (Exception e) {
            log.error("发送告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否在冷却期内
     */
    private boolean isInCooldown(String alertKey) {
        LocalDateTime lastAlert = alertCooldowns.get(alertKey);
        if (lastAlert == null) {
            return false;
        }
        
        LocalDateTime cooldownEnd = lastAlert.plusMinutes(alertCooldownMinutes);
        boolean inCooldown = LocalDateTime.now().isBefore(cooldownEnd);
        
        // 如果冷却期已过，清理记录
        if (!inCooldown) {
            alertCooldowns.remove(alertKey);
        }
        
        return inCooldown;
    }
    
    /**
     * 格式化告警消息
     */
    private String formatAlertMessage(String nodeId, String alertType, String level, String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        return String.format("[%s] [%s] 节点: %s, 类型: %s, 消息: %s", 
                timestamp, level, nodeId, alertType, message);
    }
    
    /**
     * 发送严重告警
     */
    private void sendCriticalAlert(String message) {
        // 严重告警 - 可以集成邮件、短信、钉钉等
        log.error("🚨 严重告警: {}", message);
        
        // TODO: 集成邮件发送
        // emailService.sendAlert(message);
        
        // TODO: 集成短信发送
        // smsService.sendAlert(message);
        
        // TODO: 集成钉钉机器人
        // dingTalkService.sendAlert(message);
        
        // TODO: 集成Webhook
        // webhookService.sendAlert(message);
    }
    
    /**
     * 发送警告告警
     */
    private void sendWarningAlert(String message) {
        // 警告告警 - 通常只记录日志和发送邮件
        log.warn("⚠️ 警告告警: {}", message);
        
        // TODO: 集成邮件发送
        // emailService.sendWarning(message);
    }
    
    /**
     * 发送信息告警
     */
    private void sendInfoAlert(String message) {
        // 信息告警 - 只记录日志
        log.info("ℹ️ 信息告警: {}", message);
    }
    
    /**
     * 发送节点离线告警
     */
    public void sendNodeOfflineAlert(String nodeId, String nodeName) {
        String message = String.format("节点 %s (%s) 已离线", nodeName, nodeId);
        sendAlert(nodeId, "NODE_STATUS", "CRITICAL", message);
    }
    
    /**
     * 发送节点上线通知
     */
    public void sendNodeOnlineAlert(String nodeId, String nodeName) {
        String message = String.format("节点 %s (%s) 已上线", nodeName, nodeId);
        sendAlert(nodeId, "NODE_STATUS", "INFO", message);
    }
    
    /**
     * 发送隧道异常告警
     */
    public void sendTunnelErrorAlert(String nodeId, Long routeId, String error) {
        String message = String.format("隧道异常 (路由ID: %d): %s", routeId, error);
        sendAlert(nodeId, "TUNNEL_ERROR", "WARNING", message);
    }
    
    /**
     * 发送流量超限告警
     */
    public void sendTrafficLimitAlert(String nodeId, String tokenName, long usedTraffic, long maxTraffic) {
        double usagePercent = (double) usedTraffic / maxTraffic * 100;
        String message = String.format("Token %s 流量使用率达到 %.1f%% (%d/%d bytes)", 
                tokenName, usagePercent, usedTraffic, maxTraffic);
        sendAlert(nodeId, "TRAFFIC_LIMIT", "WARNING", message);
    }
    
    /**
     * 发送系统资源告警
     */
    public void sendSystemResourceAlert(String alertType, String level, String message) {
        sendAlert("SYSTEM", alertType, level, message);
    }
    
    /**
     * 清理过期的冷却记录
     */
    public void cleanupExpiredCooldowns() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(alertCooldownMinutes * 2);
        
        alertCooldowns.entrySet().removeIf(entry -> 
                entry.getValue().isBefore(cutoffTime));
        
        log.debug("清理过期告警冷却记录");
    }
    
    /**
     * 获取当前冷却中的告警数量
     */
    public int getActiveCooldownCount() {
        return alertCooldowns.size();
    }
    
    /**
     * 测试告警功能
     */
    public void sendTestAlert() {
        sendAlert("TEST_NODE", "TEST", "INFO", "这是一个测试告警消息");
    }
}
