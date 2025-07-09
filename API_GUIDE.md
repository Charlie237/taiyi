# 太乙内网穿透系统 - API使用指南

## 🎯 概述

太乙内网穿透系统提供了完整的RESTful API，支持JWT认证和API Token认证两种方式。本指南将详细介绍如何使用这些API。

## 🔐 认证方式

### 1. JWT认证（用户登录）

```bash
# 用户登录获取JWT Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'

# 响应示例
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### 2. API Token认证（程序化访问）

```bash
# 使用API Token访问
curl -X GET http://localhost:8080/api/tunnels \
  -H "X-API-Token: taiyi_your_api_token"

# 或使用Authorization Header
curl -X GET http://localhost:8080/api/tunnels \
  -H "Authorization: Bearer taiyi_your_api_token"
```

## 🎫 API Token管理

### 创建API Token

```bash
curl -X POST http://localhost:8080/api/api-tokens \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "tokenName=我的API Token&plan=BASIC"
```

### 查看Token列表

```bash
curl -X GET http://localhost:8080/api/api-tokens \
  -H "Authorization: Bearer <jwt-token>"
```

### 查看Token统计

```bash
curl -X GET http://localhost:8080/api/api-tokens/{id}/stats \
  -H "Authorization: Bearer <jwt-token>"
```

### 升级Token套餐

```bash
curl -X POST http://localhost:8080/api/api-tokens/{id}/upgrade \
  -H "Authorization: Bearer <jwt-token>" \
  -d "newPlan=PRO"
```

### 撤销Token

```bash
curl -X POST http://localhost:8080/api/api-tokens/{id}/revoke \
  -H "Authorization: Bearer <jwt-token>"
```

## 🚇 隧道管理API

### 启动隧道

```bash
curl -X POST http://localhost:8080/api/tunnels/start/{routeId} \
  -H "X-API-Token: taiyi_your_api_token"
```

### 停止隧道

```bash
curl -X POST http://localhost:8080/api/tunnels/stop/{routeId} \
  -H "X-API-Token: taiyi_your_api_token"
```

### 查看隧道状态

```bash
curl -X GET http://localhost:8080/api/tunnels/status/{routeId} \
  -H "X-API-Token: taiyi_your_api_token"
```

### 获取隧道统计

```bash
curl -X GET http://localhost:8080/api/tunnels/stats \
  -H "X-API-Token: taiyi_your_api_token"
```

## 🖥️ 节点管理API

### 获取节点列表

```bash
curl -X GET http://localhost:8080/api/nodes \
  -H "X-API-Token: taiyi_your_api_token"
```

### 创建节点

```bash
curl -X POST http://localhost:8080/api/nodes \
  -H "X-API-Token: taiyi_your_api_token" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的节点",
    "description": "测试节点",
    "location": "北京"
  }'
```

### 激活/停用节点

```bash
# 激活节点
curl -X POST http://localhost:8080/api/nodes/{id}/activate \
  -H "X-API-Token: taiyi_your_api_token"

# 停用节点
curl -X POST http://localhost:8080/api/nodes/{id}/deactivate \
  -H "X-API-Token: taiyi_your_api_token"
```

## 🛣️ 路由管理API

### 获取路由列表

```bash
curl -X GET http://localhost:8080/api/routes \
  -H "X-API-Token: taiyi_your_api_token"
```

### 创建路由

```bash
curl -X POST http://localhost:8080/api/routes \
  -H "X-API-Token: taiyi_your_api_token" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web服务路由",
    "nodeId": 1,
    "localIp": "127.0.0.1",
    "localPort": 8080,
    "remotePort": 9080,
    "protocol": "TCP",
    "description": "本地Web服务"
  }'
```

### 激活/停用路由

```bash
# 激活路由
curl -X POST http://localhost:8080/api/routes/{id}/activate \
  -H "X-API-Token: taiyi_your_api_token"

