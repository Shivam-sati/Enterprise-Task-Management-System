@echo off
echo Starting Advanced Task Management System...

echo Building all services...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed. Exiting...
    exit /b 1
)

echo Starting infrastructure services...
docker-compose up -d mongodb redis rabbitmq

echo Waiting for infrastructure services to be ready...
timeout /t 30 /nobreak

echo Starting Eureka Server...
docker-compose up -d eureka-server

echo Waiting for Eureka Server to be ready...
timeout /t 30 /nobreak

echo Starting API Gateway...
docker-compose up -d api-gateway

echo Waiting for API Gateway to be ready...
timeout /t 20 /nobreak

echo Starting all microservices...
docker-compose up -d auth-service task-service notification-service collaboration-service analytics-service ai-service

echo All services started successfully!
echo Services are available at:
echo - Eureka Server: http://localhost:8761
echo - API Gateway: http://localhost:8080
echo - Auth Service: http://localhost:8081
echo - Task Service: http://localhost:8082
echo - Notification Service: http://localhost:8083
echo - Collaboration Service: http://localhost:8084
echo - Analytics Service: http://localhost:8085
echo - AI Service: http://localhost:8086
echo - MongoDB: localhost:27017
echo - Redis: localhost:6379
echo - RabbitMQ Management: http://localhost:15672 (admin/password)