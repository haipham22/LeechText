# LeechText

A powerful Java-based text extraction and ebook creation application with plugin support, multi-platform compatibility, and modern CI/CD pipeline.

[![Build](https://github.com/haipham22/LeechText/actions/workflows/build-multi-platform.yml/badge.svg)](https://github.com/haipham22/LeechText/actions/workflows/build-multi-platform.yml)
[![Code Quality](https://github.com/haipham22/LeechText/actions/workflows/code-quality.yml/badge.svg)](https://github.com/haipham22/LeechText/actions/workflows/code-quality.yml)

## Overview

LeechText is designed to help users extract text content from websites, novels, and other online sources, then organize and export them into various formats including ebooks (EPUB), plain text, and structured documents. The application features a modern, intuitive user interface with support for plugins, custom extraction rules, and batch processing.

## Key Features

### Core Functionality
- **Text Extraction**: Download and extract content from various web sources
- **Multi-format Export**: Support for EPUB, plain text, and structured formats
- **Dual Plugin System**: Support for both Lua and JavaScript (Rhino) based plugins
- **vBook Plugin Support**: Auto-download and convert vBook plugins
- **WebP Cover Image Handling**: Automatic conversion to JPEG for EPUB compatibility
- **Batch Processing**: Download multiple chapters simultaneously
- **Progress Tracking**: Real-time download progress and status monitoring

### User Interface
- **Modern Design**: Clean, intuitive interface with custom Material UI components
- **Responsive Layout**: Adaptive design that works across different screen sizes
- **Dark/Light Themes**: Customizable appearance options
- **Animations**: Smooth transitions and visual feedback
- **Drag & Drop**: Easy file and URL management

### Advanced Features
- **Regex Support**: Custom pattern matching for content extraction
- **Text Optimization**: Automatic content cleaning and HTML sanitization
- **Multi-language Support**: Internationalization for Vietnamese and English
- **Cookie Management**: Persistent session handling for authenticated sites
- **Proxy Support**: Network configuration options
- **Plugin Security**: Comprehensive validation and sandboxing

## Architecture

The application follows a **layered architecture** with clear separation of concerns:

- **Presentation Layer**: Swing-based UI with Material Design components
- **Action Layer**: Business logic orchestration (Download, Export, Config)
- **Content Retrieval**: HTTP communication and plugin execution
- **Data Layer**: Models, entities, and settings
- **Utility Layer**: File I/O, HTTP, image processing, format conversion
- **Plugin System**: Lua + JavaScript (Rhino) engines with security sandboxing

## Installation

### Prerequisites
- **Java 17** or higher
- **Gradle 8.4+** (for building from source)
- **ffmpeg** (optional, for WebP→JPEG conversion)

### Quick Start

#### Option 1: Download Release
```bash
# Download latest release for your platform
# macOS: LeechText-mac.jar
# Windows: LeechText-windows.jar
# Linux: LeechText-linux.jar

# Run the application
java -jar LeechText-*.jar
```

#### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/haipham22/LeechText.git
cd LeechText

# Build the project
./gradlew build

# Run the application
./gradlew run
```

## Usage

### Basic Workflow
1. **Add URL**: Enter the URL of the content you want to extract
2. **Select Plugin**: Choose the appropriate extraction plugin (Lua or JavaScript)
3. **Configure Options**: Set download preferences and export format
4. **Start Download**: Begin the extraction process
5. **Export Content**: Save as EPUB, plain text, or custom format

### Plugin Management

#### Lua Plugins
- Place `.plugin` files in `tools/plugins/`
- Define extraction rules in Lua syntax
- Full API access (HTTP, HTML, JSON, Regex)

#### JavaScript Plugins (New!)
- vBook format support
- ES6+ syntax with Rhino engine
- Auto-download from repositories
- Migration tools available

#### vBook Repository
```bash
# Browse and install plugins directly
Tools → Plugin Browser → vBook Repository
```

### Export Options
- **EPUB**: Create ebooks with proper chapter structure and cover images
- **Plain Text**: Simple text output with basic formatting
- **Table of Contents**: Generate structured content outlines
- **Cover Images**: Automatic WebP to JPEG conversion for compatibility

## Development

### Project Structure
```
src/main/java/dark/leech/text/
├── action/          # Core application actions
├── animation/       # UI animation system
├── enities/         # Data models and entities
├── get/             # Content retrieval logic
├── image/           # Image processing utilities
├── listeners/       # Event listeners and handlers
├── lua/             # Lua script engine integration
├── models/          # Data models
├── plugin/          # Plugin management system
│   ├── js/          # JavaScript (Rhino) engine
│   ├── lua/         # Lua engine
│   ├── vbook/       # vBook plugin support
│   ├── security/    # Plugin validation
│   └── sandbox/     # Security sandboxing
├── ui/              # User interface components
└── util/            # Utility classes and helpers
```

### Key Classes
- **App**: Main application entry point
- **MainUI**: Primary user interface
- **PluginManager**: Dual plugin system (Lua + JavaScript)
- **DownloadUI**: Download management interface
- **ImageConverter**: WebP to JPEG conversion utility
- **Chapter**: Content chapter representation

### Building and Testing
```bash
# Run tests
./gradlew test

# Check code format
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessApply

# Run quality checks
./gradlew qualityGate

# Build distribution
./gradlew distZip
```

## Configuration

### Application Settings
The application stores configuration in `setting.json`:
```json
{
  "theme": "dark",
  "language": "en",
  "downloadPath": "./downloads",
  "maxThreads": 5
}
```

### Plugin Configuration
Plugins are defined in JSON format with the following structure:
```json
{
  "name": "Plugin Name",
  "version": 1.0,
  "regex": "example\\.com",
  "chap": "chapter extraction script",
  "toc": "table of contents script"
}
```

## Development

### Project Structure
```
src/main/java/dark/leech/text/
├── action/          # Core application actions
├── animation/       # UI animation system
├── enities/         # Data models and entities
├── get/             # Content retrieval logic
├── image/           # Image processing utilities
├── listeners/       # Event listeners and handlers
├── lua/             # Lua script engine integration
├── models/          # Data models
├── plugin/          # Plugin management system
├── ui/              # User interface components
└── util/            # Utility classes and helpers
```

### Key Classes
- **App**: Main application entry point
- **MainUI**: Primary user interface
- **PluginManager**: Plugin loading and management
- **DownloadUI**: Download management interface
- **Chapter**: Content chapter representation

## CI/CD

### Multi-Platform Builds

Automated builds for all platforms via GitHub Actions:

- **macOS**: `LeechText-mac.jar`
- **Windows**: `LeechText-windows.jar`
- **Linux**: `LeechText-linux.jar`

### Code Quality

- **Spotless**: Code formatting (Google Java Format)
- **Checkstyle**: Style validation
- **PMD**: Code quality analysis
- **JUnit**: Unit testing with coverage

### Documentation

- **Automated Validation**: Link checking, size limits
- **Deployment Guide**: Complete deployment procedures
- **CI/CD Documentation**: Workflow usage and troubleshooting

### Release Process

```bash
# Create version tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# GitHub Actions automatically:
# 1. Builds all platforms
# 2. Runs tests
# 3. Creates release
# 4. Uploads artifacts + checksums
```

See [`.github/workflows/`](.github/workflows/) for workflow configurations.

## Contributing

We welcome contributions! Please see our development guidelines:

### Code Standards
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Spotless for formatting
- Write unit tests for new features
- Update documentation as needed

### Testing Requirements
- Unit tests for all new code
- Integration tests for plugin systems
- Security validation for plugins

### Pull Request Process
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./gradlew test`)
5. Run quality checks (`./gradlew qualityGate`)
6. Commit changes (`git commit -m 'feat: add amazing feature'`)
7. Push to branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Pre-Commit Hooks

```bash
# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Hooks run automatically on git commit
```

## Documentation

- **[Deployment Guide](docs/deployment-guide.md)**: Build, deploy, and release procedures
- **[System Architecture](docs/system-architecture.md)**: Architecture overview and components
- **[Code Standards](docs/code-standards.md)**: Development standards and guidelines
- **[CI/CD Workflows](.github/workflows/README.md)**: CI/CD usage and troubleshooting
- **[WebP Fix Documentation](docs/fix-webp-cover-image-issue.md)**: Cover image conversion details

## Configuration

### Application Settings

**Location**: `setting.json` (auto-generated)

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
# Set custom home directory
export LEECHTEXT_HOME=/path/to/config

# Set default theme
export LEECHTHEME=dark

# Set default language
export LEECHLANG=vi
```

## Troubleshooting

### Common Issues

**Plugin Loading Failures**
- Ensure `.plugin` files are in `tools/plugins/`
- Check plugin format and syntax
- Verify plugin security validation

**Download Failures**
- Check network connectivity
- Verify plugin compatibility with source
- Review error logs in application

**Export Errors**
- Verify output directory permissions
- Ensure sufficient disk space
- Check Calibre installation for EPUB conversion

**Cover Images Not Displaying**
- Verify ffmpeg installation (for WebP conversion)
- Check image format compatibility
- Review EPUB structure validation

### Debug Mode

```bash
# Enable debug logging
java -Dleechtext.debug=true -jar LeechText.jar

# Enable trace logging
java -Dleechtext.trace=true -jar LeechText.jar
```

## Acknowledgments

### Upstream Project

This project is based on the original **LeechText** by Long.

- **Original Repository**: [LeechText Original](https://github.com/longlHD/LeechText)
- **Original Author**: Long
- **License**: MIT License

### This Fork

**Repository**: [haipham22/LeechText](https://github.com/haipham22/LeechText)

**Major Enhancements**:
- ✅ JavaScript (Rhino) engine with ES6+ support
- ✅ vBook plugin format compatibility
- ✅ Comprehensive plugin security validation
- ✅ WebP to JPEG cover image conversion
- ✅ Multi-platform CI/CD pipeline
- ✅ Enhanced plugin management UI
- ✅ Plugin sandboxing and resource limits
- ✅ Automated testing and quality checks

**Contributors**: LeechText Development Team
**Dependencies**: See [`build.gradle`](build.gradle) for full list

---

**LeechText** - Extract, organize, and enjoy your content.
