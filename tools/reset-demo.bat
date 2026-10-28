@echo off
title CampusSkill 演示环境一键重置
setlocal enabledelayedexpansion

rem ============================================================
rem CampusSkill 演示环境一键重置
rem 用途：答辩/演示前把数据库恢复为「干净结构 + 完整演示数据」，
rem       清除压测账号、实验预约单等脏数据。
rem 动作：删库重建 → campus.sql → 必要升级(v5/v6/v9) → 三份种子 → 自检
rem 注意：会清空 campus_runner 库内全部数据（含真实浏览记录），不可恢复！
rem ============================================================

set "MYSQL_CLIENT=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "PROJ=%~dp0.."
set "SQLDIR=%PROJ%\sql"

if not exist "%MYSQL_CLIENT%" (
    echo [错误] 未找到 mysql 客户端：%MYSQL_CLIENT%
    echo        请编辑本文件顶部 MYSQL_CLIENT 为本机路径
    goto :fail
)

echo ============================================
echo   CampusSkill 演示环境一键重置
echo   数据库: campus_runner（将被清空重建）
echo ============================================
echo.
choice /c YN /m "确认清空并重建数据库吗（Y 继续 / N 取消）"
if errorlevel 2 goto :cancel

echo.
echo [1/6] 删库重建 campus_runner ...
"%MYSQL_CLIENT%" -uroot -e "DROP DATABASE IF EXISTS campus_runner; CREATE DATABASE campus_runner DEFAULT CHARACTER SET utf8mb4;"
if errorlevel 1 goto :fail

echo [2/6] 导入基础结构与种子 campus.sql ...
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%PROJ%\campus.sql"
if errorlevel 1 goto :fail

echo [3/6] 应用必要升级（v5 公告表 / v6 账号登录 / v9 性能索引）...
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\upgrade_v5_admin_console.sql" || goto :fail
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\upgrade_v6_account_login.sql" || goto :fail
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\upgrade_v9_performance.sql" || goto :fail

echo [4/6] 导入演示数据（基础演示 + 答辩场景 + 推荐行为）...
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\seed_demo.sql" || goto :fail
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\seed_demo_v7.sql" || goto :fail
"%MYSQL_CLIENT%" -uroot --default-character-set=utf8mb4 campus_runner < "%SQLDIR%\seed_behavior_v8.sql" || goto :fail

echo [5/6] 演示就绪自检（verify-demo.mjs，32 项）...
pushd "%PROJ%"
node tools\verify-demo.mjs
set "VERIFY=%errorlevel%"
popd
if not "%VERIFY%"=="0" goto :fail

echo [6/6] 完成！
echo.
echo ============================================
echo   重置成功。演示账号：
echo   用户/技能者双身份: demo1 / 123456（姓名：测试用户）
echo   技能者: demo2 / 123456（陈技能） demo3 / 123456（林小艺）
echo   管理端: admin / 123456
echo   如应用正在运行，重启 campus-server 后登录即可。
echo ============================================
pause
exit /b 0

:fail
echo.
echo [错误] 重置失败，请查看上方 mysql 报错（常见：服务未启动 / 密码不为空）。
pause
exit /b 1

:cancel
echo 已取消，数据库未做任何改动。
pause
exit /b 0
