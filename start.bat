@echo off
title CampusSkill 校园技能工坊 一键启动
setlocal enabledelayedexpansion

rem ==================== 路径配置（按需修改） ====================
set "ROOT=%~dp0"
rem 兼容两种布局：本文件放在仓库根目录，或放在仓库上层目录
if exist "%ROOT%campus-runner\pom.xml" (
    set "PROJ=%ROOT%campus-runner"
) else (
    set "PROJ=%ROOT%"
)
set "JAR=%PROJ%\campus-server\target\campus-server-1.0-SNAPSHOT.jar"
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.8.7-hotspot"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "MVN=%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd"
if not exist "%MVN%" set "MVN=%PROJ%\..\"
set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
set "MYSQL_CLIENT=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "DATADIR=%ROOT%.local\mysql-data"
set "REDIS=%ROOT%.local\redis\redis-server.exe"

echo ============================================
echo   CampusSkill 校园技能工坊 一键启动
echo   项目目录: %PROJ%
echo ============================================
echo.

rem ==================== 0. 环境自检 ====================
echo [0/4] 环境自检...
if not exist "%JAVA%" (
    echo   [错误] 未找到 Java: %JAVA%
    echo   请编辑本文件顶部 JAVA_HOME 为本机 JDK 路径
    goto :fail
)
echo   Java    OK
if not exist "%MVN%" (
    echo   [提示] 未找到 Maven（%MVN%），若需要自动构建将失败
) else (
    echo   Maven   OK
)
if not exist "%MYSQLD%" (
    echo   [提示] 未找到 %MYSQLD%
    echo          若 MySQL 已作为系统服务运行可忽略，否则请修改 MYSQLD 路径
)
if not exist "%REDIS%" (
    echo   [提示] 未找到 %REDIS%
    echo          若 Redis 已在运行可忽略，否则请修改 REDIS 路径
)

rem ==================== 1. MySQL ====================
echo [1/4] 检查 MySQL (3306)...
netstat -ano 2>nul | findstr ":3306 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    if not exist "%DATADIR%" (
        echo   首次运行，初始化数据目录...
        mkdir "%DATADIR%" 2>nul
        "%MYSQLD%" --initialize-insecure --datadir="%DATADIR%" --character-set-server=utf8mb4
        if errorlevel 1 (
            echo   [错误] MySQL 初始化失败，请查看上方日志
            goto :fail
        )
    )
    echo   启动 MySQL...
    start "campus-mysql" /min "%MYSQLD%" --datadir="%DATADIR%" --port=3306 --console
    call :wait_port 3306 MySQL 30 || goto :fail
) else (
    echo   MySQL 已在运行
)

rem ==================== 2. Redis ====================
echo [2/4] 检查 Redis (6379)...
netstat -ano 2>nul | findstr ":6379 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    echo   启动 Redis...
    start "campus-redis" /min "%REDIS%" --port 6379 --bind 127.0.0.1 --protected-mode no
    call :wait_port 6379 Redis 15 || goto :fail
) else (
    echo   Redis 已在运行
)

rem ==================== 3. 构建（jar 不存在时） ====================
echo [3/4] 检查应用 jar...
if not exist "%JAR%" (
    echo   未找到 jar，开始构建，首次约 1-2 分钟...
    pushd "%PROJ%"
    call "%MVN%" -q package -DskipTests
    popd
    if not exist "%JAR%" (
        echo   [错误] 构建失败，请查看上方 Maven 输出
        goto :fail
    )
) else (
    echo   jar 已就绪
)

rem ==================== 4. 启动应用 ====================
echo [4/4] 检查应用 (8080)...
netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    echo   启动 campus-server（新窗口运行，请勿关闭）...
    start "campus-server" "%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%"
    call :wait_port 8080 campus-server 60 || goto :fail
) else (
    echo   应用已在运行
)

echo.
echo ============================================
echo   启动完成！
echo   管理端:   http://localhost:8080/admin-web/   （admin / 123456）
echo   用户端:   http://localhost:8080/user-app/    （演示账号 demo1）
echo   技能者端: http://localhost:8080/skiller-app/ （演示账号 demo2）
echo   接口文档: http://localhost:8080/doc.html
echo   停止服务: 关闭 campus-mysql / campus-redis / campus-server 窗口
echo ============================================
if not defined NO_OPEN (
    start "" http://localhost:8080/admin-web/
    start "" http://localhost:8080/doc.html
)
echo 窗口将在 8 秒后自动关闭...
ping -n 9 127.0.0.1 >nul
exit /b 0

:wait_port
rem 参数: %1=端口 %2=名称 %3=最大等待秒数
set /a waited=0
:wait_loop
netstat -ano 2>nul | findstr ":%1 " | findstr "LISTENING" >nul 2>nul
if not errorlevel 1 (
    echo   %2 已就绪
    exit /b 0
)
if %waited% geq %3 (
    echo   [错误] %2 启动超时（%3 秒），请查看上方日志
    exit /b 1
)
ping -n 3 127.0.0.1 >nul
set /a waited+=2
goto :wait_loop

:fail
echo.
echo 启动失败。本窗口保持打开，按任意键退出...
pause >nul
exit /b 1
