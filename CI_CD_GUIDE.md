# CI/CD Guide for LeechText

This guide explains the Continuous Integration and Continuous Deployment (CI/CD) pipeline for the LeechText project, including all workflows, configuration, and usage instructions.

## Table of Contents

- [Overview](#overview)
- [Workflow Architecture](#workflow-architecture)
- [Available Workflows](#available-workflows)
- [Configuration](#configuration)
- [Usage](#usage)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

## Overview

The LeechText project uses GitHub Actions for automated CI/CD processes. The pipeline is designed to:

- **Build and test** the application on multiple platforms (macOS, Windows, Ubuntu)
- **Ensure code quality** through automated checks and testing
- **Create platform-specific packages** for distribution
- **Automate releases** when tags are created
- **Monitor dependencies** for security vulnerabilities
- **Generate comprehensive reports** for quality assurance

## Workflow Architecture

### Pipeline Stages

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Code Push     │───▶│   Quality Gate  │───▶│   Build & Test  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   Security      │    │  Platform      │
                       │   Scanning      │    │  Packaging     │
                       └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   Release       │    │   Deployment    │
                       │   Creation      │    │   & Delivery    │
                       └─────────────────┘    └─────────────────┘
```

### Workflow Dependencies

- **Quality Gate**: Must pass before builds proceed
- **Build & Test**: Runs on all target platforms
- **Security Scan**: Independent security validation
- **Platform Packaging**: Creates native installers
- **Release**: Automated release management

## Available Workflows

### 1. Build and Test (`build.yml`)

**Purpose**: Core CI pipeline for building and testing the application

**Triggers**:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Release publication

**Features**:
- Multi-platform builds (macOS, Windows, Ubuntu)
- Multi-JDK testing (Java 8, 11, 17)
- Automated testing and quality checks
- Javadoc generation
- Artifact uploads

**Jobs**:
- `build`: Matrix build across platforms and Java versions
- `compatibility`: Cross-platform compatibility testing
- `security`: Dependency vulnerability scanning
- `quality`: Code coverage and quality analysis
- `release`: Release artifact creation

### 2. Platform-Specific Builds (`platform-builds.yml`)

**Purpose**: Create native packages and installers for each platform

**Triggers**:
- Push to `main` or `develop`
- Tag creation (v*)
- Manual workflow dispatch

**Features**:
- macOS: `.app` bundle and `.dmg` installer
- Windows: `.exe` installer
- Linux: `.deb`, `.rpm`, and AppImage packages
- Docker container builds
- Automated GitHub releases

**Jobs**:
- `macos-build`: macOS application packaging
- `windows-build`: Windows installer creation
- `linux-build`: Linux package generation
- `distribution`: Cross-platform distribution packages
- `docker-build`: Multi-architecture Docker images
- `deploy`: Release deployment and GitHub Pages

### 3. Scheduled Tasks (`scheduled.yml`)

**Purpose**: Automated maintenance and monitoring tasks

**Triggers**:
- Daily at 2 AM UTC
- Every Monday at 3 AM UTC
- Manual workflow dispatch

**Features**:
- Nightly builds and testing
- Dependency update checking
- Security vulnerability scanning
- Code quality analysis
- Performance testing
- Documentation generation

**Jobs**:
- `nightly-build`: Automated nightly builds
- `dependency-updates`: Check for outdated dependencies
- `security-scan`: Security vulnerability scanning
- `code-quality`: Automated code quality checks
- `performance-test`: Performance regression testing
- `docs-generation`: Automated documentation updates
- `summary`: Comprehensive status reporting

### 4. Pull Request Quality Gate (`pr-quality-gate.yml`)

**Purpose**: Ensure code quality before merging

**Triggers**:
- Pull requests to `main` or `develop`
- Push to `main` or `develop`

**Features**:
- Code formatting and style checks
- Static analysis and bug detection
- Security vulnerability scanning
- Test coverage validation
- Build verification
- Documentation quality checks
- Performance regression detection

**Jobs**:
- `quality-gate`: Code quality validation
- `security-gate`: Security checks
- `coverage-gate`: Test coverage validation
- `build-gate`: Build verification
- `docs-gate`: Documentation quality
- `performance-gate`: Performance validation
- `quality-summary`: Comprehensive quality report

## Configuration

### Required Secrets

The workflows require the following GitHub repository secrets:

#### For Docker Hub Integration
```yaml
DOCKERHUB_USERNAME: Your Docker Hub username
DOCKERHUB_TOKEN: Your Docker Hub access token
```

#### For SonarQube Analysis
```yaml
SONAR_TOKEN: Your SonarQube authentication token
```

#### For GitHub Releases
```yaml
GITHUB_TOKEN: Automatically provided by GitHub Actions
```

### Environment Variables

#### Global Environment
```yaml
GRADLE_OPTS: -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.caching=true
```

#### Platform-Specific Variables
- **macOS**: Uses default macOS environment
- **Windows**: Uses default Windows environment
- **Ubuntu**: Uses default Ubuntu environment

### Gradle Configuration

The workflows expect the following Gradle tasks to be available:

```gradle
// Core tasks
clean, build, test, check, javadoc

// Distribution tasks
distZip, distTar, jpackage

// Quality tasks
spotlessCheck, checkstyleMain, checkstyleTest
pmdMain, pmdTest, spotbugsMain, spotbugsTest

// Coverage tasks
jacocoTestReport

// Security tasks
dependencyCheckAnalyze

// Performance tasks
jmh
```

## Usage

### Automatic Triggers

#### Push to Main Branches
```bash
# Push to main branch
git push origin main

# Push to develop branch
git push origin develop
```

#### Pull Requests
```bash
# Create a pull request
gh pr create --title "Feature: Add new functionality" --body "Description"

# The quality gate will automatically run
```

#### Release Tags
```bash
# Create a release tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# This triggers the release workflow
```

### Manual Workflow Dispatch

#### Platform-Specific Builds
1. Go to **Actions** tab in GitHub
2. Select **Platform-Specific Builds** workflow
3. Click **Run workflow**
4. Choose target platform:
   - `all`: Build for all platforms
   - `macos`: Build only for macOS
   - `windows`: Build only for Windows
   - `linux`: Build only for Linux

#### Scheduled Tasks
1. Go to **Actions** tab in GitHub
2. Select **Scheduled Tasks** workflow
3. Click **Run workflow**
4. The workflow will run all scheduled tasks

### Monitoring Workflows

#### View Workflow Status
1. Go to **Actions** tab in GitHub
2. Select the workflow you want to monitor
3. View real-time execution status
4. Check logs for any errors

#### Download Artifacts
1. Navigate to a completed workflow run
2. Scroll down to **Artifacts** section
3. Download the artifacts you need
4. Artifacts are automatically cleaned up based on retention policies

## Troubleshooting

### Common Issues

#### Build Failures

**Problem**: Build job fails with compilation errors
**Solution**:
1. Check the build logs for specific error messages
2. Ensure all dependencies are properly declared in `build.gradle`
3. Verify Java version compatibility
4. Run `./gradlew build` locally to reproduce the issue

**Problem**: Platform-specific build failures
**Solution**:
1. Check platform-specific requirements
2. Verify native dependencies are available
3. Test the build locally on the target platform
4. Check workflow configuration for platform-specific steps

#### Test Failures

**Problem**: Tests fail in CI but pass locally
**Solution**:
1. Check for environment-specific test dependencies
2. Verify test data and resources are available
3. Check for timing issues in tests
4. Ensure tests are properly isolated

#### Quality Gate Failures

**Problem**: Code quality checks fail
**Solution**:
1. Run quality checks locally: `./gradlew check`
2. Fix code style issues: `./gradlew spotlessApply`
3. Address static analysis warnings
4. Improve test coverage if below thresholds

#### Security Scan Failures

**Problem**: Security vulnerabilities detected
**Solution**:
1. Review the security report in workflow artifacts
2. Update vulnerable dependencies
3. Check for false positives
4. Consider security implications of updates

### Debugging Workflows

#### Enable Debug Logging
```yaml
# Add to workflow step
env:
  ACTIONS_STEP_DEBUG: true
  ACTIONS_RUNNER_DEBUG: true
```

#### Local Testing
```bash
# Test Gradle tasks locally
./gradlew clean build test

# Test quality checks
./gradlew check spotlessCheck

# Test security scan
./gradlew dependencyCheckAnalyze
```

#### Workflow Debugging
1. Check workflow syntax in GitHub Actions tab
2. Verify YAML indentation and structure
3. Check for missing secrets or environment variables
4. Review workflow dependencies and job ordering

## Best Practices

### Development Workflow

#### Before Pushing Code
1. **Run tests locally**: `./gradlew test`
2. **Check code quality**: `./gradlew check`
3. **Verify formatting**: `./gradlew spotlessCheck`
4. **Update dependencies**: Check for outdated dependencies

#### Pull Request Process
1. **Create feature branch**: `git checkout -b feature/new-feature`
2. **Make changes**: Implement your feature
3. **Test locally**: Ensure all tests pass
4. **Create PR**: Submit pull request
5. **Monitor CI**: Watch quality gate results
6. **Address feedback**: Fix any issues identified

#### Release Process
1. **Update version**: Modify version in appropriate files
2. **Create tag**: `git tag -a v1.0.0 -m "Release v1.0.0"`
3. **Push tag**: `git push origin v1.0.0`
4. **Monitor release**: Watch release workflow execution
5. **Verify artifacts**: Check generated packages
6. **Update documentation**: Update release notes and documentation

### Workflow Maintenance

#### Regular Updates
1. **Update actions**: Keep GitHub Actions up to date
2. **Review dependencies**: Monitor for security updates
3. **Optimize performance**: Identify and fix bottlenecks
4. **Update documentation**: Keep this guide current

#### Monitoring and Alerts
1. **Watch workflow failures**: Set up notifications for failures
2. **Monitor performance**: Track workflow execution times
3. **Review artifacts**: Check generated reports and packages
4. **Security monitoring**: Monitor security scan results

### Performance Optimization

#### Build Optimization
1. **Use caching**: Leverage Gradle and GitHub Actions caching
2. **Parallel execution**: Run independent jobs in parallel
3. **Conditional execution**: Skip unnecessary steps when possible
4. **Resource optimization**: Use appropriate runner sizes

#### Workflow Optimization
1. **Minimize dependencies**: Reduce job dependencies where possible
2. **Efficient triggers**: Use appropriate workflow triggers
3. **Artifact management**: Clean up artifacts regularly
4. **Matrix strategies**: Use matrix builds for similar jobs

---

This CI/CD guide provides comprehensive information about the automated pipeline for the LeechText project. For specific workflow details, see the individual workflow files in the `.github/workflows/` directory.

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Java Platform Documentation](https://docs.oracle.com/en/java/)
- [Project Development Guide](DEVELOPMENT.md)
- [API Documentation](API_DOCUMENTATION.md)
