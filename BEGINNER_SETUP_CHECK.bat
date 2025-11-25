@echo off
echo ========================================
echo    MICROSERVICES SETUP CHECKER
echo ========================================
echo.

echo Checking Java...
java -version
if %errorlevel% neq 0 (
    echo ❌ Java not found! Please install Java 17 or higher
    echo Download from: https://adoptium.net/
) else (
    echo ✅ Java is installed!
)
echo.

echo Checking Maven...
mvn -version
if %errorlevel% neq 0 (
    echo ❌ Maven not found! Please install Maven
    echo Download from: https://maven.apache.org/download.cgi
) else (
    echo ✅ Maven is installed!
)
echo.

echo Checking Docker...
docker --version
if %errorlevel% neq 0 (
    echo ❌ Docker not found! Please install Docker Desktop
    echo Download from: https://www.docker.com/products/docker-desktop
) else (
    echo ✅ Docker is installed!
)
echo.

echo Checking Docker Compose...
docker-compose --version
if %errorlevel% neq 0 (
    echo ❌ Docker Compose not found!
) else (
    echo ✅ Docker Compose is installed!
)
echo.

echo ========================================
echo If you see any ❌, please install the missing software first!
echo If all show ✅, you're ready to run the project!
echo ========================================
pause