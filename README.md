# SWPP AI Application

An intelligent mobile application powered by AI models, built for the SNU Software Practice (SWPP) course.

![Build Status](https://github.com/snuhcs-course/swpp-2025-project-team-10/workflows/CI/badge.svg)
![Python Version](https://img.shields.io/badge/python-3.8%2B-blue)
![Kotlin](https://img.shields.io/badge/kotlin-1.9%2B-orange)
![License](https://img.shields.io/badge/license-MIT-green)

## 🚀 Quick Start

```bash
# Clone the repository
git clone https://github.com/snuhcs-course/swpp-2025-project-team-10.git
cd swpp-2025-project-team-**10**

# Set up development environment
./scripts/setup/setup_dev_environment.sh

# Activate virtual environment
source .venv/bin/activate

# Start development
make dev
```

## 📱 Features

- **AI-Powered Intelligence**: Advanced machine learning models for intelligent predictions
- **Cross-Platform Mobile App**: Native Android application built with Kotlin
- **Real-time Processing**: Fast API backend with real-time data processing
- **Scalable Architecture**: Microservices-based design for scalability
- **Developer-Friendly**: Comprehensive tooling and documentation

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   AI Model     │
│   (Kotlin)      │◄──►│   (FastAPI)     │◄──►│   (PyTorch)     │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile UI     │    │   PostgreSQL    │    │   Model Store   │
│                 │    │   Database      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🛠️ Technology Stack

### Backend
- **Python 3.8+** with FastAPI framework
- **PostgreSQL** for data persistence
- **Redis** for caching and message queuing
- **Celery** for background task processing
- **SQLAlchemy** for ORM

### Frontend
- **Kotlin** for Android development
- **Android SDK** (API level 21+)
- **Jetpack Compose** for modern UI
- **Retrofit** for API communication

### AI/ML
- **PyTorch** for deep learning models
- **Transformers** for NLP capabilities
- **NumPy & Pandas** for data processing
- **Jupyter** for model development

### DevOps
- **Docker** for containerization
- **GitHub Actions** for CI/CD
- **Nginx** for reverse proxy
- **Prometheus & Grafana** for monitoring

## 📋 Prerequisites

- **Python 3.8+**
- **Java 11+** (for Android development)
- **Android Studio** with Android SDK
- **PostgreSQL 12+**
- **Git**
- **Docker** (optional, for containerized deployment)

## 🚀 Getting Started

### 1. Choose Your Workspace (VS Code Users)

We provide role-specific VS Code workspace configurations:

```bash
# For backend/Python developers
code workspace/backend.code-workspace

# For frontend/Android developers
code workspace/frontend.code-workspace
```

See [workspace/README.md](workspace/README.md) for details.

### 2. Environment Setup

```bash
# Clone repository
git clone https://github.com/snuhcs-course/swpp-2025-project-team-10.git
cd swpp-2025-project-team-10

# Run automated setup
./scripts/setup/setup_dev_environment.sh
```

### 3. Configuration

```bash
# Copy environment files
cp backend/.env.example backend/.env
cp ai-model/.env.example ai-model/.env

# Edit configuration files with your settings
nano backend/.env
nano ai-model/.env
```

### 4. Database Setup

```bash
# Create database
createdb swpp_ai_app_dev

# Run migrations
cd backend
alembic upgrade head
```

### 5. Start Development

```bash
# Start backend
make run-backend

# Build and install mobile app
make run-frontend
```

## 📖 Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[Development Setup](docs/development/SETUP.md)** - Complete setup guide
- **[Coding Conventions](docs/development/CONVENTIONS.md)** - Code standards and best practices
- **[Git Workflow](docs/development/GIT_WORKFLOW.md)** - Branching strategy and workflow
- **[Testing Guide](docs/development/TESTING.md)** - Testing practices and guidelines
- **[API Documentation](docs/api/)** - Backend and AI model APIs
- **[Deployment Guide](docs/deployment/)** - Production deployment instructions

## 🧪 Testing

```bash
# Run all tests
make test

# Run specific test suites
make test-python    # Python backend and AI model tests
make test-kotlin    # Kotlin frontend tests

# Run with coverage
make test-coverage
```

## 🎨 Code Quality

We maintain high code quality standards with automated formatting and linting:

```bash
# Format all code
make format

# Check code quality
make lint

# Run security checks
make security-check
```

### Pre-commit Hooks

Pre-commit hooks automatically run code formatters and checks:

- **Python**: Black, isort, flake8, mypy, bandit
- **Kotlin**: ktlint, detekt
- **General**: trailing whitespace, file endings, merge conflicts

## 🚀 Deployment

### Development
```bash
# Start all services locally
make dev
```

### Docker
```bash
# Build and run with Docker Compose
make docker-build
make docker-run
```

### Production
See [Deployment Guide](docs/deployment/DEPLOYMENT.md) for production deployment instructions.

## 🤝 Contributing

We follow a structured development workflow:

1. **Read the documentation**: Start with [CONVENTIONS.md](docs/development/CONVENTIONS.md)
2. **Create feature branch**: `git checkout -b feature/your-feature-name`
3. **Follow coding standards**: Use provided formatters and linters
4. **Write tests**: Maintain test coverage above 80%
5. **Create pull request**: Follow the PR template
6. **Code review**: Get approval from team members
7. **Merge to dev**: Integration testing in dev branch
8. **Deploy to main**: Production deployment from main branch

### Branch Strategy
- `main`: Production-ready code
- `dev`: Integration branch for features
- `feature/*`: New features and enhancements
- `bugfix/*`: Bug fixes
- `hotfix/*`: Critical production fixes

## 📊 Project Status

- ✅ Project structure and tooling setup
- ✅ Development environment configuration
- ✅ Code quality tools and pre-commit hooks
- ✅ Documentation and conventions
- 🚧 Backend API development
- 🚧 AI model implementation
- 🚧 Frontend mobile application
- ⏳ Integration testing
- ⏳ Production deployment

## 📞 Support

- **Documentation**: Check the `docs/` directory first
- **Issues**: Create a GitHub issue with detailed information
- **Discussions**: Use GitHub Discussions for questions
- **Team**: Contact the development team for urgent matters

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **SNU SWPP Course** for the project framework
- **Open Source Libraries** used throughout the project
- **Team Members** for their contributions and collaboration

---

**Happy Coding!** 🎉
