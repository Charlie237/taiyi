#!/bin/bash

# 太乙边缘节点部署脚本

set -e

# 默认配置
CONTROL_CENTER_URL=""
NODE_ID=""
NODE_NAME=""
AUTH_TOKEN=""
ZROK_VERSION="latest"
INSTALL_DIR="/opt/taiyi-edge"
SERVICE_USER="taiyi"
LOG_LEVEL="INFO"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
太乙边缘节点部署脚本

用法: $0 [选项]

必需选项:
  -c, --control-center URL    太乙控制中心地址
  -t, --token TOKEN          认证Token

可选选项:
  -n, --node-id ID           节点ID (默认: 自动生成)
  -N, --node-name NAME       节点名称 (默认: 主机名)
  -z, --zrok-version VER     zrok版本 (默认: latest)
  -d, --install-dir DIR      安装目录 (默认: /opt/taiyi-edge)
  -u, --user USER            服务用户 (默认: taiyi)
  -l, --log-level LEVEL      日志级别 (默认: INFO)
  -h, --help                 显示此帮助信息

示例:
  $0 -c https://your-taiyi-server.com -t your_auth_token
  $0 -c https://your-taiyi-server.com -t your_token -n edge-node-001 -N "北京节点"

EOF
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--control-center)
            CONTROL_CENTER_URL="$2"
            shift 2
            ;;
        -t|--token)
            AUTH_TOKEN="$2"
            shift 2
            ;;
        -n|--node-id)
            NODE_ID="$2"
            shift 2
            ;;
        -N|--node-name)
            NODE_NAME="$2"
            shift 2
            ;;
        -z|--zrok-version)
            ZROK_VERSION="$2"
            shift 2
            ;;
        -d|--install-dir)
            INSTALL_DIR="$2"
            shift 2
            ;;
        -u|--user)
            SERVICE_USER="$2"
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
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证必需参数
if [ -z "$CONTROL_CENTER_URL" ]; then
    log_error "必须指定控制中心地址"
    show_help
    exit 1
fi

if [ -z "$AUTH_TOKEN" ]; then
    log_error "必须指定认证Token"
    show_help
    exit 1
fi

# 设置默认值
if [ -z "$NODE_ID" ]; then
    NODE_ID="edge_$(hostname)_$(date +%s)"
fi

if [ -z "$NODE_NAME" ]; then
    NODE_NAME="$(hostname)"
fi

# 检查权限
if [ "$EUID" -ne 0 ]; then
    log_error "请使用root权限运行此脚本"
    exit 1
fi

log_info "开始部署太乙边缘节点"
log_info "控制中心: $CONTROL_CENTER_URL"
log_info "节点ID: $NODE_ID"
log_info "节点名称: $NODE_NAME"
log_info "安装目录: $INSTALL_DIR"

# 检查系统
check_system() {
    log_info "检查系统环境..."
    
    # 检查操作系统
    if [ ! -f /etc/os-release ]; then
        log_error "不支持的操作系统"
        exit 1
    fi
    
    # 检查架构
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)
            ZROK_ARCH="amd64"
            ;;
        aarch64|arm64)
            ZROK_ARCH="arm64"
            ;;
        *)
            log_error "不支持的架构: $ARCH"
            exit 1
            ;;
    esac
    
    log_info "系统架构: $ARCH -> $ZROK_ARCH"
}

# 安装依赖
install_dependencies() {
    log_info "安装依赖..."
    
    # 检测包管理器
    if command -v apt-get &> /dev/null; then
        apt-get update
        apt-get install -y curl wget unzip openjdk-17-jre-headless
    elif command -v yum &> /dev/null; then
        yum update -y
        yum install -y curl wget unzip java-17-openjdk-headless
    elif command -v dnf &> /dev/null; then
        dnf update -y
        dnf install -y curl wget unzip java-17-openjdk-headless
    else
        log_error "不支持的包管理器"
        exit 1
    fi
    
    # 验证Java安装
    if ! command -v java &> /dev/null; then
        log_error "Java安装失败"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log_info "Java版本: $JAVA_VERSION"
}

# 创建用户
create_user() {
    log_info "创建服务用户: $SERVICE_USER"
    
    if ! id "$SERVICE_USER" &>/dev/null; then
        useradd -r -s /bin/false -d "$INSTALL_DIR" "$SERVICE_USER"
        log_info "用户 $SERVICE_USER 创建成功"
    else
        log_info "用户 $SERVICE_USER 已存在"
    fi
}

