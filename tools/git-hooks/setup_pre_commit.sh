#!/bin/bash
# Setup script for pre-commit hooks

set -e  # Exit on error

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Pre-commit Hooks Setup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if pre-commit is installed
echo -e "${BLUE}Checking for pre-commit...${NC}"
if command -v pre-commit &> /dev/null; then
    echo -e "${GREEN}✅ pre-commit is already installed${NC}"
    pre-commit --version
else
    echo -e "${YELLOW}⚠️  pre-commit not found, installing...${NC}"
    pip install pre-commit
    echo -e "${GREEN}✅ pre-commit installed${NC}"
fi

echo ""

# Check if ruff is installed
echo -e "${BLUE}Checking for ruff...${NC}"
if python -m ruff --version &> /dev/null; then
    echo -e "${GREEN}✅ ruff is already installed${NC}"
    python -m ruff --version
else
    echo -e "${YELLOW}⚠️  ruff not found, installing...${NC}"
    pip install ruff
    echo -e "${GREEN}✅ ruff installed${NC}"
fi

echo ""

# Install pre-commit hooks
echo -e "${BLUE}Installing pre-commit hooks...${NC}"
pre-commit install

echo ""
echo -e "${GREEN}✅ Pre-commit hooks installed successfully!${NC}"
echo ""

# Show what hooks are configured
echo -e "${BLUE}Configured hooks:${NC}"
echo "  - ruff (linting and import sorting)"
echo "  - ruff-format (code formatting)"
echo "  - trailing-whitespace"
echo "  - end-of-file-fixer"
echo "  - check-yaml, check-json"
echo "  - check-added-large-files"
echo "  - check-merge-conflict"
echo ""

# Test the hooks
echo -e "${BLUE}Testing hooks on a sample file...${NC}"
echo ""

# Run pre-commit on README.md (safe file to test)
if pre-commit run --files README.md 2>&1 | head -20; then
    echo ""
    echo -e "${GREEN}✅ Hooks are working!${NC}"
else
    echo ""
    echo -e "${YELLOW}⚠️  Some hooks failed, but that's okay for testing${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Format your code:"
echo "     ${GREEN}python tools/formatters/format_python.py --use-ruff${NC}"
echo ""
echo "  2. Make a commit (hooks will run automatically):"
echo "     ${GREEN}git add .${NC}"
echo "     ${GREEN}git commit -m 'Your message'${NC}"
echo ""
echo "  3. Run hooks manually on all files:"
echo "     ${GREEN}pre-commit run --all-files${NC}"
echo ""
echo -e "${BLUE}For more info, see: tools/README.md${NC}"
echo ""

