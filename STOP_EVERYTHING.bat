@echo off
echo ========================================
echo    STOPPING MICROSERVICES PROJECT
echo ========================================
echo.
echo This will stop all running services and free up your computer resources.
echo.
pause

echo Stopping all services...
docker-compose down

echo.
echo ✅ All services stopped!
echo Your computer resources are now free.
echo.
echo To start again, just run: START_EVERYTHING.bat
echo.
pause