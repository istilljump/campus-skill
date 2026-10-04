@echo off
title CampusSkill 校园技能工坊 一键启动
setlocal enabledelayedexpansion

rem ============ 路径配置（按需修改） ============
set "ROOT=%~dp0"
rem 兼容两种布局：start.bat 在仓库上层或直接在仓库根目录
if exist "%ROOT%campus-runner\pom.xml" (set "PROJ=%ROOT%campus-runner") else (set "PROJ=%ROOT%")
set "JAR=%PROJ%\campus-server\target\campus-server-1.0-SNAPSHOT.jar"
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.8.7-hotspot"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "MVN=%ROOT%.local\apache-maven-3.9.9\bin\mvn.cmd"
set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
set "DATADIR=%ROOT%.local\mysql-data"
set "REDIS=%ROOT%.local\redis\redis-server.exe"

echo ============================================
echo   CampusSkill 校园技能工坊 一键启动
echo ============================================

rem ============ 1. MySQL ============
echo [1/4] 检查 MySQL (3306)...
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    if not exist "%DATADIR%" (
        echo   首次运行，初始化数据目录...
        mkdir "%DATADIR%" 2>nul
        "%MYSQLD%" --initialize-insecure --datadir="%DATADIR%" --character-set-server=utf8mb4
    )
    echo   启动 MySQL...
    start "campus-mysql" /min "%MYSQLD%" --datadir="%DATADIR%" --port=3306 --console
    call :wait_port 3306 MySQL 30 || goto :fail
) else (
    echo   MySQL 已在运行
)

rem ============ 2. Redis ============
echo [2/4] 检查 Redis (6379)...
netstat -ano | findstr ":6379 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    echo   启动 Redis...
    start "campus-redis" /min "%REDIS%" --port 6379 --bind 127.0.0.1 --protected-mode no
    call :wait_port 6379 Redis 15 || goto :fail
) else (
    echo   Redis 已在运行
)

rem ============ 3. 构建（jar 不存在时） ============
echo [3/4] 检查应用 jar...
if not exist "%JAR%" (
    echo   未找到 jar，开始构建，首次约1-2分钟...
    pushd "%PROJ%"
    call "%MVN%" -q package -DskipTests
    popd
    if not exist "%JAR%" (
        echo   构建失败，请检查 Maven 输出
        goto :fail
    )
) else (
    echo   jar 已就绪
)

rem ============ 4. 启动应用 ============
echo [4/4] 检查应用 (8080)...
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul 2>nul
if errorlevel 1 (
    echo   启动 campus-server...
    start "campus-server" "%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%"
    call :wait_port 8080 campus-server 60 || goto :fail
) else (
    echo   应用已在运行
)

echo.
echo ============================================
echo   启动完成！
echo   接口文档: http://localhost:8080/doc.html
echo   管理端账号: admin / 123456
echo   停止服务: 运行 stop.bat 或关闭对应窗口
echo ============================================
start "" http://localhost:8080/admin-web/
start "" http://localhost:8080/doc.html
"%WINDIR%\System32	imeout.exe" /t 8 /nobreak >nul
exit /b 0

:wait_port
rem %1=端口 %2=名称 %3=最大等待秒数
set /a waited=0
:wait_loop
netstat -ano | findstr ":%1 " | findstr "LISTENING" >nul 2>nul
if not errorlevel 1 exit /b 0
if %waited% geq %3 (
    echo   %2 启动超时
    exit /b 1
)
"%WINDIR%\System32	imeout.exe" /t 2 /nobreak >nul
set /a waited+=2
goto :wait_loop

:fail
echo.
echo 启动失败，请检查上方日志
pause
exit /b 1
