@echo off
REM Aegis Demo Application Launcher for Windows

echo ================================================================
echo    Aegis Authentication SDK - Demo Application
echo ================================================================
echo.

REM Check Java
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed. Please install Java 21 or later.
    pause
    exit /b 1
)

echo Java is installed
echo.

REM Check Maven
echo Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven is not installed. Please install Maven 3.8+
    pause
    exit /b 1
)

echo Maven is installed
echo.

REM Build the project
echo Building the project...
echo This may take a few minutes on first run...
echo.

call mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo ERROR: Build failed. Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo Build successful!
echo.

REM Run the application
echo Starting Aegis Demo Application...
echo ================================================================
echo.
echo Access the demo UI at: http://localhost:8080
echo.
echo Test Credentials:
echo   Username: demo   ^| Password: password   ^| Roles: USER
echo   Username: alice  ^| Password: alice123  ^| Roles: USER, MANAGER
echo   Username: admin  ^| Password: admin123  ^| Roles: USER, ADMIN
echo.
echo API Keys:
echo   demo-key-alice-12345      (alice - USER)
echo   demo-key-admin-67890      (admin - ADMIN)
echo   demo-key-service-abc123   (service-bot - SERVICE)
echo.
echo Press Ctrl+C to stop the application
echo ================================================================
echo.

java -jar target\demo-app-1.0.0-SNAPSHOT.jar

pause
