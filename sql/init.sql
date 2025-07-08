-- 太乙内网穿透系统数据库初始化脚本

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS taiyi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taiyi;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色',
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 节点表
CREATE TABLE IF NOT EXISTS nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(64) NOT NULL UNIQUE COMMENT '节点ID',
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    description VARCHAR(500) COMMENT '节点描述',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    client_ip VARCHAR(45) COMMENT '客户端IP',
    client_port INT COMMENT '客户端端口',
    server_ip VARCHAR(45) COMMENT '服务器IP',
    server_port INT COMMENT '服务器端口',
    status ENUM('ONLINE', 'OFFLINE', 'ERROR') NOT NULL DEFAULT 'OFFLINE' COMMENT '状态',
    protocol ENUM('TCP', 'UDP', 'HTTP', 'HTTPS') NOT NULL DEFAULT 'TCP' COMMENT '协议',
    max_connections INT DEFAULT 10 COMMENT '最大连接数',
    current_connections INT DEFAULT 0 COMMENT '当前连接数',
    total_bytes_in BIGINT DEFAULT 0 COMMENT '总入流量',
    total_bytes_out BIGINT DEFAULT 0 COMMENT '总出流量',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_node_id (node_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点表';

-- 路由表
CREATE TABLE IF NOT EXISTS routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '路由名称',
    description VARCHAR(500) COMMENT '路由描述',
    node_id BIGINT NOT NULL COMMENT '节点ID',
    local_ip VARCHAR(45) NOT NULL COMMENT '本地IP',
    local_port INT NOT NULL COMMENT '本地端口',
    remote_port INT NOT NULL UNIQUE COMMENT '远程端口',
    protocol ENUM('TCP', 'UDP', 'HTTP', 'HTTPS') NOT NULL DEFAULT 'TCP' COMMENT '协议',
    status ENUM('ACTIVE', 'INACTIVE', 'ERROR') NOT NULL DEFAULT 'INACTIVE' COMMENT '状态',
    max_connections INT DEFAULT 10 COMMENT '最大连接数',
    current_connections INT DEFAULT 0 COMMENT '当前连接数',
    total_bytes_in BIGINT DEFAULT 0 COMMENT '总入流量',
    total_bytes_out BIGINT DEFAULT 0 COMMENT '总出流量',
    bandwidth_limit BIGINT COMMENT '带宽限制(KB/s)',
    compression_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用压缩',
    encryption_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用加密',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_used_at DATETIME COMMENT '最后使用时间',
    FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    INDEX idx_node_id (node_id),
    INDEX idx_remote_port (remote_port),
    INDEX idx_status (status),
    INDEX idx_local_ip_port (local_ip, local_port)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路由表';

-- 节点状态表
CREATE TABLE IF NOT EXISTS node_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '节点ID',
    cpu_usage DOUBLE COMMENT 'CPU使用率',
    memory_usage DOUBLE COMMENT '内存使用率',
    disk_usage DOUBLE COMMENT '磁盘使用率',
    network_in BIGINT COMMENT '网络入流量',
    network_out BIGINT COMMENT '网络出流量',
    connection_count INT COMMENT '连接数',
    uptime BIGINT COMMENT '运行时间',
    load_average DOUBLE COMMENT '系统负载',
    temperature DOUBLE COMMENT '温度',
    ping_latency INT COMMENT '延迟',
    bandwidth_in BIGINT COMMENT '入带宽',
    bandwidth_out BIGINT COMMENT '出带宽',
    error_count INT DEFAULT 0 COMMENT '错误计数',
    warning_count INT DEFAULT 0 COMMENT '警告计数',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    INDEX idx_node_id (node_id),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点状态表';

-- 插入默认管理员用户
INSERT IGNORE INTO users (username, password, email, real_name, role, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@taiyi.com', '系统管理员', 'ADMIN', 'ACTIVE');

SET FOREIGN_KEY_CHECKS = 1;
