@echo off
title Vegam City Smart Traffic Management System
echo ============================================================================
echo  Starting Vegam City Smart Traffic System (Zero-Framework Pure Java SE)
echo ============================================================================
echo.
echo Compiling SmartTrafficSystem.java...
javac SmartTrafficSystem.java
if %errorlevel% neq 0 (
    echo [!] Compilation failed. Please ensure JDK 11+ is installed.
    pause
    exit /b %errorlevel%
)
echo Compilation successful.
echo.
echo Starting Web Server on http://localhost:8080 ...
echo Press Ctrl+C in this terminal window to stop the server.
echo.
java SmartTrafficSystem
pause

