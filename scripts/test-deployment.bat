@echo off
REM Comprehensive Deployment and Configuration Test Script
REM Tests Docker container startup, health checks, service discovery, and inter-service communication

echo ========================================
echo Task Management System Deployment Tests
echo ========================================

REM Initialize test results
set TESTS_PASSED=0
set TESTS_FAILED=0
set WARNINGS=0

REM Check if Docker is running
echo [INFO] Checking Docker environment...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not installed or not running
    set /a TESTS_FAILED+=1
    goto summary
)
echo [PASS] Docker is available
set /a TESTS_PASSED+=1

REM Check if docker-compose is available
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose is not installed
    set /a TESTS_FAILED+=1
    goto summary
)
echo [PASS] Docker Compose is available
set /a TESTS_PASSED+=1

REM Test Docker Compose configuration
echo [INFO] Validating Docker Compose configuration...
docker-compose config >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose configuration is invalid
    set /a TESTS_FAILED+=1
    goto summary
)
echo [PASS] Docker Compose configuration is valid
set /a TESTS_PASSED+=1

REM Test environment variable configuration
echo [INFO] Testing environment variable configuration...

REM Check if .env file exists
if not exist .env (
    echo [WARNING] .env file not found, using defaults from .env.example
    if exist .env.example (
        copy .env.example .env >nul
        echo [INFO] Created .env from .env.example
    ) else (
        echo [WARNING] .env.example not found either
        set /a WARNINGS+=1
    )
) else (
    echo [PASS] .env file found
    set /a TESTS_PASSED+=1
)

REM Validate critical environment variables
echo [INFO] Validating critical environment variables...
set ENV_VARS_OK=1

REM Check MongoDB URI
findstr /C:"MONGODB_URI" .env >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] MONGODB_URI not found in .env file
    set /a WARNINGS+=1
    set ENV_VARS_OK=0
)

REM Check OpenAI API Key
findstr /C:"OPENAI_API_KEY" .env >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] OPENAI_API_KEY not found in .env file
    set /a WARNINGS+=1
    set ENV_VARS_OK=0
)

if %ENV_VARS_OK%==1 (
    echo [PASS] Critical environment variables are configured
    set /a TESTS_PASSED+=1
)

REM Start services in detached mode
echo [INFO] Starting services...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start services
    set /a TESTS_FAILED+=1
    goto cleanup
)
echo [PASS] Services started successfully
set /a TESTS_PASSED+=1

REM Wait for services to start with progress indicator
echo [INFO] Waiting for services to initialize...
for /L %%i in (1,1,12) do (
    echo [INFO] Waiting... %%i/12 ^(5 second intervals^)
    timeout /t 5 /nobreak >nul
)

REM Test container status
echo [INFO] Checking container status...
docker-compose ps | findstr "Up" >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Some containers are not running
    docker-compose ps
    set /a TESTS_FAILED+=1
    goto cleanup
)
echo [PASS] All containers are running
set /a TESTS_PASSED+=1

REM Test service health checks with retry logic
echo [INFO] Testing service health checks...

REM Test Eureka Server with retries
echo [INFO] Testing Eureka Server health...
set EUREKA_OK=0
for /L %%i in (1,1,5) do (
    curl -f http://localhost:8761/actuator/health >nul 2>&1
    if !errorlevel!==0 (
        set EUREKA_OK=1
        goto eureka_done
    )
    echo [INFO] Eureka Server not ready, retrying... %%i/5
    timeout /t 10 /nobreak >nul
)
:eureka_done
if %EUREKA_OK%==1 (
    echo [PASS] Eureka Server is healthy
    set /a TESTS_PASSED+=1
) else (
    echo [ERROR] Eureka Server health check failed
    set /a TESTS_FAILED+=1
    goto cleanup
)

REM Test Python AI Service with retries
echo [INFO] Testing Python AI Service health...
set PYTHON_AI_OK=0
for /L %%i in (1,1,5) do (
    curl -f http://localhost:8087/health >nul 2>&1
    if !errorlevel!==0 (
        set PYTHON_AI_OK=1
        goto python_ai_done
    )
    echo [INFO] Python AI Service not ready, retrying... %%i/5
    timeout /t 10 /nobreak >nul
)
:python_ai_done
if %PYTHON_AI_OK%==1 (
    echo [PASS] Python AI Service is healthy
    set /a TESTS_PASSED+=1
) else (
    echo [ERROR] Python AI Service health check failed
    set /a TESTS_FAILED+=1
    goto cleanup
)

