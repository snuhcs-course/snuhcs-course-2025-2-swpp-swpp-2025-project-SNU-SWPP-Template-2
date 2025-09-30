# Virtual Environment Activation Scripts

These scripts help you activate the Python virtual environment for the backend from any directory.

## Available Scripts

- **`activate_venv.ps1`** - PowerShell script (Windows)
- **`activate_venv.bat`** - Batch script (Windows Command Prompt)
- **`activate_venv.sh`** - Shell script (Linux/macOS/WSL)

## Usage

### From Project Root Directory
```bash
# PowerShell
.\backend\scripts\activate_venv.ps1

# Command Prompt
backend\scripts\activate_venv.bat

# Shell (Linux/macOS/WSL)
./backend/scripts/activate_venv.sh
```

### From Backend Directory
```bash
# PowerShell
.\scripts\activate_venv.ps1

# Command Prompt
scripts\activate_venv.bat

# Shell (Linux/macOS/WSL)
./scripts/activate_venv.sh
```

### From Backend/Scripts Directory
```bash
# PowerShell
.\activate_venv.ps1

# Command Prompt
activate_venv.bat

# Shell (Linux/macOS/WSL)
./activate_venv.sh
```

## What These Scripts Do

1. **Auto-detect** your current directory (project root, backend, or backend/scripts)
2. **Find** the virtual environment automatically
3. **Activate** the virtual environment
4. **Display** the Python interpreter path
5. **Keep** the terminal session active with the virtual environment

## For VSCode Users

After running any of these scripts, the Python interpreter path will be displayed. You can use this path to manually set the Python interpreter in VSCode:

1. Open Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`)
2. Type "Python: Select Interpreter"
3. Choose "Enter interpreter path..."
4. Paste the displayed path

## Troubleshooting

If you get an error about the virtual environment not being found:

1. Make sure you're in one of the supported directories
2. Create the virtual environment if it doesn't exist:
   ```bash
   cd backend
   python -m venv venv
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements-dev.txt
   ```
