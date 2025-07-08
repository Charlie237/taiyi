# 太乙内网穿透系统

基于Spring Boot 3.x开发的内网穿透服务后端系统，提供安全、稳定、高效的内网穿透解决方案。

## 🚀 项目特性

- **用户管理**：完整的用户注册、登录、权限管理体系
- **节点管理**：内网节点注册、状态监控、配置管理
- **路由管理**：端口映射配置、流量转发规则、路由策略管理
- **实时通信**：基于WebSocket的节点实时通信
- **安全认证**：JWT Token认证，Spring Security安全框架
- **API文档**：集成Swagger UI，提供完整的API文档
- **监控统计**：系统运行状态监控和统计信息

## 🛠 技术栈

- **框架**：Spring Boot 3.5.3
- **数据库**：MySQL 8.0+
- **安全**：Spring Security + JWT
- **持久化**：Spring Data JPA + Hibernate
- **通信**：WebSocket
- **缓存**：Caffeine
- **文档**：Swagger/OpenAPI 3
- **监控**：Spring Boot Actuator
- **构建工具**：Maven 3.6+
- **Java版本**：JDK 17+

## 📋 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用      │    │   内网客户端    │    │   外网用户      │
│   (管理界面)    │    │   (节点程序)    │    │   (访问者)      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │ HTTP/HTTPS           │ WebSocket            │ TCP/UDP
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    太乙内网穿透服务                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 用户管理    │  │ 节点管理    │  │ 路由管理    │              │
│  │ 模块        │  │ 模块        │  │ 模块        │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 认证授权    │  │ WebSocket   │  │ 隧道管理    │              │
│  │ 模块        │  │ 通信模块    │  │ 模块        │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   MySQL数据库   │
                    └─────────────────┘
```

## 🗄 数据库设计

### 核心表结构

#### 用户表 (users)
- id: 主键
- username: 用户名
- password: 密码(加密)
- email: 邮箱
- role: 角色(USER/ADMIN)
- status: 状态(ACTIVE/INACTIVE/BANNED)
- created_at: 创建时间

#### 节点表 (nodes)
- id: 主键
- node_id: 节点唯一标识
- name: 节点名称
- user_id: 所属用户
- client_ip: 客户端IP
- status: 状态(ONLINE/OFFLINE/ERROR)
- last_heartbeat: 最后心跳时间

#### 路由表 (routes)
- id: 主键
- node_id: 所属节点
- name: 路由名称
- local_ip: 本地IP
- local_port: 本地端口
- remote_port: 远程端口
- protocol: 协议(TCP/UDP/HTTP/HTTPS)
- status: 状态(ACTIVE/INACTIVE/ERROR)

## 🔧 开发环境搭建

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- IDE (推荐IntelliJ IDEA)

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/charlie237/taiyi.git
cd taiyi
```

2. **配置数据库**
```sql
CREATE DATABASE taiyi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'taiyi'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON taiyi.* TO 'taiyi'@'localhost';
FLUSH PRIVILEGES;
```

3. **修改配置文件**
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taiyi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: taiyi
    password: 123456
```

4. **编译运行**
```bash
mvn clean compile
mvn spring-boot:run
```

5. **访问应用**
- 应用地址：http://localhost:8080/api
- API文档：http://localhost:8080/api/swagger-ui.html
- 健康检查：http://localhost:8080/api/actuator/health

### 默认账户

- 管理员账户：admin / admin123

## 📚 API接口文档

### 认证接口

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "realName": "新用户"
}
```

### 节点管理接口

#### 获取节点列表
```http
GET /api/nodes
Authorization: Bearer {token}
```

#### 注册节点
```http
POST /api/nodes
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "我的节点",
  "description": "节点描述",
  "protocol": "TCP",
  "maxConnections": 10
}
```

### 路由管理接口

#### 创建路由
```http
POST /api/routes
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Web服务",
  "description": "内网Web服务映射",
  "node": {"id": 1},
  "localIp": "192.168.1.100",
  "localPort": 80,
  "protocol": "HTTP"
}
```

## 🔌 WebSocket通信协议

### 连接地址
```
ws://localhost:8080/api/ws/node?nodeId={nodeId}
```

### 消息格式
```json
{
  "type": "heartbeat|status_update|tunnel_start|tunnel_stop",
  "message": "消息描述",
  "data": {},
  "timestamp": 1640995200000
}
```

### 心跳消息
```json
{
  "type": "heartbeat",
  "message": "心跳",
  "data": {},
  "timestamp": 1640995200000
}
```

### 状态更新
```json
{
  "type": "status_update",
  "message": "状态更新",
  "data": {
    "connections": 5,
    "bytesIn": 1024,
    "bytesOut": 2048
  },
  "timestamp": 1640995200000
}
```

## 🧪 测试

### 运行单元测试
```bash
mvn test
```

### 运行集成测试
```bash
mvn verify
```

## 📦 部署配置

### 生产环境配置

1. **修改生产配置**
```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://prod-db:3306/taiyi
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}

logging:
  level:
    io.github.charlie237.taiyi: INFO
```

2. **构建JAR包**
```bash
mvn clean package -Pprod
```

3. **运行应用**
```bash
java -jar target/taiyi-0.0.1-SNAPSHOT.jar
```

### Docker部署
```dockerfile
FROM openjdk:17-jre-slim
COPY target/taiyi-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 作者：Charlie237
- 邮箱：charlie237@example.com
- 项目地址：https://github.com/charlie237/taiyi

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！
