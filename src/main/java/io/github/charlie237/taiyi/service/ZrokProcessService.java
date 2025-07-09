package io.github.charlie237.taiyi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * zrok控制器管理服务
 * 负责太乙控制中心的zrok控制器启动、停止和管理
 * 在SaaS架构中，只管理控制器，不管理具体的隧道进程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZrokProcessService {
    
    @Value("${zrok.binary.path:/usr/local/bin/zrok}")
    private String zrokBinaryPath;
    
    @Value("${zrok.controller.enabled:true}")
    private boolean controllerEnabled;
    
    @Value("${zrok.controller.port:18080}")
    private int controllerPort;
    
    @Value("${zrok.environment.name:taiyi-env}")
    private String environmentName;
    
    private Process controllerProcess;
    
    @PostConstruct
    public void initialize() {
        if (controllerEnabled) {
            startController();
        }
        
        // 检查zrok环境
        checkZrokEnvironment();
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("清理zrok控制器");

        // 停止控制器
        if (controllerProcess != null) {
            stopProcess(controllerProcess);
        }
    }
    
    /**
     * 启动zrok控制器
     */
    public void startController() {
        try {
            if (controllerProcess != null && controllerProcess.isAlive()) {
                log.info("zrok控制器已在运行");
                return;
            }
            
            List<String> command = Arrays.asList(
                    zrokBinaryPath, "controller",
                    "--bind", "0.0.0.0:" + controllerPort
            );
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            controllerProcess = pb.start();
            
            // 启动日志读取线程
            new Thread(() -> readProcessOutput(controllerProcess, "controller")).start();
            
            // 等待控制器启动
            Thread.sleep(3000);
            
            if (controllerProcess.isAlive()) {
                log.info("zrok控制器启动成功，端口: {}", controllerPort);
            } else {
                log.error("zrok控制器启动失败");
            }
            
        } catch (Exception e) {
            log.error("启动zrok控制器失败", e);
        }
    }
    
    /**
     * 获取控制器状态
     */
    public boolean isControllerRunning() {
        return controllerProcess != null && controllerProcess.isAlive();
    }

    /**
     * 获取控制器进程ID
     */
    public long getControllerPid() {
        if (controllerProcess != null && controllerProcess.isAlive()) {
            return controllerProcess.pid();
        }
        return -1;
    }
    
    /**
     * 停止zrok控制器
     */
    public void stopController() {
        if (controllerProcess != null) {
            stopProcess(controllerProcess);
            controllerProcess = null;
            log.info("zrok控制器已停止");
        }
    }

    /**
     * 重启控制器
     */
    public boolean restartController() {
        log.info("重启zrok控制器");
        stopController();
        try {
            Thread.sleep(2000); // 等待2秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startController();
        return isControllerRunning();
    }
    
    /**
     * 检查zrok环境
     */
    private void checkZrokEnvironment() {
        try {
            List<String> command = Arrays.asList(zrokBinaryPath, "status");
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("zrok环境检查通过");
            } else {
                log.warn("zrok环境可能未正确配置，请检查zrok enable状态");
            }
            
        } catch (Exception e) {
            log.error("检查zrok环境失败", e);
        }
    }
    
    /**
     * 停止进程
     */
    private boolean stopProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return true;
        }
        
        try {
            // 优雅停止
            process.destroy();
            
            // 等待进程结束
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                // 强制停止
                process.destroyForcibly();
                finished = process.waitFor(5, TimeUnit.SECONDS);
            }
            
            return finished;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 读取进程输出
     */
    private void readProcessOutput(Process process, String processName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[{}] {}", processName, line);
            }
            
        } catch (IOException e) {
            log.error("读取进程输出失败: {}", processName, e);
        }
    }
    
    /**
     * 生成隧道ID
     */
    private String generateTunnelId(String userId) {
        return "tunnel_" + userId + "_" + System.currentTimeMillis();
    }
    
    /**
     * 获取控制器统计信息
     */
    public Map<String, Object> getControllerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("controllerRunning", isControllerRunning());
        stats.put("controllerPid", getControllerPid());
        stats.put("startTime", System.currentTimeMillis());
        return stats;
    }
    
    /**
     * 检查zrok是否可用
     */
    public boolean isZrokAvailable() {
        try {
            List<String> command = Arrays.asList(zrokBinaryPath, "version");
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
            
        } catch (Exception e) {
            log.error("检查zrok可用性失败", e);
            return false;
        }
    }
}
