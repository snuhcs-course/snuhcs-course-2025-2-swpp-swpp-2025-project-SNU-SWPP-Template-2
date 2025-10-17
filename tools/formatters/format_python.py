#!/usr/bin/env python3
"""
Python Code Formatter Script

This script formats Python code using black/ruff, isort/ruff, and runs
flake8/ruff for linting. It can be used as a standalone script or
integrated into pre-commit hooks.

Usage:
    # Using ruff (recommended - faster)
    python tools/formatters/format_python.py --use-ruff [--check] [paths...]

    # Using traditional tools (black, isort, flake8)
    python tools/formatters/format_python.py [--check] [--diff] [paths...]

Arguments:
    --check: Don't write back modified files, just return status
    --diff: Show diffs instead of rewriting files
    --use-ruff: Use ruff instead of black+isort+flake8 (recommended)
    --base-dirs: Base directories to search (default: backend, ai-model)
    paths: Specific paths to format (default: auto-discover Python
           directories in backend/ and ai-model/)

For more information, see: tools/README.md
"""

import argparse
import subprocess
import sys
from pathlib import Path


class PythonFormatter:
    """Python code formatter using black, isort/ruff, and flake8/ruff."""

    def __init__(
        self,
        check_mode: bool = False,
        show_diff: bool = False,
        use_ruff: bool = False,
    ):
        self.check_mode = check_mode
        self.show_diff = show_diff
        self.use_ruff = use_ruff
        self.project_root = Path(__file__).parent.parent.parent

    def discover_python_directories(
        self, base_dirs: list[str] | None = None
    ) -> list[str]:
        """
        Auto-discover directories containing Python files.

        Args:
            base_dirs: Base directories to search in
            (default: ['backend', 'ai-model'])

        Returns:
            List of directory paths containing Python files
        """
        if base_dirs is None:
            base_dirs = ["backend", "ai-model"]

        # Directories to exclude from discovery
        exclude_patterns = {
            ".venv",
            "venv",
            "__pycache__",
            ".git",
            "node_modules",
            ".pytest_cache",
            ".mypy_cache",
            "migrations",
        }

        discovered_paths = set()

        for base_dir in base_dirs:
            base_path = self.project_root / base_dir
            if not base_path.exists():
                continue

            # Find all directories containing .py files
            for py_file in base_path.rglob("*.py"):
                # Skip files in excluded directories
                if any(
                    excluded in py_file.parts
                    for excluded in exclude_patterns
                ):
                    continue

                # Get the parent directory
                parent = py_file.parent
                relative_path = parent.relative_to(self.project_root)
                parts = relative_path.parts

                if len(parts) >= 2 and parts[0] in base_dirs:
                    app_dir = f"{parts[0]}/{parts[1]}"
                    discovered_paths.add(app_dir)

        return sorted(list(discovered_paths))

    def run_command(self, cmd: list[str], description: str) -> bool:
        """Run a command and return True if successful."""
        print(f"Running {description}...")
        try:
            result = subprocess.run(
                cmd,
                cwd=self.project_root,
                capture_output=True,
                text=True,
                check=False,
            )

            if result.stdout:
                print(result.stdout)
            if result.stderr:
                print(result.stderr, file=sys.stderr)

            return result.returncode == 0
        except FileNotFoundError:
            print("Error: Command not found. Please install the required tools.")
            return False

    def format_with_black(self, paths: list[str]) -> bool:
        """Format code with black."""
        cmd = ["python", "-m", "black", "--line-length", "79"]

        if self.check_mode:
            cmd.append("--check")
        if self.show_diff:
            cmd.append("--diff")

        cmd.extend(paths)
        return self.run_command(cmd, "black formatting")

    def sort_imports_with_isort(self, paths: list[str]) -> bool:
        """Sort imports with isort."""
        cmd = ["python", "-m", "isort"]

        if self.check_mode:
            cmd.append("--check-only")
        if self.show_diff:
            cmd.append("--diff")

        cmd.extend(paths)
        return self.run_command(cmd, "isort import sorting")

    def lint_with_flake8(self, paths: list[str]) -> bool:
        """Lint code with flake8."""
        cmd = [
            "python",
            "-m",
            "flake8",
            "--exclude=.venv,venv,__pycache__,.git,node_modules",
        ] + paths
        return self.run_command(cmd, "flake8 linting")

    def check_with_ruff(self, paths: list[str]) -> bool:
        """
        Lint and check code with ruff.
        Ruff can replace both flake8 and isort.
        Uses root pyproject.toml config if available.
        Automatically applies fixes when not in check mode.
        """
        cmd = ["python", "-m", "ruff", "check"]

        # Check if root pyproject.toml exists
        root_config = self.project_root / "pyproject.toml"
        if root_config.exists():
            # Use root config - it has all the settings including excludes
            cmd.extend(["--config", str(root_config)])
        else:
            # Fallback to command-line options
            cmd.extend(["--line-length", "79"])
            cmd.extend(
                [
                    "--exclude",
                    ".venv,venv,__pycache__,.git,node_modules,"
                    ".pytest_cache,.mypy_cache,migrations",
                ]
            )

        if not self.check_mode:
            # In format mode, automatically apply fixes
            cmd.append("--fix")

        if self.show_diff:
            cmd.append("--diff")

        cmd.extend(paths)
        return self.run_command(cmd, "ruff linting and checking")

    def format_with_ruff(self, paths: list[str]) -> bool:
        """
        Format code with ruff (alternative to black).
        Uses root pyproject.toml config if available.
        Automatically formats code when not in check mode.
        """
        cmd = ["python", "-m", "ruff", "format"]

        # Check if root pyproject.toml exists
        root_config = self.project_root / "pyproject.toml"
        if root_config.exists():
            # Use root config - includes excludes and formatting rules
            cmd.extend(["--config", str(root_config)])
        else:
            # Fallback to command-line options
            cmd.extend(["--line-length", "79"])

        if self.check_mode:
            cmd.append("--check")

        if self.show_diff:
            cmd.append("--diff")

        cmd.extend(paths)
        return self.run_command(cmd, "ruff formatting")



    def security_check_with_bandit(self, paths: list[str]) -> bool:
        """Security check with bandit."""
        # Filter to only source directories for bandit (exclude tests)
        src_paths = [
            p
            for p in paths
            if not any(x in p for x in ["tests", "test_", "migrations"])
        ]
        if not src_paths:
            print(
                "No source paths found for bandit security check, "
                "skipping..."
            )
            return True

        # Run bandit with severity level set to medium and high only
        cmd = [
            "python",
            "-m",
            "bandit",
            "-r",
            "-ll",  # Only report medium and high severity
        ] + src_paths
        return self.run_command(cmd, "bandit security checking")

    def format_all(self, paths: list[str]) -> bool:
        """Run all formatting and checking tools."""
        success = True

        # Filter existing paths
        existing_paths = []
        for path in paths:
            path_obj = self.project_root / path
            if path_obj.exists():
                existing_paths.append(path)
            else:
                print(f"Warning: Path {path} does not exist, skipping...")

        if not existing_paths:
            print("No valid paths found to format")
            return False

        print(f"Formatting Python code in: {', '.join(existing_paths)}")
        if self.use_ruff:
            print("Using ruff for linting and formatting")
        print("=" * 60)

        if self.use_ruff:
            # Use ruff for both linting and formatting
            # Ruff check handles import sorting and linting
            ruff_check_result = self.check_with_ruff(existing_paths)
            if self.check_mode:
                if ruff_check_result:
                    print("✅ Ruff linting and import checks passed")
                else:
                    print("❌ Ruff linting and import checks failed")
                    success = False
            else:
                if not ruff_check_result:
                    success = False

            # Ruff format handles code formatting
            ruff_format_result = self.format_with_ruff(existing_paths)
            if self.check_mode:
                if ruff_format_result:
                    print("✅ Ruff formatting check passed")
                else:
                    print("❌ Ruff formatting check failed")
                    success = False
            else:
                if not ruff_format_result:
                    success = False
        else:
            # Use traditional tools: isort, black, flake8
            # Run formatters (only if not in check mode)
            if not self.check_mode:
                if not self.sort_imports_with_isort(existing_paths):
                    success = False

                if not self.format_with_black(existing_paths):
                    success = False
            else:
                # In check mode, run checks
                if not self.sort_imports_with_isort(existing_paths):
                    print("❌ Import sorting check failed")
                    success = False
                else:
                    print("✅ Import sorting check passed")

                if not self.format_with_black(existing_paths):
                    print("❌ Black formatting check failed")
                    success = False
                else:
                    print("✅ Black formatting check passed")

            # Always run linting
            if not self.lint_with_flake8(existing_paths):
                print("❌ Flake8 linting failed")
                success = False
            else:
                print("✅ Flake8 linting passed")

        # Run security checking (warnings only, not fatal)
        bandit_result = self.security_check_with_bandit(existing_paths)
        if bandit_result:
            print("✅ Bandit security check passed")
        else:
            print(
                "⚠️  Bandit found potential security issues "
                "(review above)"
            )
            # Don't fail the build for bandit warnings
            # success = False

        print("=" * 60)
        if success:
            print("🎉 All Python formatting and checks passed!")
        else:
            print("💥 Some Python formatting or checks failed!")

        return success


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description=(
            "Format Python code using black/ruff, isort/ruff, "
            "and run quality checks"
        )
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Don't write back modified files, just return status",
    )
    parser.add_argument(
        "--diff",
        action="store_true",
        help="Show diffs instead of rewriting files",
    )
    parser.add_argument(
        "--use-ruff",
        action="store_true",
        help=(
            "Use ruff instead of black+isort+flake8 "
            "(faster, modern alternative)"
        ),
    )
    parser.add_argument(
        "--base-dirs",
        nargs="*",
        default=None,
        help=(
            "Base directories to search for Python files "
            "(default: backend, ai-model)"
        ),
    )
    parser.add_argument(
        "paths",
        nargs="*",
        default=None,
        help=(
            "Specific paths to format. If not provided, "
            "auto-discovers directories with Python files"
        ),
    )

    args = parser.parse_args()

    formatter = PythonFormatter(
        check_mode=args.check,
        show_diff=args.diff,
        use_ruff=args.use_ruff,
    )

    # Use provided paths or auto-discover
    if args.paths:
        paths = args.paths
    else:
        print("Auto-discovering Python directories...")
        paths = formatter.discover_python_directories(args.base_dirs)
        if not paths:
            print("No Python directories found!")
            sys.exit(1)
        print(f"Found {len(paths)} directories with Python files")

    success = formatter.format_all(paths)

    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
