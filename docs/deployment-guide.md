# Deployment Guide - LeechText

## Overview

LeechText uses GitHub Actions CI/CD for automated multi-platform builds and releases.

## CI/CD Pipeline

### Workflow Structure

```
.github/workflows/
├── build-multi-platform.yml  # Multi-platform builds & releases
├── code-quality.yml           # Code quality checks
├── documentation.yml          # Documentation validation
└── README.md                  # Workflow documentation
```

### Build Process

**Triggered by:**
- Push to `main` or `develop` branches
- Pull requests
- Manual dispatch
- Tag push (for releases)

**Platforms:**
- **macOS**: macOS-latest → `LeechText-mac.jar`
- **Windows**: windows-latest → `LeechText-windows.jar`
- **Linux**: ubuntu-latest → `LeechText-linux.jar`

## Building Locally

### Prerequisites

- Java 17 or higher
- Gradle 8.4+

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Create distribution JAR
./gradlew jar

# Run application
./gradlew run

# Build with specific configuration
./gradlew build -Penvironment=production
```

### Platform-Specific Builds

```bash
# Build for current platform
./gradlew build

# Build all platforms (requires Docker)
./gradlew buildAllPlatforms
```

## Testing

### Run Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests PluginManagerTest

# Run with coverage
./gradlew test jacocoTestReport

# Run specific test suite
./gradlew test --tests "*integration*"
```

### Code Quality Checks

```bash
# Format check
./gradlew spotlessCheck

# Auto-format code
./gradlew spotlessApply

# Style checks
./gradlew checkstyleMain checkstyleTest

# PMD analysis
./gradlew pmdMain pmdTest

# All quality checks
./gradlew qualityGate
```

## Release Process

### Automated Releases (GitHub Actions)

**Trigger:** Push version tag (e.g., `v1.0.0`)

```bash
# Create and push tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

**Workflow:**
1. Builds for all 3 platforms
2. Runs tests on each platform
3. Creates release artifacts
4. Generates checksums (SHA256, MD5)
5. Publishes GitHub release

**Artifacts:**
- `LeechText-mac.jar` + checksums
- `LeechText-windows.jar` + checksums
- `LeechText-linux.jar` + checksums

### Manual Releases

```bash
# 1. Update version
# Edit build.gradle: version = '1.0.0'

# 2. Build release
./gradlew clean build -x test

# 3. Test locally
java -jar build/libs/LeechText.jar

# 4. Create tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 5. Download artifacts from GitHub Actions
gh run download --name leechtext-mac
gh run download --name leechtest-windows
gh run download --name leechtest-linux
```

## Deployment

### Running the Application

**Requirements:**
- Java 17+ JRE or JDK
- 512MB RAM minimum (1GB recommended)
- 100MB disk space

**macOS/Linux:**
```bash
java -jar LeechText-mac.jar
# or make executable
chmod +x LeechText-mac.jar
./LeechText-mac.jar
```

**Windows:**
```cmd
java -jar LeechText-windows.jar
# or create batch file
echo @java -jar LeechText-windows.jar > start.bat
```

### Distribution Options

**Option 1: Direct JAR Distribution**
- Pros: Simple, cross-platform
- Cons: Requires Java installation

**Option 2: Native Packages (Future)**
- Use jpackage for native installers
- macOS: .dmg / .app
- Windows: .exe / .msi
- Linux: .deb / .rpm

**Option 3: Portable Distribution**
```bash
# Create portable package
./gradlew createPortablePackage

# Includes:
# - JAR file
# - JRE (embedded)
# - Configuration files
# - User guide
```

## Configuration

### Application Settings

**Location:** `setting.json` (auto-generated)

```json
{
  "theme": "dark",
  "language": "vi",
  "downloadPath": "./downloads",
  "maxThreads": 5,
  "calibrePath": "/usr/bin/ebook-convert"
}
```

### Environment Variables

```bash
# Set default options
export LEECHTEXT_HOME=/path/to/config
export LEECHTHEME=dark
export LEECHLANG=vi

# Run with custom settings
java -Dleechtext.home=/custom/path -jar LeechText.jar
```

## Pre-Commit Hooks

### Setup

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Run manually
pre-commit run --all-files
```

### Hooks Configured

- **trailing-whitespace**: Trim trailing whitespace
- **end-of-file-fixer**: Ensure newline at EOF
- **check-yaml**: Validate YAML syntax
- **check-json**: Validate JSON syntax
- **check-merge-conflict**: Detect merge conflicts
- **check-case-conflict**: Detect case conflicts
- **check-added-large-files**: Prevent large files (>1MB)
- **gradle-format**: Run Spotless format check
- **gradle-compile**: Verify Java compilation
- **gradle-spotless**: Auto-apply Spotless formatting

## Monitoring

### CI/CD Status

```bash
# Check workflow status
gh run list --workflow=build-multi-platform.yml

# View specific run
gh run view <run-id>

# Watch logs in real-time
gh run watch
```

### Build Notifications

Enable GitHub notifications:
- Repository → Settings → Notifications
- Configure email/Slack/webhook notifications

## Troubleshooting

### Build Failures

**Issue:** Compilation error
```bash
# Clean and rebuild
./gradlew clean build --no-daemon

# Check Java version
java -version  # Should be 17+
```

**Issue:** Out of memory
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Or in gradle.properties
org.gradle.jvmargs=-Xmx2048m
```

**Issue:** Spotless violations
```bash
# Auto-fix formatting
./gradlew spotlessApply

# Re-check
./gradlew spotlessCheck
```

### Runtime Issues

**Issue:** Application won't start
```bash
# Check Java version
java -version

# Check JAR integrity
jar tf LeechText.jar

# Run with debug output
java -verbose:class -jar LeechText.jar
```

**Issue:** Plugin loading errors
```bash
# Check plugins directory
ls -la tools/plugins/

# Verify plugin format
java -jar LeechText.jar --validate-plugins

# Enable debug logging
java -Dleechtext.debug=true -jar LeechText.jar
```

## Performance Optimization

### Build Performance

```bash
# Enable Gradle caching
org.gradle.caching=true

# Parallel builds
./gradlew build --parallel

# Configure parallelism
org.gradle.workers.max=4
```

### Runtime Performance

```bash
# Increase heap size
java -Xmx1g -jar LeechText.jar

# Use G1GC (better for large heaps)
java -XX:+UseG1GC -Xmx1g -jar LeechText.jar

# Enable GC logging
java -Xlog:gc*:file=gc.log -jar LeechText.jar
```

## Security

### Dependency Scanning

```bash
# Run dependency check
./gradlew dependencyCheckAnalyze

# Update dependencies
./gradlew dependencyUpdates
```

### Code Security

```bash
# Run security scanner
./gradlew securityScan

# Check for vulnerabilities
./gradlew auditDependencies
```

## Backup and Recovery

### Backup Configuration

```bash
# Backup application data
tar -czf leechtext-backup.tar.gz \
  setting.json \
  tools/plugins/ \
  downloads/ \
  .leechtext/
```

### Restore Configuration

```bash
# Extract backup
tar -xzf leechtext-backup.tar.gz

# Verify integrity
java -jar LeechText.jar --verify-config
```

## Additional Resources

- [CI/CD Workflows](../.github/workflows/README.md)
- [Code Standards](code-standards.md)
- [System Architecture](system-architecture.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
