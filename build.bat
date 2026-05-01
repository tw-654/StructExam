@echo off
echo ====================================
echo StructExam - 本地构建脚本
echo ====================================
echo.

echo [1/2] 构建 common 模块...
cd backend
call mvn clean install -DskipTests -pl common -am
if errorlevel 1 (
    echo 构建 common 模块失败！
    exit /b 1
)

echo.
echo [2/2] 构建所有服务...
call mvn clean package -DskipTests -pl common,gateway,services/user-service,services/exam-service,services/code-service -am
if errorlevel 1 (
    echo 构建服务失败！
    exit /b 1
)

echo.
echo ====================================
echo 构建完成！
echo ====================================
cd ..
pause
