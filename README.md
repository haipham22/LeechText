# LeechText

A powerful Java-based text extraction and ebook creation application that allows users to download content from various sources and convert them into different formats.

## Overview

LeechText is designed to help users extract text content from websites, novels, and other online sources, then organize and export them into various formats including ebooks (EPUB), plain text, and structured documents. The application features a modern, intuitive user interface with support for plugins, custom extraction rules, and batch processing.

## Features

### Core Functionality
- **Text Extraction**: Download and extract content from various web sources
- **Multi-format Export**: Support for EPUB, plain text, and structured formats
- **Plugin System**: Extensible architecture with custom extraction plugins
- **Batch Processing**: Download multiple chapters or documents simultaneously
- **Progress Tracking**: Real-time download progress and status monitoring

### User Interface
- **Modern Design**: Clean, intuitive interface with custom UI components
- **Responsive Layout**: Adaptive design that works across different screen sizes
- **Dark/Light Themes**: Customizable appearance options
- **Animations**: Smooth transitions and visual feedback
- **Drag & Drop**: Easy file and URL management

### Advanced Features
- **Regex Support**: Custom pattern matching for content extraction
- **Text Optimization**: Automatic content cleaning and formatting
- **Multi-language Support**: Internationalization for Vietnamese and English
- **Cookie Management**: Persistent session handling for authenticated sites
- **Proxy Support**: Network configuration options

## Architecture

The application is built using a modular architecture with the following key components:

- **UI Layer**: Swing-based user interface with custom components
- **Plugin System**: Extensible plugin architecture for content extraction
- **Download Engine**: Multi-threaded download management
- **Export System**: Multiple output format support
- **Utility Framework**: Common utilities and helper classes

## Installation

### Prerequisites
- Java 8 or higher
- Gradle (for building from source)

### Building from Source
```bash
# Clone the repository
git clone https://github.com/your-username/LeechText.git
cd LeechText

# Build the project
./gradlew build

# Run the application
./gradlew run
```

### Running the JAR
```bash
java -jar build/libs/LeechText.jar
```

## Usage

### Basic Workflow
1. **Add URL**: Enter the URL of the content you want to extract
2. **Select Plugin**: Choose the appropriate extraction plugin for the source
3. **Configure Options**: Set download preferences and export format
4. **Start Download**: Begin the extraction process
5. **Export Content**: Save the extracted content in your preferred format

### Plugin Management
- **Install Plugins**: Place .plugin files in the `tools/plugins` directory
- **Plugin Configuration**: Each plugin defines extraction rules and patterns
- **Plugin Updates**: Automatic update checking and management

### Export Options
- **EPUB**: Create ebooks with proper chapter structure
- **Plain Text**: Simple text output with basic formatting
- **Table of Contents**: Generate structured content outlines
- **Custom Formats**: Extensible export system

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

### Building and Testing
```bash
# Run tests
./gradlew test

# Build documentation
./gradlew javadoc

# Create distribution
./gradlew distZip
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Code style and standards
- Testing requirements
- Pull request process
- Development setup

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests and documentation
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

### Getting Help
- **Issues**: Report bugs and request features on GitHub
- **Documentation**: Check the [Wiki](https://github.com/your-username/LeechText/wiki)
- **Community**: Join our discussion forums

### Common Issues
- **Plugin Loading**: Ensure .plugin files are in the correct directory
- **Download Failures**: Check network connectivity and plugin compatibility
- **Export Errors**: Verify output directory permissions and disk space

## Changelog

### Version 2019.03.30
- Initial public release
- Core text extraction functionality
- Plugin system implementation
- Modern UI with custom components
- Multi-format export support

## Acknowledgments

- **Original Author**: Long
- **Contributors**: LeechText Development Team
- **Open Source Libraries**: See [DEPENDENCIES.md](DEPENDENCIES.md)

---

**LeechText** - Extract, organize, and enjoy your content.
