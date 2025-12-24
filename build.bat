@echo off
echo ========================================
echo    API项目构建脚本 (Windows)
echo ========================================

echo [1/4] 清理项目...
call mvn clean

if %errorlevel% neq 0 (
    echo 清理失败，退出构建
    pause
    exit /b 1
)

echo [2/4] 安装公共模块到本地仓库...
call mvn install -pl api_common -am -DskipTests

if %errorlevel% neq 0 (
    echo 公共模块安装失败，退出构建
    pause
    exit /b 1
)

echo [3/4] 编译所有模块 (详细日志输出)...
call mvn compile -X

if %errorlevel% neq 0 (
    echo 编译失败，退出构建
    pause
    exit /b 1
)

echo [4/4] 打包项目 (跳过测试)...
call mvn package -DskipTests

if %errorlevel% neq 0 (
    echo 打包失败，退出构建
    pause
    exit /b 1
)

echo ========================================
echo    构建成功完成!
echo ========================================
echo 可运行的JAR包位置:
echo api_web/target/api_web-0.0.1-SNAPSHOT.jar
echo ========================================
pause