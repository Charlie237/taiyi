@echo off
chcp 65001 >nul

REM 太乙用户客户端启动脚本 (Windows)

REM 默认配置
set EDGE_NODE_URL=
set LOCAL_SERVICE=
set TUNNEL_TYPE=http
set SUBDOMAIN=
set AUTH_TOKEN=
set ZROK_BINARY=zrok.exe

REM 解析命令行参数
:parse_args
if "%1"=="" goto start_client
if "%1"=="-s" (
    set SERVER_URL=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--server" (
    set SERVER_URL=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-n" (
    set NODE_ID=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--node-id" (
    set NODE_ID=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-l" (
    set LOG_LEVEL=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--log-level" (
    set LOG_LEVEL=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-h" goto show_help
if "%1"=="--help" goto show_help

echo 未知选项: %1
goto show_help

:show_help
echo 太乙节点客户端启动脚本
echo.
echo 用法: %0 [选项]
echo.
echo 选项:
echo   -s, --server URL     服务器地址 (默认: http://localhost:8080)
echo   -n, --node-id ID     节点ID (默认: 自动生成)
echo   -l, --log-level LVL  日志级别 (默认: INFO)
echo   -h, --help           显示此帮助信息
echo.
echo 示例:
echo   %0 -s http://your-server.com:8080 -n my-node-001
echo.
goto end

:start_client
REM 如果没有指定节点ID，自动生成一个
if "%NODE_ID%"=="" (
    for /f "tokens=2 delims==" %%i in ('wmic computersystem get name /value') do set HOSTNAME=%%i
    for /f "tokens=1-3 delims=: " %%a in ('echo %time%') do set TIMESTAMP=%%a%%b%%c
    set NODE_ID=node_%HOSTNAME%_%TIMESTAMP%
)

echo =========================================
echo 太乙节点客户端启动
echo =========================================
echo 服务器地址: %SERVER_URL%
echo 节点ID: %NODE_ID%
echo 日志级别: %LOG_LEVEL%
echo =========================================

REM 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Java环境，请先安装Java 17或更高版本
    goto end
)

REM 检查项目jar文件是否存在
if not exist "target\taiyi-*.jar" (
    echo 错误: 未找到jar文件，请先编译项目
    echo 运行: mvn clean package
    goto end
)

REM 获取jar文件名
for %%f in (target\taiyi-*.jar) do set JAR_FILE=%%f

REM 启动客户端
echo 启动太乙节点客户端...
java -cp "%JAR_FILE%" ^
    -Dtaiyi.server.url="%SERVER_URL%" ^
    -Dtaiyi.node.id="%NODE_ID%" ^
    -Dlogging.level.root="%LOG_LEVEL%" ^
    io.github.charlie237.taiyi.client.TaiyiNodeClient

echo 太乙节点客户端已退出

:end
pause
