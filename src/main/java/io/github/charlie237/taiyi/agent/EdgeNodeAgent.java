package io.github.charlie237.taiyi.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 太乙边缘节点监控Agent
 * 部署在zrok边缘服务器上，负责收集系统信息并上报给太乙控制中心
 */
@Slf4j
public class EdgeNodeAgent {
    
    private final String controlCenterUrl;
    private final String nodeId;
    private final String nodeName;
    private final String authToken;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    // 系统信息
    private final OperatingSystemMXBean osBean;
    private final Runtime runtime;
    
    // 网络统计
    private long lastNetworkIn = 0;
    private long lastNetworkOut = 0;
    private long lastCheckTime = System.currentTimeMillis();
    
    public EdgeNodeAgent(String controlCenterUrl, String nodeId, String nodeName, String authToken) {
        this.controlCenterUrl = controlCenterUrl;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.authToken = authToken;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtime = Runtime.getRuntime();
    }
    
    /**
     * 启动监控Agent
     */
    public void start() {
        log.info("启动太乙边缘节点监控Agent: {} ({})", nodeName, nodeId);
        
        // 注册节点
        registerNode();
        
        // 启动心跳任务（每30秒）
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 30, TimeUnit.SECONDS);
        
        // 启动监控数据上报任务（每60秒）
        scheduler.scheduleAtFixedRate(this::reportMonitoringData, 30, 60, TimeUnit.SECONDS);
        
