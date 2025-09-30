@echo off
REM Batch script to activate virtual environment and set Python path
REM Run this script to use the virtual environment Python

echo Activating virtual environment...

REM Determine if we're in project root or backend directory
if exist "backend\venv" (
    REM We're in project root
    set VENV_PATH=backend\venv
    echo Running from project root directory
) else if exist "venv" (
    REM We're in backend directory
    set VENV_PATH=venv
    echo Running from backend directory
) else if exist "..\venv" (
    REM We're in backend/scripts directory
    set VENV_PATH=..\venv
    echo Running from backend/scripts directory
) else (
    echo Error: Virtual environment not found!
    echo Please run this script from either:
    echo   - Project root directory ^(where backend\ folder exists^)
    echo   - Backend directory ^(where venv\ folder exists^)
    echo   - Backend\scripts directory
    pause
    exit /b 1
)

REM Check if virtual environment exists
if not exist "%VENV_PATH%\Scripts\activate.bat" (
    echo Error: Virtual environment activation script not found at %VENV_PATH%\Scripts\activate.bat
    echo Please create the virtual environment first:
    echo   cd backend ^&^& python -m venv venv
    pause
    exit /b 1
)

REM Activate the virtual environment
call "%VENV_PATH%\Scripts\activate.bat"

REM Display current Python path
echo Python interpreter path:
python -c "import sys; print(sys.executable)"

echo Virtual environment activated successfully!
echo You can now use 'python' command with the virtual environment.
echo.
echo To deactivate, run: deactivate

REM Keep the command prompt open
cmd /k
