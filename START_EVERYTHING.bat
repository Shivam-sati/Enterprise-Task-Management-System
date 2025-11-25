@echo off
echo ========================================
echo    STARTING MICROSERVICES PROJECT
echo ========================================
echo.
echo This will start your entire system automatically!
echo Please be patient - it takes 3-5 minutes for everything to start.
echo.
echo What's happening:
echo 1. Building all 8 services...
echo 2. Starting database and infrastructure...
echo 3. Starting all microservices...
echo 4. Starting the web interface...
echo.
pause

echo ========================================
echo Step 1: Building the project...
echo ========================================
mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ❌ Build failed! Check the error messages above.
    pause
    exit /b 1
)
echo ✅ Build successful!
echo.

echo ========================================
echo Step 2: Starting infrastructure...
echo ========================================
echo Starting Redis and RabbitMQ...
docker-compose up -d redis rabbitmq
echo Waiting 30 seconds for infrastructure to be ready...
timeout /t 30 /nobreak > nul
echo.

echo ========================================
echo Step 3: Starting Eureka Server (Service Discovery)...
echo ========================================
docker-compose up -d eureka-server
echo Waiting 30 seconds for Eureka to be ready...
timeout /t 30 /nobreak > nul
echo.

echo ========================================
echo Step 4: Starting API Gateway...
echo ========================================
docker-compose up -d api-gateway
echo Waiting 20 seconds for Gateway to be ready...
timeout /t 20 /nobreak > nul
echo.

echo ========================================
echo Step 5: Starting Python AI service...
echo ========================================
docker-compose up -d ai-python-service
echo Waiting 20 seconds for Python AI service to be ready...
timeout /t 20 /nobreak > nul
echo.

echo ========================================
echo Step 6: Starting all microservices...
echo ========================================
docker-compose up -d auth-service task-service notification-service
docker-compose up -d collaboration-service analytics-service ai-service
echo Waiting 30 seconds for services to register...
timeout /t 30 /nobreak > nul
echo.

echo ========================================
echo Step 7: Starting React frontend...
echo ========================================
docker-compose up -d todo-react
echo.

echo ========================================
echo 🎉 SUCCESS! Your microservices are starting!
echo ========================================
echo.
echo 📱 Your applications will be available at:
echo.
echo 🌐 Main Website: http://localhost:3000
echo 🚪 API Gateway: http://localhost:8080
echo 📋 Service Dashboard: http://localhost:8761
echo 🐰 Message Queue: http://localhost:15672 (admin/password)
echo 🤖 Python AI Service: http://localhost:8087
echo 📊 Analytics Metrics: http://localhost:9091/metrics
echo 🔬 AI Metrics: http://localhost:9090/metrics
echo.
echo ⏰ Please wait 2-3 more minutes for everything to fully start.
echo Then open your browser and go to: http://localhost:3000
echo.
echo 💡 To check if everything is running, run: CHECK_STATUS.bat
echo 🛑 To stop everything, run: STOP_EVERYTHING.bat
echo.
pause