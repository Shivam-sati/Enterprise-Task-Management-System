@echo off
echo ========================================
echo    CHECKING MICROSERVICES STATUS
echo ========================================
echo.

echo Checking Docker containers...
docker-compose ps
echo.

echo ========================================
echo Checking individual services...
echo ========================================

echo 🏢 Eureka Server (Service Discovery):
curl -s http://localhost:8761/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8761
) else (
    echo ❌ Not responding
)

echo 🚪 API Gateway:
curl -s http://localhost:8080/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8080
) else (
    echo ❌ Not responding
)

echo 🔐 Auth Service:
curl -s http://localhost:8081/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8081
) else (
    echo ❌ Not responding
)

echo 📝 Task Service:
curl -s http://localhost:8082/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8082
) else (
    echo ❌ Not responding
)

echo 📧 Notification Service:
curl -s http://localhost:8083/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8083
) else (
    echo ❌ Not responding
)

echo 👥 Collaboration Service:
curl -s http://localhost:8084/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8084
) else (
    echo ❌ Not responding
)

echo 📊 Analytics Service:
curl -s http://localhost:8085/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8085
) else (
    echo ❌ Not responding
)

echo 🤖 AI Service:
curl -s http://localhost:8086/actuator/health > nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Running at http://localhost:8086
) else (
    echo ❌ Not responding
)

echo.
echo ========================================
echo 🌐 Open these in your browser:
echo ========================================
echo Main App: http://localhost:3000
echo Service Dashboard: http://localhost:8761
echo API Gateway: http://localhost:8080
echo.
pause