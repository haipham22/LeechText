# Development Guide

This document provides comprehensive guidelines for developers working on the LeechText project.

## Table of Contents

- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Architecture Patterns](#architecture-patterns)
- [Testing Guidelines](#testing-guidelines)
- [Documentation Standards](#documentation-standards)
- [Build System](#build-system)
- [Code Quality Tools](#code-quality-tools)

## Development Setup

### Prerequisites
- **Java**: JDK 8 or higher (recommended: JDK 11+)
- **Gradle**: Version 7.0 or higher
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Git**: Version control system

### Environment Setup
```bash
# Clone the repository
git clone https://github.com/haipham22/LeechText.git
cd LeechText

# Install dependencies
./gradlew dependencies

# Build the project
./gradlew build

# Run the application
./gradlew run
```

### IDE Configuration
- **IntelliJ IDEA**: Import as Gradle project
- **Eclipse**: Use Gradle integration plugin
- **VS Code**: Install Java Extension Pack

## Project Structure

### Source Organization
```
src/
├── main/
│   ├── java/
│   │   └── dark/leech/text/
│   │       ├── action/          # Core application actions
│   │       ├── animation/       # UI animation system
│   │       ├── enities/         # Data models and entities
│   │       ├── get/             # Content retrieval logic
│   │       ├── image/           # Image processing utilities
│   │       ├── listeners/       # Event listeners and handlers
│   │       ├── lua/             # Lua script engine integration
│   │       ├── models/          # Data models
│   │       ├── plugin/          # Plugin management system
│   │       ├── ui/              # User interface components
│   │       └── util/            # Utility classes and helpers
│   └── resources/
│       ├── dark/leech/res/      # Application resources
│       ├── font/                # Font files
│       └── values/              # Localization files
└── test/
    └── java/                    # Test source code
```

### Package Naming Convention
- **Main packages**: `dark.leech.text.*`
- **UI components**: `dark.leech.text.ui.*`
- **Utilities**: `dark.leech.text.util.*`
- **Models**: `dark.leech.text.models.*`
- **Plugins**: `dark.leech.text.plugin.*`

## Coding Standards

### Java Code Style
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Naming**: Follow Java conventions
  - Classes: PascalCase (e.g., `MainUI`)
  - Methods: camelCase (e.g., `getChapterList()`)
  - Constants: UPPER_SNAKE_CASE (e.g., `MAX_DOWNLOADS`)
  - Variables: camelCase (e.g., `chapterCount`)

### Code Organization
```java
// File header with package and imports
package dark.leech.text.ui.main;

import java.awt.*;
import javax.swing.*;

/**
 * Class description with comprehensive JavaDoc.
 *
 * @author Developer Name
 * @version 1.0
 * @since 1.0
 */
public class ExampleClass {

    // Constants first
    private static final String DEFAULT_NAME = "Example";

    // Static fields
    private static int instanceCount = 0;

    // Instance fields
    private String name;
    private int value;

    // Constructors
    public ExampleClass() {
        this(DEFAULT_NAME);
    }

    public ExampleClass(String name) {
        this.name = name;
        instanceCount++;
    }

    // Public methods
    public String getName() {
        return name;
    }

    // Private methods
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }
}
```

### Documentation Standards
- **Class-level**: Describe purpose, usage, and key features
- **Method-level**: Document parameters, return values, and exceptions
- **Field-level**: Explain purpose and constraints
- **Inline comments**: Clarify complex logic

## Architecture Patterns

### Design Principles
1. **Single Responsibility**: Each class has one clear purpose
2. **Open/Closed**: Open for extension, closed for modification
3. **Dependency Inversion**: Depend on abstractions, not concretions
4. **Interface Segregation**: Keep interfaces focused and minimal

### Key Patterns Used
- **Singleton**: PluginManager, AppUtils
- **Observer**: Event listeners and handlers
- **Factory**: Plugin creation and UI component instantiation
- **Strategy**: Different export formats and extraction methods
- **Template Method**: Download and export workflows

### UI Architecture
```
MainUI (Main Window)
├── DownloadUI (Download Management)
├── SettingUI (Configuration)
├── PluginUI (Plugin Management)
└── HelpUI (Documentation)
```

## Testing Guidelines

### Test Structure
```
src/test/java/
└── dark/leech/text/
    ├── models/          # Model tests
    ├── util/            # Utility tests
    ├── plugin/          # Plugin tests
    └── ui/              # UI component tests
```

### Testing Standards
- **Unit Tests**: Test individual methods and classes
- **Integration Tests**: Test component interactions
- **Test Coverage**: Aim for 80%+ code coverage
- **Test Naming**: `MethodName_Scenario_ExpectedResult`

### Example Test
```java
@Test
public void createPlugin_ValidJsonFile_ReturnsPluginEntity() {
    // Arrange
    String validJson = "{\"name\":\"Test Plugin\",\"version\":1.0}";

    // Act
    PluginEntity result = PluginManager.createPlugin(validJson);

    // Assert
    assertNotNull(result);
    assertEquals("Test Plugin", result.getName());
    assertEquals(1.0, result.getVersion(), 0.01);
}
```

## Documentation Standards

### JavaDoc Requirements
- **Public APIs**: All public methods and classes must be documented
- **Parameters**: Document all parameters with @param tags
- **Return Values**: Document return values with @return tags
- **Exceptions**: Document thrown exceptions with @throws tags
- **Examples**: Include usage examples for complex methods

### Code Comments
- **Why, not what**: Explain the reasoning behind code decisions
- **Complex logic**: Clarify non-obvious algorithms
- **Workarounds**: Document known issues and temporary solutions
- **TODO/FIXME**: Mark areas needing attention

## Build System

### Gradle Configuration
- **Version**: 7.0+
- **Java Version**: 8+
- **Dependencies**: Managed through build.gradle
- **Plugins**: Java, Application, Distribution

### Build Commands
```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Generate Javadoc
./gradlew javadoc

# Create distribution
./gradlew distZip

# Run application
./gradlew run
```

### Dependencies
- **Core**: Java 8+ APIs
- **UI**: Swing (built-in)
- **JSON**: Gson for data serialization
- **Lua**: LuaJ for scripting
- **Testing**: JUnit 4+ for unit tests

## Code Quality Tools

### Static Analysis
- **Checkstyle**: Code style enforcement
- **PMD**: Code quality and complexity analysis
- **SpotBugs**: Bug pattern detection

### Configuration Files
- `config/checkstyle/checkstyle.xml` - Checkstyle rules
- `config/pmd/pmd-rules.xml` - PMD rules
- `.editorconfig` - Editor configuration

### Pre-commit Hooks
- **Formatting**: Automatic code formatting
- **Validation**: Style and quality checks
- **Tests**: Run unit tests before commit

## Development Workflow

### Feature Development
1. **Create Branch**: `git checkout -b feature/feature-name`
2. **Implement**: Write code following standards
3. **Test**: Ensure all tests pass
4. **Document**: Update documentation and JavaDoc
5. **Commit**: Use conventional commit format
6. **Push**: Submit pull request

### Bug Fixes
1. **Identify**: Reproduce and document the issue
2. **Fix**: Implement the solution
3. **Test**: Verify the fix works
4. **Document**: Update relevant documentation
5. **Commit**: Reference issue number

### Code Review Process
1. **Self-review**: Check your own code first
2. **Peer Review**: Have another developer review
3. **Address Feedback**: Make requested changes
4. **Final Approval**: Get approval from maintainer
5. **Merge**: Merge to main branch

## Performance Guidelines

### Memory Management
- **Object Reuse**: Reuse objects when possible
- **Resource Cleanup**: Properly close streams and connections
- **Memory Leaks**: Avoid circular references and static collections

### UI Performance
- **SwingUtilities.invokeLater**: Use for UI updates
- **Background Threads**: Keep UI responsive
- **Lazy Loading**: Load data only when needed

### Network Operations
- **Connection Pooling**: Reuse HTTP connections
- **Timeout Handling**: Set appropriate timeouts
- **Error Recovery**: Implement retry mechanisms

## Security Considerations

### Input Validation
- **URL Validation**: Validate all input URLs
- **File Paths**: Prevent directory traversal attacks
- **Plugin Security**: Validate plugin files before loading

### Network Security
- **HTTPS**: Prefer secure connections
- **Certificate Validation**: Validate SSL certificates
- **Proxy Security**: Secure proxy configuration

## Troubleshooting

### Common Issues
- **Build Failures**: Check Java version and Gradle compatibility
- **Test Failures**: Ensure all dependencies are available
- **Runtime Errors**: Check classpath and resource loading

### Debug Tools
- **Logging**: Use application logging for debugging
- **IDE Debugger**: Set breakpoints and inspect variables
- **JVM Options**: Use `-Xdebug` for remote debugging

## Contributing

### Getting Started
1. Read this development guide
2. Set up your development environment
3. Choose an issue to work on
4. Follow the development workflow
5. Submit your contribution

### Communication
- **Issues**: Use GitHub issues for bug reports and feature requests
- **Discussions**: Use GitHub discussions for questions and ideas
- **Pull Requests**: Provide clear descriptions and context

---

For additional information, see the [README.md](README.md) and [API Documentation](docs/api/).
