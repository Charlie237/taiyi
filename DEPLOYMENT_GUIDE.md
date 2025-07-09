# 太乙内网穿透系统 - 部署指南

## 🎯 部署概述

太乙内网穿透系统基于zrok集成方案，提供高性能、稳定可靠的内网穿透服务。本指南将详细介绍系统的部署过程。

## 📋 系统要求

### 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核+ |
| 内存 | 4GB | 8GB+ |
| 存储 | 20GB | 50GB+ |
| 网络 | 10Mbps | 100Mbps+ |

### 软件要求

- **操作系统**：Linux (Ubuntu 20.04+, CentOS 8+)
- **Java**：OpenJDK 17+
- **数据库**：MySQL 8.0+
- **zrok**：最新版本
- **Docker**：20.10+ (可选)

## 🚀 快速部署

### 方式一：Docker部署（推荐）

#### 1. 准备Docker环境

```bash
# 安装Docker和Docker Compose
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
sudo systemctl enable docker
sudo systemctl start docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

#### 2. 创建部署目录

```bash
mkdir -p /opt/taiyi
cd /opt/taiyi
```

#### 3. 创建docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: taiyi-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ILYabc123
      MYSQL_DATABASE: taiyi
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    restart: unless-stopped

  taiyi:
    image: taiyi:latest
    container_name: taiyi-app
    depends_on:
      - mysql
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/taiyi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ILYabc123
      ZROK_BINARY_PATH: /usr/local/bin/zrok
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
      - zrok_data:/root/.zrok
    ports:
      - "8080:8080"
      - "18080:18080"
      - "10000-20000:10000-20000"
    restart: unless-stopped

volumes:
  mysql_data:
  zrok_data:
```

#### 4. 启动服务

```bash
# 下载SQL初始化文件
mkdir -p sql
curl -o sql/init.sql https://raw.githubusercontent.com/charlie237/taiyi/main/sql/init.sql

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f taiyi
```

### 方式二：手动部署

#### 1. 安装Java环境

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# 验证安装
java -version
```

#### 2. 安装MySQL

```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# 启动MySQL
sudo systemctl enable mysql
sudo systemctl start mysql

# 安全配置
sudo mysql_secure_installation
```

#### 3. 安装zrok

```bash
# 下载zrok
wget https://github.com/openziti/zrok/releases/latest/download/zrok_linux_amd64.tar.gz
tar -xzf zrok_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/
sudo chmod +x /usr/local/bin/zrok

# 验证安装
zrok version
```

#### 4. 配置数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库和用户
CREATE DATABASE taiyi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'taiyi'@'localhost' IDENTIFIED BY 'ILYabc123';
GRANT ALL PRIVILEGES ON taiyi.* TO 'taiyi'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入初始化脚本
mysql -u taiyi -p taiyi < sql/init.sql
```

#### 5. 部署应用

```bash
# 创建应用目录
sudo mkdir -p /opt/taiyi
cd /opt/taiyi

# 下载应用JAR包
wget https://github.com/charlie237/taiyi/releases/latest/download/taiyi.jar

# 创建配置文件
cat > application-prod.yml << EOF
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taiyi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: taiyi
    password: ILYabc123
    driver-class-name: com.mysql.cj.jdbc.Driver

zrok:
  api:
    base-url: http://localhost:18080
  binary:
    path: /usr/local/bin/zrok
  controller:
    enabled: true
    port: 18080

logging:
  level:
    io.github.charlie237.taiyi: INFO
  file:
    name: /opt/taiyi/logs/taiyi.log
EOF

# 创建systemd服务
sudo cat > /etc/systemd/system/taiyi.service << EOF
[Unit]
Description=Taiyi Tunnel Service
After=network.target mysql.service

[Service]
Type=simple
User=taiyi
WorkingDirectory=/opt/taiyi
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod taiyi.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 创建用户和设置权限
sudo useradd -r -s /bin/false taiyi
sudo chown -R taiyi:taiyi /opt/taiyi

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable taiyi
sudo systemctl start taiyi
```

## 🔧 配置说明

### 核心配置项

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taiyi
    username: taiyi
    password: your_password

zrok:
  api:
    base-url: http://localhost:18080
    token: ""  # zrok API token (可选)
  binary:
    path: /usr/local/bin/zrok
  controller:
    enabled: true
    port: 18080
    auto-start: true
  environment:
    name: taiyi-env
    auto-enable: true

taiyi:
  jwt:
    secret: your-secret-key
    expiration: PT24H
  tunnel:
    port-range:
      start: 10000
      end: 20000
  security:
    cors-enabled: true
    allowed-origins: ["*"]
```

### 环境变量

```bash
# 数据库配置
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/taiyi"
export SPRING_DATASOURCE_USERNAME="taiyi"
export SPRING_DATASOURCE_PASSWORD="your_password"

# zrok配置
export ZROK_BINARY_PATH="/usr/local/bin/zrok"
export ZROK_API_BASE_URL="http://localhost:18080"

# 安全配置
export TAIYI_JWT_SECRET="your-secret-key"
```

## 🔒 安全配置

### 1. 防火墙设置

```bash
# Ubuntu/Debian (ufw)
sudo ufw allow 8080/tcp    # 太乙Web服务
sudo ufw allow 18080/tcp   # zrok控制器
sudo ufw allow 10000:20000/tcp  # 隧道端口范围

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=18080/tcp
sudo firewall-cmd --permanent --add-port=10000-20000/tcp
sudo firewall-cmd --reload
```

### 2. SSL/TLS配置

```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your_password
    key-store-type: PKCS12
    key-alias: taiyi
  port: 8443
```

### 3. 反向代理配置（Nginx）

```nginx
# /etc/nginx/sites-available/taiyi
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

## 📊 监控和日志

### 1. 日志配置

```yaml
# logback-spring.xml
logging:
  level:
    io.github.charlie237.taiyi: INFO
    org.springframework.security: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /opt/taiyi/logs/taiyi.log
    max-size: 100MB
    max-history: 30
```

### 2. 监控配置

```yaml
# Prometheus监控
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

## 🔄 维护和升级

### 备份数据

```bash
# 数据库备份
mysqldump -u taiyi -p taiyi > backup_$(date +%Y%m%d_%H%M%S).sql

# 配置文件备份
tar -czf config_backup_$(date +%Y%m%d_%H%M%S).tar.gz /opt/taiyi/config
```

### 升级应用

```bash
# 停止服务
sudo systemctl stop taiyi

# 备份当前版本
cp taiyi.jar taiyi.jar.backup

# 下载新版本
wget https://github.com/charlie237/taiyi/releases/latest/download/taiyi.jar

# 启动服务
sudo systemctl start taiyi

# 检查状态
sudo systemctl status taiyi
```

## 🚨 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 检查日志
   sudo journalctl -u taiyi -f
   
   # 检查端口占用
   sudo netstat -tlnp | grep 8080
   ```

2. **数据库连接失败**
   ```bash
   # 测试数据库连接
   mysql -u taiyi -p -h localhost taiyi
   
   # 检查MySQL状态
   sudo systemctl status mysql
   ```

3. **zrok服务异常**
   ```bash
   # 检查zrok状态
   zrok status
   
   # 重新启用zrok环境
   zrok enable <token>
   ```

### 性能调优

```bash
# JVM参数优化
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 数据库连接池优化
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

## 📞 技术支持

- **文档**：https://docs.taiyi.com
- **GitHub**：https://github.com/charlie237/taiyi
- **问题反馈**：https://github.com/charlie237/taiyi/issues
- **社区讨论**：https://github.com/charlie237/taiyi/discussions
