package io.github.charlie237.taiyi.common;

/**
 * 系统常量定义
 */
public class Constants {
    
    /**
     * JWT相关常量
     */
    public static class JWT {
        public static final String TOKEN_PREFIX = "Bearer ";
        public static final String HEADER_STRING = "Authorization";
        public static final String AUTHORITIES_KEY = "authorities";
        public static final String USER_ID_KEY = "userId";
        public static final String USERNAME_KEY = "username";
    }
    
    /**
     * 缓存相关常量
     */
    public static class Cache {
        public static final String USER_CACHE = "user";
        public static final String NODE_CACHE = "node";
        public static final String ROUTE_CACHE = "route";
        public static final int DEFAULT_EXPIRE_TIME = 3600; // 1小时
    }
    
    /**
     * 隧道相关常量
     */
    public static class Tunnel {
        public static final int DEFAULT_PORT_START = 10000;
        public static final int DEFAULT_PORT_END = 20000;
        public static final int MAX_CONNECTIONS = 1000;
        public static final int HEARTBEAT_INTERVAL = 30; // 秒
        public static final int CONNECTION_TIMEOUT = 60; // 秒
        public static final int READ_TIMEOUT = 30; // 秒
        public static final int WRITE_TIMEOUT = 30; // 秒
    }
    
    /**
     * WebSocket相关常量
     */
    public static class WebSocket {
        public static final String ENDPOINT = "/ws";
        public static final String NODE_ENDPOINT = "/ws/node";
        public static final String ADMIN_ENDPOINT = "/ws/admin";
        public static final String HEARTBEAT_TOPIC = "/topic/heartbeat";
        public static final String STATUS_TOPIC = "/topic/status";
        public static final String MESSAGE_TOPIC = "/topic/message";
    }
    
    /**
     * 消息类型常量
     */
    public static class MessageType {
        public static final String HEARTBEAT = "heartbeat";
        public static final String STATUS_UPDATE = "status_update";
        public static final String ROUTE_CONFIG = "route_config";
        public static final String TUNNEL_START = "tunnel_start";
        public static final String TUNNEL_STOP = "tunnel_stop";
        public static final String ERROR = "error";
        public static final String INFO = "info";
    }
    
    /**
     * 系统配置常量
     */
    public static class System {
        public static final String DEFAULT_ADMIN_USERNAME = "admin";
        public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
        public static final String DEFAULT_ADMIN_EMAIL = "admin@taiyi.com";
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final int LOGIN_LOCK_TIME = 300; // 5分钟
        public static final int PASSWORD_MIN_LENGTH = 6;
        public static final int PASSWORD_MAX_LENGTH = 20;
    }
    
    /**
     * 文件相关常量
     */
    public static class File {
        public static final String UPLOAD_DIR = "uploads";
        public static final String LOG_DIR = "logs";
        public static final String CONFIG_DIR = "config";
        public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
        public static final String[] ALLOWED_EXTENSIONS = {".txt", ".log", ".conf", ".json", ".xml", ".yml", ".yaml"};
    }
    
    /**
     * 正则表达式常量
     */
    public static class Regex {
        public static final String EMAIL = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        public static final String PHONE = "^1[3-9]\\d{9}$";
        public static final String IP_ADDRESS = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        public static final String PORT = "^([1-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
        public static final String USERNAME = "^[a-zA-Z0-9_]{3,20}$";
        public static final String NODE_ID = "^[a-zA-Z0-9_-]{8,64}$";
    }
    
    /**
     * 错误码常量
     */
    public static class ErrorCode {
        public static final int SUCCESS = 200;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
        
        // 业务错误码
        public static final int USER_NOT_FOUND = 1001;
        public static final int USER_ALREADY_EXISTS = 1002;
        public static final int INVALID_PASSWORD = 1003;
        public static final int ACCOUNT_LOCKED = 1004;
        
        public static final int NODE_NOT_FOUND = 2001;
        public static final int NODE_ALREADY_EXISTS = 2002;
        public static final int NODE_OFFLINE = 2003;
        public static final int NODE_LIMIT_EXCEEDED = 2004;
        
        public static final int ROUTE_NOT_FOUND = 3001;
        public static final int ROUTE_ALREADY_EXISTS = 3002;
        public static final int PORT_ALREADY_USED = 3003;
        public static final int ROUTE_LIMIT_EXCEEDED = 3004;
    }
}
