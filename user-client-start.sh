#!/bin/bash

# 太乙用户客户端启动脚本
# 用于在用户内网环境中启动zrok客户端，连接到太乙边缘节点

# 默认配置
EDGE_NODE_URL=""
LOCAL_SERVICE=""
TUNNEL_TYPE="http"
SUBDOMAIN=""
AUTH_TOKEN=""
ZROK_BINARY="zrok"

# 显示帮助信息
show_help() {
    echo "太乙用户客户端启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "必需选项:"
    echo "  -e, --edge-node URL     边缘节点地址"
    echo "  -l, --local-service     本地服务地址 (如: localhost:8080)"
    echo "  -t, --token TOKEN       认证Token"
    echo ""
    echo "可选选项:"
    echo "  -T, --type TYPE         隧道类型 (http/tcp, 默认: http)"
    echo "  -s, --subdomain NAME    子域名 (仅http类型)"
    echo "  -z, --zrok-binary PATH  zrok程序路径 (默认: zrok)"
    echo "  -h, --help              显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  # HTTP隧道"
    echo "  $0 -e https://edge1.your-taiyi.com -l localhost:8080 -t your_token"
    echo ""
    echo "  # TCP隧道"
    echo "  $0 -e https://edge1.your-taiyi.com -l localhost:22 -T tcp -t your_token"
    echo ""
    echo "  # 指定子域名"
    echo "  $0 -e https://edge1.your-taiyi.com -l localhost:8080 -s myapp -t your_token"
    echo ""
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--edge-node)
            EDGE_NODE_URL="$2"
            shift 2
            ;;
        -l|--local-service)
            LOCAL_SERVICE="$2"
            shift 2
            ;;
        -t|--token)
            AUTH_TOKEN="$2"
            shift 2
            ;;
        -T|--type)
            TUNNEL_TYPE="$2"
            shift 2
            ;;
        -s|--subdomain)
            SUBDOMAIN="$2"
            shift 2
            ;;
        -z|--zrok-binary)
            ZROK_BINARY="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证必需参数
if [ -z "$EDGE_NODE_URL" ]; then
    echo "错误: 必须指定边缘节点地址"
    show_help
    exit 1
fi

if [ -z "$LOCAL_SERVICE" ]; then
    echo "错误: 必须指定本地服务地址"
    show_help
    exit 1
fi

if [ -z "$AUTH_TOKEN" ]; then
    echo "错误: 必须指定认证Token"
    show_help
    exit 1
fi

echo "========================================="
echo "太乙用户客户端启动"
echo "========================================="
echo "边缘节点: $EDGE_NODE_URL"
echo "本地服务: $LOCAL_SERVICE"
echo "隧道类型: $TUNNEL_TYPE"
if [ -n "$SUBDOMAIN" ]; then
    echo "子域名: $SUBDOMAIN"
fi
echo "========================================="

# 检查zrok是否安装
if ! command -v "$ZROK_BINARY" &> /dev/null; then
    echo "错误: 未找到zrok程序"
    echo ""
    echo "请先安装zrok:"
    echo "1. 访问 https://github.com/openziti/zrok/releases"
    echo "2. 下载适合您系统的版本"
    echo "3. 解压并将zrok程序放到PATH中"
    echo ""
    echo "或者使用 -z 参数指定zrok程序的完整路径"
    exit 1
fi

# 检查zrok版本
echo "检查zrok版本..."
ZROK_VERSION=$($ZROK_BINARY version 2>/dev/null || echo "unknown")
echo "zrok版本: $ZROK_VERSION"

# 检查zrok环境
echo "检查zrok环境..."
if ! $ZROK_BINARY status &>/dev/null; then
    echo "zrok环境未初始化，正在初始化..."
    
    # 这里需要用户提供zrok环境token
    echo "请输入zrok环境token (从太乙控制面板获取):"
    read -r ZROK_ENV_TOKEN
    
    if [ -z "$ZROK_ENV_TOKEN" ]; then
        echo "错误: 必须提供zrok环境token"
        exit 1
    fi
    
    $ZROK_BINARY enable "$ZROK_ENV_TOKEN"
    if [ $? -ne 0 ]; then
        echo "错误: zrok环境初始化失败"
        exit 1
    fi
    
    echo "zrok环境初始化成功"
else
    echo "zrok环境已就绪"
fi

# 构建zrok命令
ZROK_CMD="$ZROK_BINARY share"

case $TUNNEL_TYPE in
    "http")
        ZROK_CMD="$ZROK_CMD public"
        if [ -n "$SUBDOMAIN" ]; then
            ZROK_CMD="$ZROK_CMD --subdomain $SUBDOMAIN"
        fi
        ZROK_CMD="$ZROK_CMD $LOCAL_SERVICE"
        ;;
    "tcp")
        ZROK_CMD="$ZROK_CMD public --backend-mode tcpTunnel $LOCAL_SERVICE"
        ;;
    *)
        echo "错误: 不支持的隧道类型: $TUNNEL_TYPE"
        echo "支持的类型: http, tcp"
        exit 1
        ;;
esac

# 添加认证token
ZROK_CMD="$ZROK_CMD --auth-token $AUTH_TOKEN"

echo "启动隧道..."
echo "执行命令: $ZROK_CMD"
echo ""

# 启动隧道
exec $ZROK_CMD