# 停用路由
curl -X POST http://localhost:8080/api/routes/{id}/deactivate \
  -H "X-API-Token: taiyi_your_api_token"
```

## 📊 监控和统计API

### 系统统计

```bash
curl -X GET http://localhost:8080/api/dashboard/stats \
  -H "X-API-Token: taiyi_your_api_token"
```

### 最近活动

```bash
curl -X GET http://localhost:8080/api/dashboard/recent-activities \
  -H "X-API-Token: taiyi_your_api_token"
```

### 健康检查

```bash
curl -X GET http://localhost:8080/api/monitoring/health \
  -H "X-API-Token: taiyi_your_api_token"
```

## 🔧 错误处理

### 标准错误响应格式

```json
{
  "success": false,
  "message": "错误描述",
  "code": "ERROR_CODE",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### 常见错误码

| 错误码 | 描述 | 解决方案 |
|--------|------|----------|
| `INVALID_TOKEN` | Token无效或已过期 | 检查Token格式，重新获取Token |
| `INSUFFICIENT_PERMISSIONS` | 权限不足 | 检查Token套餐和权限设置 |
| `QUOTA_EXCEEDED` | 配额超限 | 升级套餐或等待配额重置 |
| `TUNNEL_LIMIT_REACHED` | 隧道数量达到上限 | 停止不需要的隧道或升级套餐 |
| `TRAFFIC_LIMIT_EXCEEDED` | 流量超限 | 等待流量重置或升级套餐 |

## 📝 使用示例

### Python示例

```python
import requests

class TaiyiClient:
    def __init__(self, base_url, api_token):
        self.base_url = base_url
        self.headers = {
            'X-API-Token': api_token,
            'Content-Type': 'application/json'
        }
    
    def start_tunnel(self, route_id):
        url = f"{self.base_url}/api/tunnels/start/{route_id}"
        response = requests.post(url, headers=self.headers)
        return response.json()
    
    def get_tunnel_stats(self):
        url = f"{self.base_url}/api/tunnels/stats"
        response = requests.get(url, headers=self.headers)
        return response.json()

# 使用示例
client = TaiyiClient('http://localhost:8080', 'taiyi_your_api_token')
result = client.start_tunnel(1)
print(result)
```

### JavaScript示例

```javascript
class TaiyiClient {
    constructor(baseUrl, apiToken) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
    }
    
    async startTunnel(routeId) {
        const response = await fetch(`${this.baseUrl}/api/tunnels/start/${routeId}`, {
            method: 'POST',
            headers: {
                'X-API-Token': this.apiToken
            }
        });
        return await response.json();
    }
    
    async getTunnelStats() {
        const response = await fetch(`${this.baseUrl}/api/tunnels/stats`, {
            headers: {
                'X-API-Token': this.apiToken
            }
        });
        return await response.json();
    }
}

// 使用示例
const client = new TaiyiClient('http://localhost:8080', 'taiyi_your_api_token');
client.startTunnel(1).then(result => console.log(result));
```

## 🚀 最佳实践

### 1. Token安全

- **安全存储**：将API Token存储在环境变量或安全的配置文件中
- **定期轮换**：定期更新API Token
- **最小权限**：使用最低权限的套餐满足需求

### 2. 错误处理

- **重试机制**：实现指数退避的重试机制
- **错误日志**：记录详细的错误信息用于调试
- **优雅降级**：在服务不可用时提供备选方案

### 3. 性能优化

- **连接复用**：使用HTTP连接池
- **请求缓存**：缓存不经常变化的数据
- **批量操作**：尽可能使用批量API减少请求次数

### 4. 监控告警

- **流量监控**：监控API调用频率和流量使用
- **错误监控**：监控错误率和响应时间
- **配额告警**：在接近配额限制时发送告警

## 📞 技术支持

- **API文档**：http://localhost:8080/api/swagger-ui.html
- **GitHub**：https://github.com/charlie237/taiyi
- **问题反馈**：https://github.com/charlie237/taiyi/issues
