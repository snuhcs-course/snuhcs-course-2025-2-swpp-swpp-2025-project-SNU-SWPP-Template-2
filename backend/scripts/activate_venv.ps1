# PowerShell script to activate virtual environment and set Python path
# Run this script to use the virtual environment Python

Write-Host "Activating virtual environment..." -ForegroundColor Green

# Determine if we're in project root or backend directory
if (Test-Path "backend\venv") {
    # We're in project root
    $VenvPath = "backend\venv"
    Write-Host "Running from project root directory" -ForegroundColor Cyan
} elseif (Test-Path "venv") {
    # We're in backend directory
    $VenvPath = "venv"
    Write-Host "Running from backend directory" -ForegroundColor Cyan
} elseif (Test-Path "..\venv") {
    # We're in backend/scripts directory
    $VenvPath = "..\venv"
    Write-Host "Running from backend/scripts directory" -ForegroundColor Cyan
} else {
    Write-Host "Error: Virtual environment not found!" -ForegroundColor Red
    Write-Host "Please run this script from either:" -ForegroundColor Yellow
    Write-Host "  - Project root directory (where backend\ folder exists)" -ForegroundColor Yellow
    Write-Host "  - Backend directory (where venv\ folder exists)" -ForegroundColor Yellow
    Write-Host "  - Backend\scripts directory" -ForegroundColor Yellow
    exit 1
}

# Check if virtual environment exists
if (-not (Test-Path "$VenvPath\Scripts\Activate.ps1")) {
    Write-Host "Error: Virtual environment activation script not found at $VenvPath\Scripts\Activate.ps1" -ForegroundColor Red
    Write-Host "Please create the virtual environment first:" -ForegroundColor Yellow
    Write-Host "  cd backend && python -m venv venv" -ForegroundColor Yellow
    exit 1
}

# Activate the virtual environment
& "$VenvPath\Scripts\Activate.ps1"

# Display current Python path
Write-Host "Python interpreter path:" -ForegroundColor Yellow
python -c "import sys; print(sys.executable)"

Write-Host "Virtual environment activated successfully!" -ForegroundColor Green
Write-Host "You can now use 'python' command with the virtual environment." -ForegroundColor Cyan
Write-Host ""
Write-Host "To deactivate, run: deactivate" -ForegroundColor Magenta
