# 太乙内网穿透系统

<div align="center">

![太乙Logo](https://img.shields.io/badge/太乙-内网穿透-blue?style=for-the-badge)
[![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge)](https://spring.io/projects/spring-boot)

**一个基于zrok集成的现代化内网穿透解决方案，提供商业化Token认证和完整的管理功能**

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [API文档](API_GUIDE.md) • [部署指南](DEPLOYMENT_GUIDE.md) • [zrok集成](ZROK_INTEGRATION.md)

</div>

## 🎯 项目概述

太乙内网穿透系统是一个企业级的内网穿透解决方案，基于成熟的zrok引擎，提供高性能、稳定可靠的穿透服务。系统采用商业化Token认证模式，类似V2Board的订阅管理，支持多种套餐和精确的流量控制。

### 🏗️ 系统架构

```
用户管理界面 (Vue.js/React)
    ↓
太乙后端系统 (Spring Boot 3.x)
    ↓ 进程调用/API
zrok穿透引擎 (Go)
    ↓ 隧道建立
内网客户端 ← → 外网访问
```

## ✨ 功能特性

### 🚀 核心功能
- **高性能穿透**：基于zrok引擎，支持TCP/UDP/HTTP/HTTPS协议
- **商业化认证**：API Token认证，支持多种套餐和权限控制
- **实时监控**：WebSocket实时通信，完整的流量统计和监控
- **用户管理**：JWT认证，角色权限控制，用户注册登录
- **隧道管理**：动态创建/停止隧道，支持多节点管理

### 💳 商业化特性
- **套餐管理**：免费版、基础版、专业版、企业版四种套餐
- **流量控制**：精确的月流量统计和限制
- **权限限制**：隧道数量、带宽、并发连接数限制
- **自动计费**：流量自动重置，套餐到期管理

### 🛡️ 安全特性
- **Token安全**：加密存储，支持撤销和过期管理
- **访问控制**：基于角色的权限控制
- **审计日志**：完整的操作审计记录
- **SSL支持**：支持HTTPS隧道和SSL证书管理

## 🚀 快速开始

### 📋 环境要求

| 组件 | 版本要求 |
|------|----------|
| Java | 17+ |
| MySQL | 8.0+ |
| zrok | 最新版 |
| Maven | 3.6+ |

## 📊 SaaS监控架构

太乙采用现代SaaS架构，支持管理多个分布式边缘节点。

### 系统架构
```
太乙控制中心 (官网)
├── 用户管理、套餐管理
├── zrok控制器 (核心)
├── 节点管理面板
└── 监控数据收集
    ↑ HTTP API上报
    │ 监控数据 + 管理指令
zrok边缘节点1 (公网服务器)
├── zrok边缘服务
├── 太乙监控Agent
└── 系统资源监控
zrok边缘节点2 (公网服务器)
├── zrok边缘服务
├── 太乙监控Agent
└── 系统资源监控
```

### 部署边缘节点

#### 自动部署脚本（推荐）
```bash
# 下载部署脚本
wget https://raw.githubusercontent.com/charlie237/taiyi/main/deploy-edge-node.sh
chmod +x deploy-edge-node.sh

# 部署边缘节点
sudo ./deploy-edge-node.sh \
  -c https://your-taiyi-control-center.com \
  -t your_auth_token \
  -N "北京节点"
```

#### 手动部署
```bash
# 1. 安装依赖
sudo apt update
sudo apt install -y openjdk-17-jre-headless

# 2. 安装zrok
wget https://github.com/openziti/zrok/releases/latest/download/zrok_linux_amd64.tar.gz
tar -xzf zrok_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/

# 3. 下载并启动太乙Agent
java -jar taiyi-edge-agent.jar \
  -Dtaiyi.control.center.url=https://your-control-center.com \
  -Dtaiyi.node.id=edge-node-001 \
  -Dtaiyi.auth.token=your_token
```

### 监控数据
边缘节点会自动采集并上报以下信息：
- **系统资源** - CPU、内存、磁盘使用率
- **网络流量** - 入站/出站流量和带宽
- **zrok状态** - zrok服务运行状态
- **隧道信息** - 隧道数量和连接统计
- **系统信息** - 操作系统、运行时间等

### 管理API
```bash
# 节点管理
GET /api/nodes                    # 获取所有节点
GET /api/nodes/{nodeId}           # 获取节点详情
POST /api/nodes/{nodeId}/enable   # 启用节点
POST /api/nodes/{nodeId}/disable  # 禁用节点

# 监控数据
GET /api/node-monitoring/{nodeId}/latest    # 最新状态
GET /api/node-monitoring/{nodeId}/stats/24h # 24小时统计
GET /api/nodes/stats                        # 节点整体统计
```

### 🐳 Docker部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/charlie237/taiyi.git
cd taiyi

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f taiyi
```

### 🔧 手动部署

```bash
# 1. 安装zrok
wget https://github.com/openziti/zrok/releases/latest/download/zrok_linux_amd64.tar.gz
tar -xzf zrok_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/

# 2. 配置数据库
mysql -u root -p -e "CREATE DATABASE taiyi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p taiyi < sql/init.sql

# 3. 启动应用
mvn spring-boot:run
```

### 🌐 访问系统

- **Web管理界面**: http://localhost:8080
- **API文档**: http://localhost:8080/api/swagger-ui.html
- **Token管理**: http://localhost:8080/token-management.html
- **默认管理员**: admin / admin (首次登录后请修改密码)

## 💡 使用示例

### 创建API Token

```bash
# 1. 用户登录获取JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'

# 2. 创建API Token
curl -X POST http://localhost:8080/api/api-tokens \
  -H "Authorization: Bearer <jwt-token>" \
  -d "tokenName=我的Token&plan=BASIC"
```

### 使用Token管理隧道

```bash
# 启动隧道
curl -X POST http://localhost:8080/api/tunnels/start/1 \
  -H "X-API-Token: taiyi_<your-token>"

# 查看隧道状态
curl -X GET http://localhost:8080/api/tunnels/status/1 \
  -H "X-API-Token: taiyi_<your-token>"
```

## 🛠️ 技术栈

### 后端技术
- **框架**: Spring Boot 3.x, Spring Security 6.x
- **数据库**: MySQL 8.0, Spring Data JPA
- **认证**: JWT Token, API Token
- **通信**: WebSocket, RESTful API
- **监控**: Micrometer, Prometheus, Actuator

### 穿透引擎
- **核心引擎**: zrok (Go语言实现)
- **协议支持**: TCP, UDP, HTTP, HTTPS
- **网络架构**: OpenZiti零信任网络

### 前端技术
- **界面**: Bootstrap 5, HTML5, JavaScript
- **图表**: Chart.js
- **图标**: Bootstrap Icons

## 📚 文档导航

- [📖 API使用指南](API_GUIDE.md) - 详细的API使用说明和示例
- [🚀 部署指南](DEPLOYMENT_GUIDE.md) - 生产环境部署配置
- [🔧 zrok集成说明](ZROK_INTEGRATION.md) - zrok集成技术方案

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 如何贡献

1. **Fork** 本仓库
2. **创建** 功能分支 (`git checkout -b feature/AmazingFeature`)
3. **提交** 更改 (`git commit -m 'Add some AmazingFeature'`)
4. **推送** 到分支 (`git push origin feature/AmazingFeature`)
5. **创建** Pull Request

### 开发规范

- 遵循Java代码规范
- 添加适当的单元测试
- 更新相关文档
- 确保CI/CD通过

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

- [zrok](https://github.com/openziti/zrok) - 优秀的穿透引擎
- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的Java框架
- [OpenZiti](https://openziti.github.io/) - 零信任网络架构

## 📞 联系我们

- **GitHub Issues**: [提交问题](https://github.com/Charlie237/taiyi/issues)
- **GitHub Discussions**: [社区讨论](https://github.com/Charlie237/taiyi/discussions)

---

<div align="center">

**如果这个项目对您有帮助，请给我们一个 ⭐ Star！**

Made with ❤️ by [Charlie237](https://github.com/Charlie237)

</div>
