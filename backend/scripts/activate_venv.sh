#!/bin/bash
# Shell script to activate virtual environment and set Python path
# Run this script to use the virtual environment Python

echo "Activating virtual environment..."

# Determine if we're in project root or backend directory
if [ -d "backend/venv" ]; then
    # We're in project root
    VENV_PATH="backend/venv"
    echo "Running from project root directory"
elif [ -d "venv" ]; then
    # We're in backend directory
    VENV_PATH="venv"
    echo "Running from backend directory"
elif [ -d "../venv" ]; then
    # We're in backend/scripts directory
    VENV_PATH="../venv"
    echo "Running from backend/scripts directory"
else
    echo "Error: Virtual environment not found!"
    echo "Please run this script from either:"
    echo "  - Project root directory (where backend/ folder exists)"
    echo "  - Backend directory (where venv/ folder exists)"
    echo "  - Backend/scripts directory"
    exit 1
fi

# Check if virtual environment exists
if [ ! -f "$VENV_PATH/bin/activate" ]; then
    echo "Error: Virtual environment activation script not found at $VENV_PATH/bin/activate"
    echo "Please create the virtual environment first:"
    echo "  cd backend && python -m venv venv"
    exit 1
fi

# Activate the virtual environment
source "$VENV_PATH/bin/activate"

# Display current Python path
echo "Python interpreter path:"
python -c "import sys; print(sys.executable)"

echo "Virtual environment activated successfully!"
echo "You can now use 'python' command with the virtual environment."
echo ""
echo "To deactivate, run: deactivate"
