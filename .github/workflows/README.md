# CI/CD Workflows

This directory contains GitHub Actions workflows for automated building, testing, and quality checks.

## Workflows

### 1. Build Multi-Platform (`build-multi-platform.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**Platforms:**
- macOS (macos-latest)
- Windows (windows-latest)
- Linux (ubuntu-latest)

**Jobs:**
- **build**: Compiles and tests on each platform
  - Builds JAR with Gradle
  - Runs unit tests
  - Uploads platform-specific artifacts
  - Uploads test results

- **release**: Creates GitHub releases (on tags)
  - Downloads all platform artifacts
  - Generates SHA256 and MD5 checksums
  - Creates GitHub release with all binaries

**Artifacts:**
- `LeechText-mac.jar` - macOS binary
- `LeechText-windows.jar` - Windows binary
- `LeechText-linux.jar` - Linux binary
- Checksums for each binary
- Test results for each platform

### 2. Code Quality (`code-quality.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

**Jobs:**
- **format**: Checks code formatting with Spotless
- **style**: Runs Checkstyle analysis
- **pmd**: Performs PMD code quality analysis
- **test**: Runs unit tests and publishes results

**Reports:**
- Checkstyle violations report
- PMD analysis report
- Test results with JUnit XML
- Test coverage report

### 3. Documentation (`documentation.yml`)

**Triggers:**
- Push to `main` or `develop` (docs paths only)
- Pull requests to `main` or `develop` (docs paths only)

**Jobs:**
- **validate-docs**: Validates documentation
  - Checks markdown links
  - Lints markdown files
  - Enforces document size limits (800 LOC max)

- **build-docs**: Builds documentation site
  - Generates documentation index
  - Uploads documentation artifacts

## Workflow Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                    Pull Request / Push                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Code Quality │   │   Build      │   │Documentation │
│  (Parallel)  │   │ (Parallel)   │   │  (Parallel)  │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       │                  │                   │
       │            ┌─────┴─────┐              │
       │            │           │              │
       ▼            ▼           ▼              ▼
┌──────────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│ Format Check │ │  macOS  │ │ Windows │ │  Linux  │
│ Style Check  │ └────┬────┘ └────┬────┘ └────┬────┘
│ PMD Analysis │     │          │          │
│ Unit Tests   │     └──────────┴──────────┘
└──────────────┘                   │
                                  ▼
                          ┌───────────────┐
                          │    Release    │
                          │ (Tags only)   │
                          └───────────────┘
```

## Usage

### Manual Workflow Trigger

```bash
# Trigger build workflow manually
gh workflow run build-multi-platform.yml

# Trigger with specific branch
gh workflow run build-multi-platform.yml --ref develop
```

### Download Artifacts

```bash
# List recent workflow runs
gh run list --workflow=build-multi-platform.yml

# Download artifacts from a run
gh run download <run-id>

# Download specific artifact
gh run download <run-id> -n leechtext-mac
```

### Local Testing

Test workflows locally before pushing:

```bash
# Install act (https://github.com/nektos/act)
brew install act  # macOS

# Run workflow locally
act push

# Run specific job
act -j build
```

## Configuration

### Secrets

No secrets required for basic workflows. For release functionality:
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions

### Environment Variables

Set in workflow files:
- `JAVA_VERSION`: 17 (default)
- `MAX_DOC_LOC`: 800 (documentation size limit)

### Gradle Properties

Configure in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true
```

## Status Badges

Add to README.md:

```markdown
[![Build](https://github.com/username/leechtext-java/actions/workflows/build-multi-platform.yml/badge.svg)](https://github.com/username/leechtext-java/actions/workflows/build-multi-platform.yml)
[![Code Quality](https://github.com/username/leechtext-java/actions/workflows/code-quality.yml/badge.svg)](https://github.com/username/leechtext-java/actions/workflows/code-quality.yml)
[![Documentation](https://github.com/username/leechtext-java/actions/workflows/documentation.yml/badge.svg)](https://github.com/username/leechtext-java/actions/workflows/documentation.yml)
```

## Troubleshooting

### Build Failures

**Issue:** Build fails on specific platform
```bash
# Check platform-specific logs
gh run view <run-id> --log

# Re-run failed job
gh run rerun <run-id> --failed
```

**Issue:** Spotless format violations
```bash
# Fix locally
./gradlew spotlessApply

# Verify
./gradlew spotlessCheck
```

### Test Failures

**Issue:** Tests pass locally but fail in CI
```bash
# Check CI environment differences
act -j test --env ACT=true

# Run with similar JVM settings
./gradlew test -Dorg.gradle.jvmargs="-Xmx2048m"
```

### Release Issues

**Issue:** Release not created on tag push
```bash
# Verify tag format
git tag -l "v*"

# Tag must match: refs/tags/v*
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

## Maintenance

### Update Dependencies

```yaml
# .github/workflows/build-multi-platform.yml
- uses: actions/checkout@v4  # Check for updates
+ uses: actions/checkout@v5

- uses: actions/setup-java@v4
+ uses: actions/setup-java@v5
```

### Add New Platforms

```yaml
strategy:
  matrix:
    os: [macos-latest, windows-latest, ubuntu-latest, macos-13]
    include:
      - os: macos-13
        platform: mac-intel
        artifact_name: LeechText-mac-intel.jar
```

### Performance Tuning

```yaml
# Enable caching
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v1

# Parallel builds
./gradlew build --parallel

# Build cache
org.gradle.caching=true
```
