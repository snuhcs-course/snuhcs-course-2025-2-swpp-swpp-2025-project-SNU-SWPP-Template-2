# Development Tools

This directory contains development tools for code formatting, linting, and automation.

---

## 📁 Directory Structure

```
tools/
├── README.md                    # This file
├── formatters/                  # Code formatting tools
│   ├── format_python.py         # Python formatter (ruff/black/isort)
│   ├── format_kotlin.sh         # Kotlin formatter (ktlint/detekt)
│   └── format_all.sh            # Run all formatters
├── git-hooks/                   # Git hook setup scripts
│   └── setup_pre_commit.sh      # Install pre-commit hooks
└── deployment/                  # Deployment scripts (future)
```

---

## 🚀 Quick Start

### 1. Setup Pre-commit Hooks (Recommended)

```bash
# One-time setup - installs git hooks that run automatically
bash tools/git-hooks/setup_pre_commit.sh
```

### 2. Format Code Manually

```bash
# Format Python code (fast, auto-fixes issues)
python tools/formatters/format_python.py --use-ruff

# Format Kotlin code
bash tools/formatters/format_kotlin.sh

# Format all code (Python + Kotlin)
bash tools/formatters/format_all.sh
```

---

## 🛠️ Tools Overview

### **Python Formatter** (`formatters/format_python.py`)

Formats and lints Python code using ruff (recommended) or traditional tools.

**Features:**
- ✅ Auto-discovery of Python files
- ✅ Auto-fix formatting and linting issues
- ✅ Excludes migrations automatically
- ✅ 10-100x faster with ruff
- ✅ Security scanning with bandit

**Usage:**
```bash
# Auto-fix all issues (recommended)
python tools/formatters/format_python.py --use-ruff

# Check without modifying files
python tools/formatters/format_python.py --use-ruff --check

# Format specific directory
python tools/formatters/format_python.py --use-ruff backend/accounts

# Show what would change
python tools/formatters/format_python.py --use-ruff --diff
```

**Options:**
- `--use-ruff` - Use ruff instead of black+isort+flake8 (faster)
- `--check` - Don't modify files, just report issues
- `--diff` - Show diffs of what would change
- `--base-dirs DIR [DIR ...]` - Base directories to search (default: backend, ai-model)

**What it checks:**
- Code formatting (line length, indentation, quotes)
- Import sorting
- Linting (PEP 8, unused imports, Django-specific)
- Security issues (bandit)

---

### **Kotlin Formatter** (`formatters/format_kotlin.sh`)

Formats and analyzes Kotlin code using ktlint and detekt.

**Features:**
- ✅ Auto-formatting with ktlint
- ✅ Static analysis with detekt
- ✅ Auto-downloads tools if missing
- ✅ Generates HTML reports

**Usage:**
```bash
# Auto-fix all issues
bash tools/formatters/format_kotlin.sh

# Check without modifying files
bash tools/formatters/format_kotlin.sh --check

# Format specific directory
bash tools/formatters/format_kotlin.sh frontend/src/main
```

**Options:**
- `--check` - Don't modify files, just report issues
- `--diff` - Show diffs of what would change
- `paths` - Specific paths to format (default: frontend/src)

**What it checks:**
- Code formatting (Kotlin style guide)
- Code smells and complexity (detekt)
- Best practices

---

### **Universal Formatter** (`formatters/format_all.sh`)

Runs all formatters (Python + Kotlin) in one command.

**Usage:**
```bash
# Format all code
bash tools/formatters/format_all.sh

# Check all code
bash tools/formatters/format_all.sh --check
```

---

### **Pre-commit Setup** (`git-hooks/setup_pre_commit.sh`)

Installs git hooks that run automatically before each commit.

**Features:**
- ✅ Checks and installs pre-commit if needed
- ✅ Checks and installs ruff if needed
- ✅ Installs git hooks
- ✅ Tests the hooks
- ✅ Shows next steps

**Usage:**
```bash
# One-time setup
bash tools/git-hooks/setup_pre_commit.sh

# Now hooks run automatically on every commit!
git add .
git commit -m "Your message"
```

