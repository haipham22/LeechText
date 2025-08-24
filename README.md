# LeechText

A Java-based desktop application for scraping and managing text content from web sources, particularly Vietnamese web novels.

## Features

- **Web Scraping**: Extract content from HTML pages using JSoup
- **Text Export**: Export content to various formats (TXT, HTML, EPUB)
- **Download Management**: Track and manage download progress
- **Plugin Support**: Extensible via Lua scripts
- **Syntax Highlighting**: Built-in text formatting capabilities
- **User Interface**: Custom Material Design-inspired Swing UI

## Quick Start

### Prerequisites

- Java 17 or higher
- mise (for tool management) or manual Java/Gradle installation

### Building and Running

```bash
# Install tools (if using mise)
mise install

# Build the project
./gradlew build

# Run the application
./gradlew run
```

## Development

This project uses modern Java development practices:

- **Build System**: Gradle 8.4
- **Code Quality**: Spotless, Checkstyle, PMD
- **Pre-commit Hooks**: Automated formatting and validation
- **Conventional Commits**: Standardized commit messages

For detailed development guidelines, see [DEVELOPMENT.md](DEVELOPMENT.md).

### Development Setup

```bash
# Clone and setup
git clone <repository-url>
cd leechtext-java
mise install
npm install

# Install pre-commit hooks
pre-commit install
pre-commit install --hook-type commit-msg

# Build and test
./gradlew build
```

## Libraries Used

- **JSoup**: HTML parsing - https://jsoup.org
- **RSyntaxTextArea**: Syntax highlighting - http://bobbylight.github.io/RSyntaxTextArea
- **zip4j**: Archive handling - https://github.com/srikanth-lingala/zip4j
- **LuaJ**: Lua scripting engine - https://sourceforge.net/projects/luaj
- **Gson**: JSON processing

## Contributing

1. Read the [development guidelines](DEVELOPMENT.md)
2. Follow the conventional commit format
3. Ensure all pre-commit checks pass
4. Write tests for new features
5. Submit a pull request

## License

This project is provided as-is for educational and personal use.

## Support

For bug reports and suggestions, please create an issue in the repository.

---

**Note**: This application is designed for Vietnamese web novel content extraction. Please respect website terms of service and copyright laws when using this tool.
