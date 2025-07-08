package io.github.charlie237.taiyi.task;

import io.github.charlie237.taiyi.service.NodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {
    
    private final NodeService nodeService;
    
    /**
     * 清理离线节点
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupOfflineNodes() {
        try {
            log.debug("开始清理离线节点");
            nodeService.cleanupOfflineNodes();
            log.debug("离线节点清理完成");
        } catch (Exception e) {
            log.error("清理离线节点失败: {}", e.getMessage());
        }
    }
}
