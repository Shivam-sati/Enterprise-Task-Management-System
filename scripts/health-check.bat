@echo off
echo Checking health of all services...
echo.

echo Checking Eureka Server...
curl -s http://localhost:8761/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Eureka Server is healthy
) else (
    echo ✗ Eureka Server is not responding
)

echo Checking API Gateway...
curl -s http://localhost:8080/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ API Gateway is healthy
) else (
    echo ✗ API Gateway is not responding
)

echo Checking Auth Service...
curl -s http://localhost:8081/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Auth Service is healthy
) else (
    echo ✗ Auth Service is not responding
)

echo Checking Task Service...
curl -s http://localhost:8082/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Task Service is healthy
) else (
    echo ✗ Task Service is not responding
)

echo Checking Notification Service...
curl -s http://localhost:8083/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Notification Service is healthy
) else (
    echo ✗ Notification Service is not responding
)

echo Checking Collaboration Service...
curl -s http://localhost:8084/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Collaboration Service is healthy
) else (
    echo ✗ Collaboration Service is not responding
)

echo Checking Analytics Service...
curl -s http://localhost:8085/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ Analytics Service is healthy
) else (
    echo ✗ Analytics Service is not responding
)

echo Checking AI Service...
curl -s http://localhost:8086/actuator/health > nul
if %ERRORLEVEL% equ 0 (
    echo ✓ AI Service is healthy
) else (
    echo ✗ AI Service is not responding
)

echo.
echo Health check completed!