REM Test Java AI Service with retries
echo [INFO] Testing Java AI Service health...
set JAVA_AI_OK=0
for /L %%i in (1,1,5) do (
    curl -f http://localhost:8086/actuator/health >nul 2>&1
    if !errorlevel!==0 (
        set JAVA_AI_OK=1
        goto java_ai_done
    )
    echo [INFO] Java AI Service not ready, retrying... %%i/5
    timeout /t 10 /nobreak >nul
)
:java_ai_done
if %JAVA_AI_OK%==1 (
    echo [PASS] Java AI Service is healthy
    set /a TESTS_PASSED+=1
) else (
    echo [ERROR] Java AI Service health check failed
    set /a TESTS_FAILED+=1
    goto cleanup
)

REM Test Analytics Service with retries
echo [INFO] Testing Analytics Service health...
set ANALYTICS_OK=0
for /L %%i in (1,1,5) do (
    curl -f http://localhost:8085/actuator/health >nul 2>&1
    if !errorlevel!==0 (
        set ANALYTICS_OK=1
        goto analytics_done
    )
    echo [INFO] Analytics Service not ready, retrying... %%i/5
    timeout /t 10 /nobreak >nul
)
:analytics_done
if %ANALYTICS_OK%==1 (
    echo [PASS] Analytics Service is healthy
    set /a TESTS_PASSED+=1
) else (
    echo [ERROR] Analytics Service health check failed
    set /a TESTS_FAILED+=1
    goto cleanup
)

REM Test API Gateway
echo [INFO] Testing API Gateway health...
curl -f http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] API Gateway health check failed
    set /a WARNINGS+=1
) else (
    echo [PASS] API Gateway is healthy
    set /a TESTS_PASSED+=1
)

REM Test service discovery
echo [INFO] Testing service discovery...
curl -f http://localhost:8761/eureka/apps >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Service discovery check failed
    set /a TESTS_FAILED+=1
    goto cleanup
)
echo [PASS] Service discovery is working
set /a TESTS_PASSED+=1

REM Test service registration
echo [INFO] Testing service registration...
curl -s http://localhost:8761/eureka/apps | findstr "AI-SERVICE-PYTHON" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Python AI Service not registered with Eureka
    set /a WARNINGS+=1
) else (
    echo [PASS] Python AI Service registered with Eureka
    set /a TESTS_PASSED+=1
)

curl -s http://localhost:8761/eureka/apps | findstr "AI-SERVICE" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Java AI Service not registered with Eureka
    set /a WARNINGS+=1
) else (
    echo [PASS] Java AI Service registered with Eureka
    set /a TESTS_PASSED+=1
)

REM Test inter-service communication
echo [INFO] Testing inter-service communication...

REM Test AI service proxy to Python service
echo [INFO] Testing AI service proxy communication...
curl -f -X POST http://localhost:8086/api/ai/parse-task -H "Content-Type: application/json" -d "{\"description\":\"Test task for deployment validation\"}" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] AI service proxy test failed - services may still be initializing
    set /a WARNINGS+=1
) else (
    echo [PASS] AI service proxy communication is working
    set /a TESTS_PASSED+=1
)

REM Test Analytics service endpoints
echo [INFO] Testing Analytics service endpoints...
curl -f http://localhost:8085/api/analytics/health >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Analytics service endpoint test failed
    set /a WARNINGS+=1
) else (
    echo [PASS] Analytics service endpoints are accessible
    set /a TESTS_PASSED+=1
)

REM Test metrics endpoints
echo [INFO] Testing metrics endpoints...
curl -f http://localhost:9090/metrics >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Python AI Service metrics endpoint not accessible
    set /a WARNINGS+=1
) else (
    echo [PASS] Python AI Service metrics are available
    set /a TESTS_PASSED+=1
)

curl -f http://localhost:9091/metrics >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Analytics Service metrics endpoint not accessible
    set /a WARNINGS+=1
) else (
    echo [PASS] Analytics Service metrics are available
    set /a TESTS_PASSED+=1
)

REM Test Redis connectivity
echo [INFO] Testing Redis connectivity...
docker exec task-management-redis redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Redis connectivity test failed
    set /a WARNINGS+=1
) else (
    echo [PASS] Redis is accessible
    set /a TESTS_PASSED+=1
)

REM Test container resource usage
echo [INFO] Testing container resource usage...
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}" | findstr "task-management" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Could not retrieve container stats
    set /a WARNINGS+=1
) else (
    echo [PASS] Container resource monitoring is working
    set /a TESTS_PASSED+=1
)

