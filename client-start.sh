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
        -s|--server)
            SERVER_URL="$2"
            shift 2
            ;;
        -n|--node-id)
            NODE_ID="$2"
            shift 2
            ;;
        -l|--log-level)
            LOG_LEVEL="$2"
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

# 如果没有指定节点ID，自动生成一个
if [ -z "$NODE_ID" ]; then
    NODE_ID="node_$(hostname)_$(date +%s)"
fi

echo "========================================="
echo "太乙节点客户端启动"
echo "========================================="
echo "服务器地址: $SERVER_URL"
echo "节点ID: $NODE_ID"
echo "日志级别: $LOG_LEVEL"
echo "========================================="

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装Java 17或更高版本"
    exit 1
fi

# 检查项目jar文件是否存在
JAR_FILE="target/taiyi-*.jar"
if ! ls $JAR_FILE 1> /dev/null 2>&1; then
    echo "错误: 未找到jar文件，请先编译项目"
    echo "运行: mvn clean package"
    exit 1
fi

# 获取实际的jar文件名
JAR_FILE=$(ls target/taiyi-*.jar | head -1)

# 启动客户端
echo "启动太乙节点客户端..."
java -cp "$JAR_FILE" \
    -Dtaiyi.server.url="$SERVER_URL" \
    -Dtaiyi.node.id="$NODE_ID" \
    -Dlogging.level.root="$LOG_LEVEL" \
    io.github.charlie237.taiyi.client.TaiyiNodeClient

echo "太乙节点客户端已退出"