# 安装zrok
install_zrok() {
    log_info "安装zrok..."
    
    # 创建临时目录
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    # 下载zrok
    if [ "$ZROK_VERSION" = "latest" ]; then
        DOWNLOAD_URL="https://github.com/openziti/zrok/releases/latest/download/zrok_linux_${ZROK_ARCH}.tar.gz"
    else
        DOWNLOAD_URL="https://github.com/openziti/zrok/releases/download/v${ZROK_VERSION}/zrok_linux_${ZROK_ARCH}.tar.gz"
    fi
    
    log_info "下载zrok: $DOWNLOAD_URL"
    wget -O zrok.tar.gz "$DOWNLOAD_URL"
    
    # 解压并安装
    tar -xzf zrok.tar.gz
    chmod +x zrok
    mv zrok /usr/local/bin/
    
    # 验证安装
    if ! command -v zrok &> /dev/null; then
        log_error "zrok安装失败"
        exit 1
    fi
    
    ZROK_VERSION_INSTALLED=$(zrok version)
    log_info "zrok安装成功: $ZROK_VERSION_INSTALLED"
    
    # 清理临时文件
    cd /
    rm -rf "$TEMP_DIR"
}

# 下载太乙Agent
download_agent() {
    log_info "下载太乙边缘节点Agent..."
    
    # 创建安装目录
    mkdir -p "$INSTALL_DIR"
    cd "$INSTALL_DIR"
    
    # 这里应该从太乙官方下载Agent jar包
    # 暂时使用占位符
    log_warn "请手动将taiyi-edge-agent.jar放置到 $INSTALL_DIR 目录"
    
    # 创建配置文件
    cat > "$INSTALL_DIR/agent.properties" << EOF
# 太乙边缘节点Agent配置
taiyi.control.center.url=$CONTROL_CENTER_URL
taiyi.node.id=$NODE_ID
taiyi.node.name=$NODE_NAME
taiyi.auth.token=$AUTH_TOKEN
logging.level.root=$LOG_LEVEL
EOF
    
    # 设置权限
    chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR"
    chmod 600 "$INSTALL_DIR/agent.properties"
}

# 创建systemd服务
create_service() {
    log_info "创建systemd服务..."
    
    cat > /etc/systemd/system/taiyi-edge.service << EOF
[Unit]
Description=Taiyi Edge Node Agent
After=network.target
Wants=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -jar $INSTALL_DIR/taiyi-edge-agent.jar --spring.config.location=file:$INSTALL_DIR/agent.properties
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=taiyi-edge

# 安全设置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=$INSTALL_DIR

[Install]
WantedBy=multi-user.target
EOF

    # 重载systemd
    systemctl daemon-reload
    systemctl enable taiyi-edge.service
    
    log_info "systemd服务创建成功"
}

# 配置防火墙
configure_firewall() {
    log_info "配置防火墙..."
    
    # 检查防火墙类型
    if command -v ufw &> /dev/null; then
        # Ubuntu/Debian UFW
        ufw allow 22/tcp comment "SSH"
        log_info "UFW防火墙配置完成"
    elif command -v firewall-cmd &> /dev/null; then
        # CentOS/RHEL firewalld
        firewall-cmd --permanent --add-service=ssh
        firewall-cmd --reload
        log_info "firewalld防火墙配置完成"
    else
        log_warn "未检测到防火墙，请手动配置"
    fi
}

# 启动服务
start_service() {
    log_info "启动太乙边缘节点服务..."
    
    # 检查Agent jar文件是否存在
    if [ ! -f "$INSTALL_DIR/taiyi-edge-agent.jar" ]; then
        log_error "Agent jar文件不存在: $INSTALL_DIR/taiyi-edge-agent.jar"
        log_error "请从太乙官方下载Agent并放置到指定位置"
        return 1
    fi
    
    systemctl start taiyi-edge.service
    
    # 等待服务启动
    sleep 5
    
    if systemctl is-active --quiet taiyi-edge.service; then
        log_info "太乙边缘节点服务启动成功"
        systemctl status taiyi-edge.service --no-pager
    else
        log_error "太乙边缘节点服务启动失败"
        journalctl -u taiyi-edge.service --no-pager -n 20
        return 1
    fi
}

# 显示部署信息
show_deployment_info() {
    cat << EOF

${GREEN}========================================${NC}
${GREEN}太乙边缘节点部署完成！${NC}
${GREEN}========================================${NC}

节点信息:
  节点ID: $NODE_ID
  节点名称: $NODE_NAME
  控制中心: $CONTROL_CENTER_URL

安装信息:
  安装目录: $INSTALL_DIR
  服务用户: $SERVICE_USER
  配置文件: $INSTALL_DIR/agent.properties

服务管理:
  启动服务: systemctl start taiyi-edge
  停止服务: systemctl stop taiyi-edge
  重启服务: systemctl restart taiyi-edge
  查看状态: systemctl status taiyi-edge
  查看日志: journalctl -u taiyi-edge -f

${YELLOW}注意事项:${NC}
1. 请确保Agent jar文件已放置到 $INSTALL_DIR/taiyi-edge-agent.jar
2. 请确保网络连接正常，能够访问控制中心
3. 如有问题，请查看日志进行排查

EOF
}

# 主函数
main() {
    check_system
    install_dependencies
    create_user
    install_zrok
    download_agent
    create_service
    configure_firewall
    
    if start_service; then
        show_deployment_info
    else
        log_error "部署过程中出现错误，请检查日志"
        exit 1
    fi
}

# 执行主函数
main