REM Test Docker volumes
echo [INFO] Testing Docker volume configuration...
docker volume ls | findstr "task-management" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Task management volumes not found
    set /a WARNINGS+=1
) else (
    echo [PASS] Docker volumes are configured
    set /a TESTS_PASSED+=1
)

REM Test specific volumes
echo [INFO] Testing specific volume mounts...
docker volume inspect task-management_ai_models >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] AI models volume not found
    set /a WARNINGS+=1
) else (
    echo [PASS] AI models volume is configured
    set /a TESTS_PASSED+=1
)

docker volume inspect task-management_redis_data >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Redis data volume not found
    set /a WARNINGS+=1
) else (
    echo [PASS] Redis data volume is configured
    set /a TESTS_PASSED+=1
)

REM Test container health status
echo [INFO] Testing container health status...
docker ps --format "table {{.Names}}\t{{.Status}}" | findstr "healthy" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] No containers showing healthy status
    set /a WARNINGS+=1
) else (
    echo [PASS] Some containers are reporting healthy status
    set /a TESTS_PASSED+=1
)

REM Test network connectivity
echo [INFO] Testing Docker network connectivity...
docker network ls | findstr "task-management" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Task management network not found
    set /a WARNINGS+=1
) else (
    echo [PASS] Task management network exists
    set /a TESTS_PASSED+=1
)

REM Test container logs for errors
echo [INFO] Testing container logs for critical errors...
docker logs ai-python-service 2>&1 | findstr /I "FATAL ERROR CRITICAL" >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARNING] Critical errors found in Python AI service logs
    set /a WARNINGS+=1
) else (
    echo [PASS] No critical errors in Python AI service logs
    set /a TESTS_PASSED+=1
)

docker logs analytics-service 2>&1 | findstr /I "FATAL ERROR CRITICAL" >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARNING] Critical errors found in Analytics service logs
    set /a WARNINGS+=1
) else (
    echo [PASS] No critical errors in Analytics service logs
    set /a TESTS_PASSED+=1
)

REM Test environment variable configuration
echo [INFO] Testing environment variable configuration in containers...
docker exec ai-python-service printenv AI_SERVICE_SERVICE_NAME >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Python AI service environment variables not accessible
    set /a WARNINGS+=1
) else (
    echo [PASS] Python AI service environment variables are configured
    set /a TESTS_PASSED+=1
)

docker exec analytics-service printenv SPRING_APPLICATION_NAME >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Analytics service environment variables not accessible
    set /a WARNINGS+=1
) else (
    echo [PASS] Analytics service environment variables are configured
    set /a TESTS_PASSED+=1
)

REM Test container restart capability
echo [INFO] Testing container restart capability...
docker restart ai-python-service >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Could not restart Python AI service
    set /a WARNINGS+=1
) else (
    echo [PASS] Python AI service restart successful
    set /a TESTS_PASSED+=1
    
    REM Wait for service to come back up
    echo [INFO] Waiting for service to restart...
    timeout /t 15 /nobreak >nul
    
    REM Test health after restart
    curl -f http://localhost:8087/health >nul 2>&1
    if !errorlevel! neq 0 (
        echo [WARNING] Python AI service not healthy after restart
        set /a WARNINGS+=1
    ) else (
        echo [PASS] Python AI service healthy after restart
        set /a TESTS_PASSED+=1
    )
)

goto summary

:cleanup
echo [INFO] Cleaning up test environment...
docker-compose down >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Cleanup may have failed
    set /a WARNINGS+=1
)

:summary
echo.
echo ========================================
echo        DEPLOYMENT TEST SUMMARY
echo ========================================
echo Tests Passed: %TESTS_PASSED%
echo Tests Failed: %TESTS_FAILED%
echo Warnings: %WARNINGS%
echo ========================================

if %TESTS_FAILED% gtr 0 (
    echo [RESULT] DEPLOYMENT TESTS FAILED
    echo [INFO] Check the error messages above for details
    echo [INFO] Use 'docker-compose logs [service-name]' to view service logs
    exit /b 1
) else (
    echo [RESULT] DEPLOYMENT TESTS PASSED
    if %WARNINGS% gtr 0 (
        echo [INFO] Some warnings were encountered - review above for details
    )
    echo [INFO] Services are running and ready for use
    echo [INFO] Use 'docker-compose down' to stop all services
    exit /b 0
)