        log.info("边缘节点监控Agent启动完成");
    }
    
    /**
     * 停止监控Agent
     */
    public void stop() {
        log.info("停止边缘节点监控Agent: {}", nodeId);
        
        // 注销节点
        unregisterNode();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("边缘节点监控Agent已停止");
    }
    
    /**
     * 注册节点到控制中心
     */
    private void registerNode() {
        try {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("nodeId", nodeId);
            nodeInfo.put("nodeName", nodeName);
            nodeInfo.put("serverIp", getPublicIpAddress());
            nodeInfo.put("osName", System.getProperty("os.name"));
            nodeInfo.put("osVersion", System.getProperty("os.version"));
            nodeInfo.put("osArch", System.getProperty("os.arch"));
            nodeInfo.put("javaVersion", System.getProperty("java.version"));
            nodeInfo.put("availableProcessors", osBean.getAvailableProcessors());
            
            sendToControlCenter("/api/nodes/register", nodeInfo);
            log.info("节点注册成功: {}", nodeId);
            
        } catch (Exception e) {
            log.error("节点注册失败", e);
        }
    }
    
    /**
     * 注销节点
     */
    private void unregisterNode() {
        try {
            Map<String, Object> data = Map.of("nodeId", nodeId);
            sendToControlCenter("/api/nodes/unregister", data);
            log.info("节点注销成功: {}", nodeId);
        } catch (Exception e) {
            log.error("节点注销失败", e);
        }
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        try {
            Map<String, Object> heartbeat = Map.of(
                    "nodeId", nodeId,
                    "timestamp", System.currentTimeMillis(),
                    "status", "online"
            );
            
            sendToControlCenter("/api/nodes/heartbeat", heartbeat);
            log.debug("发送心跳: {}", nodeId);
            
        } catch (Exception e) {
            log.error("发送心跳失败", e);
        }
    }
    
    /**
     * 上报监控数据
     */
    private void reportMonitoringData() {
        try {
            Map<String, Object> monitoringData = collectMonitoringData();
            sendToControlCenter("/api/node-monitoring/" + nodeId + "/status", monitoringData);
            
            log.debug("上报监控数据: {} - CPU: {}%, 内存: {}%, 磁盘: {}%", 
                    nodeId, 
                    monitoringData.get("cpuUsage"), 
                    monitoringData.get("memoryUsage"), 
                    monitoringData.get("diskUsage"));
                    
        } catch (Exception e) {
            log.error("上报监控数据失败", e);
        }
    }
    
    /**
     * 收集监控数据
     */
    private Map<String, Object> collectMonitoringData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // CPU使用率
            data.put("cpuUsage", getCpuUsage());
            
            // 内存使用率
            data.put("memoryUsage", getMemoryUsage());
            data.put("totalMemory", runtime.totalMemory());
            data.put("freeMemory", runtime.freeMemory());
            data.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            
            // 磁盘使用率
            data.put("diskUsage", getDiskUsage());
            data.put("totalDiskSpace", getTotalDiskSpace());
            data.put("freeDiskSpace", getFreeDiskSpace());
            
            // 网络信息
            Map<String, Long> networkInfo = getNetworkInfo();
            data.put("networkIn", networkInfo.get("in"));
            data.put("networkOut", networkInfo.get("out"));
            data.put("bandwidthIn", networkInfo.get("bandwidthIn"));
            data.put("bandwidthOut", networkInfo.get("bandwidthOut"));
            
            // 系统负载
            data.put("loadAverage", osBean.getSystemLoadAverage());
            
            // 运行时间
            data.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
            
            // zrok相关信息
            data.put("zrokStatus", getZrokStatus());
            data.put("tunnelCount", getTunnelCount());
            data.put("connectionCount", getConnectionCount());
            
            // 时间戳
            data.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("收集监控数据失败", e);
        }
        
        return data;
    }
    
    /**
     * 获取CPU使用率
     */
    private double getCpuUsage() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("linux")) {
                // Linux系统使用top命令
                Process process = Runtime.getRuntime().exec("top -bn1 | grep 'Cpu(s)'");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if (line != null && line.contains("%")) {
                    // 解析CPU使用率
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        if (part.contains("id")) {
                            String idleStr = part.trim().split("%")[0].trim();
                            double idle = Double.parseDouble(idleStr);
                            return 100.0 - idle;
                        }
                    }
                }
            } else if (osName.contains("windows")) {
                // Windows系统使用wmic
                Process process = Runtime.getRuntime().exec("wmic cpu get loadpercentage /value");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("LoadPercentage=")) {
                        return Double.parseDouble(line.split("=")[1]);
                    }
                }
            }
            
            // 备用方案：使用系统负载
            double load = osBean.getSystemLoadAverage();
            if (load >= 0) {
                return Math.min(load / osBean.getAvailableProcessors() * 100, 100.0);
            }
            
        } catch (Exception e) {
            log.debug("获取CPU使用率失败", e);
        }
        return 0.0;
    }
    
    /**
     * 获取内存使用率
     */
    private double getMemoryUsage() {
        try {
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            if (totalMemory > 0) {
                return (double) (totalMemory - freeMemory) / totalMemory * 100;
            }
        } catch (Exception e) {
            log.error("获取内存使用率失败", e);
        }
        return 0.0;
    }
    
    /**
     * 获取磁盘使用率
     */
    private double getDiskUsage() {
        try {
            long totalSpace = getTotalDiskSpace();
            long freeSpace = getFreeDiskSpace();
            if (totalSpace > 0) {
                return (double) (totalSpace - freeSpace) / totalSpace * 100;
            }
        } catch (Exception e) {
            log.error("获取磁盘使用率失败", e);
        }
        return 0.0;
    }
    
    private long getTotalDiskSpace() {
        try {
            FileStore store = Files.getFileStore(FileSystems.getDefault().getPath("."));
            return store.getTotalSpace();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private long getFreeDiskSpace() {
        try {
            FileStore store = Files.getFileStore(FileSystems.getDefault().getPath("."));
            return store.getUsableSpace();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 获取网络信息
     */
    private Map<String, Long> getNetworkInfo() {
        Map<String, Long> networkInfo = new HashMap<>();
        
        try {
            // 简化实现，实际应该读取系统网络统计
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastCheckTime;
            
            // 这里应该实现真实的网络统计读取
            long currentNetworkIn = getCurrentNetworkBytes("rx");
            long currentNetworkOut = getCurrentNetworkBytes("tx");
            
            // 计算带宽
            long bandwidthIn = 0;
            long bandwidthOut = 0;
            
            if (timeDiff > 0 && lastNetworkIn > 0) {
                bandwidthIn = (currentNetworkIn - lastNetworkIn) * 1000 / timeDiff;
                bandwidthOut = (currentNetworkOut - lastNetworkOut) * 1000 / timeDiff;
            }
            
            networkInfo.put("in", currentNetworkIn);
            networkInfo.put("out", currentNetworkOut);
            networkInfo.put("bandwidthIn", Math.max(0, bandwidthIn));
            networkInfo.put("bandwidthOut", Math.max(0, bandwidthOut));
            
            lastNetworkIn = currentNetworkIn;
            lastNetworkOut = currentNetworkOut;
            lastCheckTime = currentTime;
            
        } catch (Exception e) {
            log.error("获取网络信息失败", e);
            networkInfo.put("in", 0L);
            networkInfo.put("out", 0L);
            networkInfo.put("bandwidthIn", 0L);
            networkInfo.put("bandwidthOut", 0L);
        }
        
        return networkInfo;
    }
    
    private long getCurrentNetworkBytes(String direction) {
        // 实际实现应该读取 /proc/net/dev (Linux) 或使用系统API
        return System.currentTimeMillis() % 1000000; // 模拟数据
    }
    
    /**
     * 获取zrok状态
     */
    private String getZrokStatus() {
        try {
            // 检查zrok进程是否运行
            Process process = Runtime.getRuntime().exec("pgrep zrok");
            process.waitFor();
            return process.exitValue() == 0 ? "running" : "stopped";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取隧道数量
     */
    private int getTunnelCount() {
        // 实际实现应该通过zrok API获取
        return 0;
    }
    
    /**
     * 获取连接数量
     */
    private int getConnectionCount() {
        // 实际实现应该通过zrok API获取
        return 0;
    }
    
    /**
     * 获取公网IP地址
     */
    private String getPublicIpAddress() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (Exception e) {
            log.error("获取公网IP失败", e);
            return "unknown";
        }
    }
    
    /**
     * 发送数据到控制中心
     */
    private void sendToControlCenter(String endpoint, Object data) throws Exception {
        String json = objectMapper.writeValueAsString(data);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(controlCenterUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        String controlCenterUrl = System.getProperty("taiyi.control.center.url", "http://localhost:8080");
        String nodeId = System.getProperty("taiyi.node.id", "edge_" + System.currentTimeMillis());
        String nodeName = System.getProperty("taiyi.node.name", nodeId);
        String authToken = System.getProperty("taiyi.auth.token", "");
        
        if (authToken.isEmpty()) {
            log.error("必须提供认证Token: -Dtaiyi.auth.token=your_token");
            System.exit(1);
        }
        
        log.info("太乙边缘节点监控Agent配置:");
        log.info("  控制中心地址: {}", controlCenterUrl);
        log.info("  节点ID: {}", nodeId);
        log.info("  节点名称: {}", nodeName);
        
        EdgeNodeAgent agent = new EdgeNodeAgent(controlCenterUrl, nodeId, nodeName, authToken);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(agent::stop));
        
        // 启动Agent
        agent.start();
        
        // 保持运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("Agent被中断");
            agent.stop();
        }
    }
}
