# Starts services in dependency order and waits for real readiness.
$root = $PSScriptRoot
$backendHealth = "http://localhost:8082/api/health"
$frontendUrl = "http://localhost:8081"

function Test-Http([string]$Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

Write-Host "Prakash Bankers - startup check" -ForegroundColor Cyan

if (-not (Test-Http $backendHealth)) {
    $listener = Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Host "Port 8082 is occupied, but it is not a healthy Prakash Bankers API." -ForegroundColor Red
        Write-Host "Stop PID $($listener.OwningProcess) and run this script again." -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Starting backend on 8082..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\backend'; .\run-backend.ps1"

    $ready = $false
    for ($i = 1; $i -le 90; $i++) {
        if (Test-Http $backendHealth) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) {
        Write-Host "Backend did not become ready within 90 seconds." -ForegroundColor Red
        Write-Host "Check the backend terminal for a MySQL, password, schema, or port error." -ForegroundColor Yellow
        exit 1
    }
}
Write-Host "Backend ready: $backendHealth" -ForegroundColor Green

if (-not (Test-Http $frontendUrl)) {
    $listener = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Host "Port 8081 is occupied by another process (PID $($listener.OwningProcess))." -ForegroundColor Red
        exit 1
    }
    Write-Host "Starting frontend on 8081..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\frontend'; .\run-frontend.ps1"

    for ($i = 1; $i -le 45; $i++) {
        if (Test-Http $frontendUrl) { break }
        Start-Sleep -Seconds 1
    }
}

if (Test-Http $frontendUrl) {
    Write-Host "Application ready: $frontendUrl" -ForegroundColor Green
    Write-Host "Login: admin / admin123" -ForegroundColor Cyan
} else {
    Write-Host "Frontend did not become ready. Check its terminal output." -ForegroundColor Red
    exit 1
}
