@echo off
title CampusSkill 校园技能工坊 一键停止
setlocal enabledelayedexpansion

rem ==================== 路径配置（与 start.bat 一致） ====================
set "ROOT=%~dp0"
rem 兼容两种布局：本文件放在仓库根目录，或放在仓库上层目录
if exist "%ROOT%campus-runner\pom.xml" (
    set "PROJ=%ROOT%campus-runner"
) else (
    set "PROJ=%ROOT%"
)
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"
set "REDIS_DIR=%ROOT%.local\redis"

rem ==================== 参数解析 ====================
rem 用法：stop.bat              停止全部（后端 + Redis + MySQL）
rem       stop.bat server       仅停止 campus-server 后端
rem       stop.bat redis        仅停止 Redis
rem       stop.bat mysql        仅停止 MySQL
set "TARGET=%~1"
if not defined TARGET set "TARGET=all"
if /i "%TARGET%"=="all"    goto :run
if /i "%TARGET%"=="server" goto :run
if /i "%TARGET%"=="redis"  goto :run
if /i "%TARGET%"=="mysql"  goto :run
echo.
echo 用法：stop.bat [server^|redis^|mysql]
echo   （无参数 = 全部停止：后端 + Redis + MySQL）
exit /b 1

:run
echo ============================================
echo   CampusSkill 一键停止
echo   项目目录: %PROJ%
echo ============================================
echo.

if /i "%TARGET%"=="all"    call :stop_server
if /i "%TARGET%"=="server" call :stop_server
if /i "%TARGET%"=="all"    call :stop_redis
if /i "%TARGET%"=="redis"  call :stop_redis
if /i "%TARGET%"=="all"    call :stop_mysql
if /i "%TARGET%"=="mysql"  call :stop_mysql

echo.
echo 停止完成。
exit /b 0

rem ==================== 子程序 ====================

rem ---- campus-server：按 8080 端口定位 PID 后结束 ----
:stop_server
echo [后端] campus-server (8080)...
call :port_pid 8080 pid
if not defined pid (
    echo   未在运行
    goto :eof
)
set "killed=%pid%"
taskkill /F /PID %pid% >nul 2>nul
call :wait_port_down 8080 15
if errorlevel 1 (
    echo   [警告] 端口 8080 仍未释放，请手动检查进程
) else (
    echo   已停止（PID %killed%）
)
goto :eof

rem ---- Redis：redis-cli 优雅停止，超时再按 PID 强制结束 ----
:stop_redis
echo [Redis] (6379)...
call :port_pid 6379 pid
if not defined pid (
    echo   未在运行
    goto :eof
)
set "killed=%pid%"
echo   redis-cli shutdown（优雅停止，保存快照）...
"%REDIS_DIR%\redis-cli.exe" -h 127.0.0.1 -p 6379 shutdown >nul 2>nul
call :wait_port_down 6379 10
if errorlevel 1 (
    echo   优雅停止超时，强制结束（PID %killed%）...
    taskkill /F /PID %killed% >nul 2>nul
    call :wait_port_down 6379 10
)
call :port_pid 6379 pid
if defined pid (
    echo   [警告] Redis 仍未停止，请手动检查
) else (
    echo   已停止
)
goto :eof

rem ---- MySQL：mysqladmin 优雅停止（root 空密码），失败再按 PID 强制结束 ----
:stop_mysql
echo [MySQL] (3306)...
call :port_pid 3306 pid
if not defined pid (
    echo   未在运行
    goto :eof
)
set "killed=%pid%"
echo   mysqladmin shutdown（优雅停止，落盘事务）...
"%MYSQL_BIN%\mysqladmin.exe" -uroot shutdown >nul 2>nul
if errorlevel 1 (
    echo   优雅停止失败（可能 root 已设置密码），强制结束（PID %killed%）...
    echo           InnoDB 具备崩溃恢复能力，数据一般安全，建议事后重启核对
    taskkill /F /PID %killed% >nul 2>nul
) else (
    call :wait_port_down 3306 20
    if errorlevel 1 (
        echo   优雅停止超时，强制结束（PID %killed%）...
        taskkill /F /PID %killed% >nul 2>nul
    )
)
call :wait_port_down 3306 15
call :port_pid 3306 pid
if defined pid (
    echo   [警告] MySQL 仍未停止，请手动检查
) else (
    echo   已停止
)
goto :eof

rem ---- 工具：取监听指定端口的第一个 PID ----
:port_pid
rem 参数: %1=端口 %2=返回变量名；找到时设置该变量，否则清空
set "%~2="
for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
    if not defined %~2 set "%~2=%%p"
)
goto :eof

rem ---- 工具：等待端口释放 ----
:wait_port_down
rem 参数: %1=端口 %2=最大等待秒数；已释放返回 0，超时返回 1
set /a waited=0
:wait_down_loop
call :port_pid %~1 pid
if not defined pid exit /b 0
if %waited% geq %~2 exit /b 1
ping -n 2 127.0.0.1 >nul
set /a waited+=2
goto :wait_down_loop
