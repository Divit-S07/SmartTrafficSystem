Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host " Vegam City Smart Traffic Management & Emergency Routing System" -ForegroundColor Green
Write-Host " Pure Java SE Backend (Zero Frameworks) + HTML/CSS/JS Frontend" -ForegroundColor Yellow
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Compiling SmartTrafficSystem.java..." -ForegroundColor Gray
javac SmartTrafficSystem.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Compilation failed. Please ensure JDK 11+ is installed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Compilation successful!" -ForegroundColor Green
Write-Host ""
Write-Host "Starting Web Server at http://localhost:8080 ..." -ForegroundColor Cyan
Write-Host "Open http://localhost:8080 in your web browser!" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Gray
Write-Host ""

java SmartTrafficSystem

