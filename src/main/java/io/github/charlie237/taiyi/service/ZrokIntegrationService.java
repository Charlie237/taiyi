package io.github.charlie237.taiyi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * zrok集成服务
 * 负责与zrok引擎的通信和管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZrokIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${zrok.api.base-url:http://localhost:18080}")
    private String zrokApiBaseUrl;
    
    @Value("${zrok.api.token:}")
    private String zrokApiToken;
    
    /**
     * 创建zrok隧道
     */
    public ZrokTunnelResponse createTunnel(String userId, String localAddress, int localPort, String protocol) {
        try {
            String url = zrokApiBaseUrl + "/api/v1/tunnels";
            
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("localAddress", localAddress);
            request.put("localPort", localPort);
            request.put("protocol", protocol.toLowerCase());
            request.put("subdomain", generateSubdomain(userId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!zrokApiToken.isEmpty()) {
                headers.setBearerAuth(zrokApiToken);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<ZrokTunnelResponse> response = restTemplate.postForEntity(
                    url, entity, ZrokTunnelResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("zrok隧道创建成功: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("zrok隧道创建失败: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用zrok API失败", e);
            return null;
        }
    }
    
    /**
     * 删除zrok隧道
     */
    public boolean deleteTunnel(String tunnelId) {
        try {
            String url = zrokApiBaseUrl + "/api/v1/tunnels/" + tunnelId;
            
            HttpHeaders headers = new HttpHeaders();
            if (!zrokApiToken.isEmpty()) {
                headers.setBearerAuth(zrokApiToken);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Void.class);
            
            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("zrok隧道删除{}: {}", success ? "成功" : "失败", tunnelId);
            return success;
            
        } catch (Exception e) {
            log.error("删除zrok隧道失败", e);
            return false;
        }
    }
    
    /**
     * 获取隧道状态
     */
    public ZrokTunnelStatus getTunnelStatus(String tunnelId) {
        try {
            String url = zrokApiBaseUrl + "/api/v1/tunnels/" + tunnelId + "/status";
            
            HttpHeaders headers = new HttpHeaders();
            if (!zrokApiToken.isEmpty()) {
                headers.setBearerAuth(zrokApiToken);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ZrokTunnelStatus> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ZrokTunnelStatus.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            
        } catch (Exception e) {
            log.error("获取zrok隧道状态失败", e);
        }
        return null;
    }
    
    /**
     * 获取隧道流量统计
     */
    public ZrokTrafficStats getTrafficStats(String tunnelId) {
        try {
            String url = zrokApiBaseUrl + "/api/v1/tunnels/" + tunnelId + "/stats";
            
            HttpHeaders headers = new HttpHeaders();
            if (!zrokApiToken.isEmpty()) {
                headers.setBearerAuth(zrokApiToken);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ZrokTrafficStats> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ZrokTrafficStats.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            
        } catch (Exception e) {
            log.error("获取zrok流量统计失败", e);
        }
        return null;
    }
    
    /**
     * 生成子域名
     */
    private String generateSubdomain(String userId) {
        return "user-" + userId + "-" + System.currentTimeMillis();
    }
    
    /**
     * zrok隧道响应
     */
    public static class ZrokTunnelResponse {
        private String tunnelId;
        private String publicUrl;
        private String status;
        private String message;
        
        // getters and setters
        public String getTunnelId() { return tunnelId; }
        public void setTunnelId(String tunnelId) { this.tunnelId = tunnelId; }
        
        public String getPublicUrl() { return publicUrl; }
        public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @Override
        public String toString() {
            return String.format("ZrokTunnelResponse{tunnelId='%s', publicUrl='%s', status='%s'}", 
                    tunnelId, publicUrl, status);
        }
    }
    
    /**
     * zrok隧道状态
     */
    public static class ZrokTunnelStatus {
        private String tunnelId;
        private String status;
        private int connections;
        private long uptime;
        
        // getters and setters
        public String getTunnelId() { return tunnelId; }
        public void setTunnelId(String tunnelId) { this.tunnelId = tunnelId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getConnections() { return connections; }
        public void setConnections(int connections) { this.connections = connections; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
    }
    
    /**
     * zrok流量统计
     */
    public static class ZrokTrafficStats {
        private String tunnelId;
        private long bytesIn;
        private long bytesOut;
        private long requestCount;
        private long lastActivity;
        
        // getters and setters
        public String getTunnelId() { return tunnelId; }
        public void setTunnelId(String tunnelId) { this.tunnelId = tunnelId; }
        
        public long getBytesIn() { return bytesIn; }
        public void setBytesIn(long bytesIn) { this.bytesIn = bytesIn; }
        
        public long getBytesOut() { return bytesOut; }
        public void setBytesOut(long bytesOut) { this.bytesOut = bytesOut; }
        
        public long getRequestCount() { return requestCount; }
        public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
        
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
        
        public long getTotalTraffic() {
            return bytesIn + bytesOut;
        }
    }
}