**Hooks that run:**
1. `ruff` - Python linting and import sorting (auto-fixes)
2. `ruff-format` - Python code formatting
3. `trailing-whitespace` - Remove trailing whitespace
4. `end-of-file-fixer` - Ensure files end with newline
5. `check-yaml`, `check-json` - Validate config files
6. `check-added-large-files` - Prevent large files
7. `check-merge-conflict` - Detect merge conflicts

---

## 📋 Common Workflows

### **Daily Development**

```bash
# 1. Make code changes
vim backend/accounts/views.py

# 2. Format code
python tools/formatters/format_python.py --use-ruff

# 3. Commit (pre-commit hooks run automatically)
git add .
git commit -m "feat: add new feature"
```

### **Before Creating PR**

```bash
# Format entire codebase
bash tools/formatters/format_all.sh

# Or run pre-commit on all files
pre-commit run --all-files

# Commit the changes
git add .
git commit -m "style: format codebase"
```

### **Fix Formatting Issues**

```bash
# Check what needs fixing
python tools/formatters/format_python.py --use-ruff --check

# Auto-fix everything
python tools/formatters/format_python.py --use-ruff

# Verify fixes
python tools/formatters/format_python.py --use-ruff --check
```

---

## ⚙️ Configuration

### **Python Configuration**

**Root config** (`pyproject.toml`):
- Shared formatting rules for all Python code
- Ruff, black, isort configuration
- Line length: 79 characters
- Excludes: migrations, .venv, cache directories

**Backend config** (`backend/pyproject.toml`):
- Backend-specific dependencies
- Django, DRF, pytest, etc.

### **Kotlin Configuration**

**Frontend config** (`frontend/build.gradle.kts`):
- Kotlin version and dependencies
- ktlint and detekt plugins

### **Pre-commit Configuration**

**Config file** (`.pre-commit-config.yaml`):
- Hook definitions and versions
- Arguments and file patterns
- Exclusion patterns

---

## 🔍 Troubleshooting

### **"No module named ruff"**

```bash
# Install ruff
pip install ruff

# Or with uv
cd backend && uv pip install ruff
```

### **"Pre-commit not installed"**

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install
```

### **Hooks are slow**

```bash
# Skip hooks for one commit (not recommended)
git commit --no-verify -m "Quick fix"

# Or disable slow hooks in .pre-commit-config.yaml
# (bandit is already commented out by default)
```

### **Want to see what changed**

```bash
# Python
python tools/formatters/format_python.py --use-ruff --diff

# Kotlin
bash tools/formatters/format_kotlin.sh --diff
```

---

## 📊 Performance

### **Python Formatting**

- **Traditional tools** (black+isort+flake8): ~3-5 seconds
- **Ruff**: ~0.5 seconds
- **Speed improvement**: 6-10x faster! ⚡

### **Pre-commit Hooks**

- **Fast hooks** (ruff, trailing-whitespace): <1 second
- **Slow hooks** (bandit): 1-2 seconds (disabled by default)

---

## 🎯 Best Practices

1. ✅ **Use ruff for Python** - It's 10-100x faster than traditional tools
2. ✅ **Format before committing** - Run formatter manually first
3. ✅ **Install pre-commit hooks** - Safety net for forgotten formatting
4. ✅ **Run on all files periodically** - Keep codebase consistent
5. ✅ **Don't edit migrations** - They're auto-generated and excluded
6. ✅ **Review changes** - Use `--diff` to see what will change
7. ❌ **Don't skip hooks** - Unless absolutely necessary

---

## 📚 Additional Resources

- **Ruff Documentation**: https://docs.astral.sh/ruff/
- **Pre-commit Documentation**: https://pre-commit.com/
- **ktlint Documentation**: https://pinterest.github.io/ktlint/
- **detekt Documentation**: https://detekt.dev/

---

## 🆘 Getting Help

If you encounter issues:

1. Check this README
2. Check `.pre-commit-config.yaml` for hook configuration
3. Check `pyproject.toml` for Python configuration
4. Ask the team in Slack/Discord

---

**Last Updated**: 2025-01-17

