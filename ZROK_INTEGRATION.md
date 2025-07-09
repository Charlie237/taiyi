# 太乙内网穿透系统 - zrok集成方案

## 🎯 方案概述

太乙系统采用zrok作为底层穿透引擎，实现高性能、稳定可靠的内网穿透服务。结合商业化Token认证，提供类似V2Board的订阅模式。

## 🏗️ 架构设计

```
用户管理界面 (Web UI)
    ↓
太乙后端系统 (Spring Boot)
    ↓ 进程调用/API
zrok穿透引擎 (Go)
    ↓ 隧道建立
内网客户端 ← → 外网访问
```

## 🚀 快速开始

### 1. 环境准备

#### 安装zrok
```bash
# 下载zrok
wget https://github.com/openziti/zrok/releases/latest/download/zrok_linux_amd64.tar.gz
tar -xzf zrok_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/

# 验证安装
zrok version
```

#### 配置zrok环境
```bash
# 启用zrok环境
zrok enable <your-token>

# 验证配置
zrok status
```

### 2. 启动太乙系统

```bash
# 启动Spring Boot应用
mvn spring-boot:run

# 或使用Docker
docker-compose up -d
```

### 3. 配置文件

在 `application.yml` 中配置zrok：

```yaml
zrok:
  api:
    base-url: http://localhost:18080
    token: ""
  binary:
    path: /usr/local/bin/zrok
  controller:
    enabled: true
    port: 18080
  environment:
    name: taiyi-env
```

## 💳 商业化Token认证

### Token套餐

| 套餐 | 隧道数 | 带宽 | 月流量 | 并发连接 |
|------|--------|------|--------|----------|
| 免费版 | 2 | 1MB/s | 1GB | 10 |
| 基础版 | 5 | 2MB/s | 10GB | 50 |
| 专业版 | 20 | 10MB/s | 100GB | 200 |
| 企业版 | 100 | 50MB/s | 1TB | 1000 |

### API使用示例

#### 1. 创建Token
```bash
curl -X POST http://localhost:8080/api-tokens \
  -H "Authorization: Bearer <jwt-token>" \
  -d "tokenName=我的Token&plan=BASIC"
```

#### 2. 使用Token创建隧道
```bash
curl -X POST http://localhost:8080/api/tunnels/start/1 \
  -H "X-API-Token: taiyi_<your-token>"
```

#### 3. 查看Token统计
```bash
curl -X GET http://localhost:8080/api-tokens/1/stats \
  -H "X-API-Token: taiyi_<your-token>"
```

## 🔧 核心功能

### 1. 隧道管理

```java
// 启动隧道
@PostMapping("/tunnels/start/{routeId}")
public ApiResponse<String> startTunnel(@PathVariable Long routeId) {
    tunnelService.startTunnel(route);
    return ApiResponse.success("隧道启动成功");
}

// 停止隧道
@PostMapping("/tunnels/stop/{routeId}")
public ApiResponse<String> stopTunnel(@PathVariable Long routeId) {
    tunnelService.stopTunnel(routeId);
    return ApiResponse.success("隧道停止成功");
}
```

### 2. Token认证

```java
// Token验证
@Component
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            Optional<ApiToken> apiToken = apiTokenService.validateToken(token);
            // 设置认证信息
        }
        filterChain.doFilter(request, response);
    }
}
```

### 3. 流量监控

```java
// 记录流量使用
@Scheduled(fixedRate = 60000)
public void monitorTraffic() {
    // 获取zrok流量统计
    // 更新用户使用量
    // 执行限制策略
}
```

## 📊 监控和统计

### 系统监控

- **隧道状态监控**：实时监控隧道连接状态
- **流量统计**：记录用户流量使用情况
- **性能指标**：监控系统性能和资源使用

### 用户统计

- **流量使用**：月流量使用统计
- **隧道数量**：当前活跃隧道数
- **连接统计**：并发连接数统计

## 🔒 安全特性

### Token安全

- **加密存储**：Token使用安全哈希存储
- **过期机制**：支持Token过期和自动续期
- **权限控制**：基于套餐的细粒度权限控制

### 网络安全

- **SSL/TLS**：支持HTTPS隧道
- **访问控制**：IP白名单和黑名单
- **审计日志**：完整的操作审计记录

## 🚀 部署指南

### Docker部署

```yaml
version: '3.8'
services:
  taiyi:
    image: taiyi:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ZROK_BINARY_PATH=/usr/local/bin/zrok
    volumes:
      - ./zrok:/usr/local/bin/zrok
      - ./config:/app/config
```

### 生产环境配置

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/taiyi
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

zrok:
  api:
    base-url: http://zrok-controller:18080
  controller:
    enabled: true
    port: 18080
```

## 📈 性能优化

### zrok优化

- **连接池**：配置合适的连接池大小
- **缓存策略**：启用适当的缓存机制
- **负载均衡**：多实例部署和负载均衡

### 系统优化

- **数据库优化**：索引优化和查询优化
- **内存管理**：JVM参数调优
- **监控告警**：完善的监控和告警机制

## 🔧 故障排除

### 常见问题

1. **zrok连接失败**
   ```bash
   # 检查zrok状态
   zrok status
   
   # 重新启用环境
   zrok enable <token>
   ```

2. **隧道创建失败**
   ```bash
   # 检查端口占用
   netstat -tlnp | grep :18080
   
   # 查看日志
   tail -f logs/taiyi.log
   ```

3. **Token认证失败**
   ```bash
   # 验证Token格式
   curl -H "X-API-Token: taiyi_<token>" http://localhost:8080/api-tokens/1/stats
   ```

### 日志分析

```bash
# 查看zrok日志
tail -f ~/.zrok/logs/zrok.log

# 查看太乙系统日志
tail -f logs/taiyi.log | grep -i error
```

## 📞 技术支持

- **文档**：[https://docs.taiyi.com](https://docs.taiyi.com)
- **GitHub**：[https://github.com/charlie237/taiyi](https://github.com/charlie237/taiyi)
- **问题反馈**：[GitHub Issues](https://github.com/charlie237/taiyi/issues)

## 🎉 总结

通过集成zrok，太乙系统实现了：

1. **高性能**：基于Go语言的zrok引擎，性能优秀
2. **稳定可靠**：成熟的穿透技术，生产环境验证
3. **商业化**：完整的Token认证和计费系统
4. **易于使用**：简单的API接口和管理界面
5. **可扩展**：支持多种协议和自定义功能

这个方案既保持了技术的先进性，又确保了开发的高效性，是内网穿透服务的理想选择。
