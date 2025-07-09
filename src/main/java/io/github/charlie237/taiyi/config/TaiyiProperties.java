package io.github.charlie237.taiyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 太乙系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "taiyi")
public class TaiyiProperties {
    
    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();
    
    /**
     * 隧道配置
     */
    private Tunnel tunnel = new Tunnel();
    
    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();
    
    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();
    
    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * zrok集成配置
     */
    private Zrok zrok = new Zrok();
    
    @Data
    public static class Jwt {
        private String secret = "taiyi-default-secret-key";
        private Duration expiration = Duration.ofHours(24);
        private Duration refreshExpiration = Duration.ofDays(7);
    }
    
    @Data
    public static class Tunnel {
        private PortRange portRange = new PortRange();
        private int maxConnections = 1000;
        private Duration heartbeatInterval = Duration.ofSeconds(30);
        private Duration connectionTimeout = Duration.ofSeconds(60);
        private boolean compressionEnabled = false;
        private boolean encryptionEnabled = false;
        
        @Data
        public static class PortRange {
            private int start = 10000;
            private int end = 20000;
        }
    }
    
    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private Api api = new Api();
        private Login login = new Login();
        private WebSocket webSocket = new WebSocket();
        
        @Data
        public static class Api {
            private int capacity = 100;
            private Duration refillPeriod = Duration.ofMinutes(1);
        }
        
        @Data
        public static class Login {
            private int capacity = 5;
            private Duration refillPeriod = Duration.ofMinutes(1);
            private Duration lockDuration = Duration.ofMinutes(15);
        }
        
        @Data
        public static class WebSocket {
            private int capacity = 10;
            private Duration refillPeriod = Duration.ofMinutes(1);
        }
    }
    
    @Data
    public static class Monitoring {
        private boolean enabled = true;
        private boolean metricsEnabled = true;
        private boolean healthCheckEnabled = true;
        private Duration metricsInterval = Duration.ofSeconds(30);
    }
    
    @Data
    public static class Security {
        private boolean corsEnabled = true;
        private String[] allowedOrigins = {"*"};
        private boolean csrfEnabled = false;
        private Password password = new Password();
        
        @Data
        public static class Password {
            private int minLength = 6;
            private int maxLength = 20;
            private boolean requireUppercase = false;
            private boolean requireLowercase = false;
            private boolean requireDigits = true;
            private boolean requireSpecialChars = false;
        }
    }

    @Data
    public static class Zrok {
        private Api api = new Api();
        private Binary binary = new Binary();
        private Controller controller = new Controller();
        private Environment environment = new Environment();

        @Data
        public static class Api {
            private String baseUrl = "http://localhost:18080";
            private String token = "";
            private int timeout = 30000; // 30秒超时
        }

        @Data
        public static class Binary {
            private String path = "/usr/local/bin/zrok";
            private boolean autoDownload = false;
            private String version = "latest";
        }

        @Data
        public static class Controller {
            private boolean enabled = true;
            private int port = 18080;
            private String bindAddress = "0.0.0.0";
            private boolean autoStart = true;
        }

        @Data
        public static class Environment {
            private String name = "taiyi-env";
            private boolean autoEnable = true;
            private String configPath = "~/.zrok";
        }
    }
}
