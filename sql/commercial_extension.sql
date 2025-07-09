-- 商业化功能扩展SQL

-- 服务套餐表
CREATE TABLE IF NOT EXISTS service_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500) COMMENT '套餐描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    duration_days INT NOT NULL COMMENT '有效期（天）',
    max_tunnels INT DEFAULT 5 COMMENT '最大隧道数',
    max_bandwidth BIGINT DEFAULT 1048576 COMMENT '最大带宽(KB/s)',
    max_traffic BIGINT DEFAULT 107374182400 COMMENT '最大流量(bytes/月)',
    max_connections INT DEFAULT 100 COMMENT '最大并发连接数',
    features JSON COMMENT '特性配置',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务套餐表';

-- 用户订阅表
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    api_token VARCHAR(128) NOT NULL UNIQUE COMMENT 'API访问令牌',
    token_secret VARCHAR(64) NOT NULL COMMENT 'Token密钥',
    status ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED') DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    traffic_used BIGINT DEFAULT 0 COMMENT '已使用流量',
    traffic_reset_at DATETIME COMMENT '流量重置时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES service_plans(id),
    INDEX idx_user_id (user_id),
    INDEX idx_api_token (api_token),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅表';

-- 使用统计表
CREATE TABLE IF NOT EXISTS usage_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    date DATE NOT NULL COMMENT '统计日期',
    tunnels_used INT DEFAULT 0 COMMENT '使用的隧道数',
    traffic_in BIGINT DEFAULT 0 COMMENT '入流量',
    traffic_out BIGINT DEFAULT 0 COMMENT '出流量',
    connections_peak INT DEFAULT 0 COMMENT '峰值连接数',
    online_duration INT DEFAULT 0 COMMENT '在线时长(分钟)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id) REFERENCES user_subscriptions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_subscription_date (subscription_id, date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用统计表';

-- 插入默认套餐
INSERT INTO service_plans (name, description, price, duration_days, max_tunnels, max_bandwidth, max_traffic, max_connections) VALUES
('免费版', '基础内网穿透服务', 0.00, 30, 2, 512, 1073741824, 10),
('标准版', '适合个人用户', 29.90, 30, 5, 2048, 10737418240, 50),
('专业版', '适合小团队', 99.90, 30, 20, 10240, 107374182400, 200),
('企业版', '适合企业用户', 299.90, 30, 100, 51200, 1073741824000, 1000);
