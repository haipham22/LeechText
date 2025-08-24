# Development Guidelines

This document outlines the development practices, code standards, and workflows for the LeechText Java project.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Code Standards](#code-standards)
3. [Commit Guidelines](#commit-guidelines)
4. [Testing](#testing)
5. [Build and CI](#build-and-ci)
6. [Development Workflow](#development-workflow)

## Getting Started

### Prerequisites

- Java 17 (OpenJDK)
- Git
- mise (for tool management)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd leechtext-java
   ```

2. **Install tools and dependencies**
   ```bash
   mise install
   ```

3. **Install pre-commit hooks**
   ```bash
   pre-commit install
   ```

4. **Build the project**
   ```bash
   ./gradlew build
   ```

## Code Standards

### Java Code Style

We use **Google Java Format** with AOSP variant for consistent code formatting:

- **Line length**: 100 characters
- **Indentation**: 4 spaces (no tabs)
- **Imports**: No wildcard imports
- **Naming**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.with.dots`

### Formatting and Linting

#### Automated Formatting
```bash
# Check formatting
./gradlew spotlessCheck

# Apply formatting fixes
./gradlew spotlessApply

# Run all quality checks
./gradlew qualityGate
```

#### Code Quality Tools

- **Spotless**: Automatic code formatting
- **Checkstyle**: Code style enforcement
- **PMD**: Static code analysis
- **Pre-commit hooks**: Automated checks on commit

### Best Practices

1. **No wildcard imports** - Use specific imports
2. **Proper exception handling** - Don't swallow exceptions
3. **Meaningful variable names** - Avoid abbreviations
4. **Documentation** - Add JavaDoc for public APIs
5. **Small methods** - Keep methods under 100 lines
6. **Single responsibility** - One class, one purpose

## Commit Guidelines

We follow **Conventional Commits** specification for consistent commit messages. While automated validation has been removed, please follow these guidelines manually.

### Commit Message Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Test additions or modifications
- `build`: Build system changes
- `ci`: CI/CD changes
- `chore`: Maintenance tasks

### Examples

```bash
# Good commit messages
feat: add new download progress indicator
fix: resolve memory leak in image processing
docs: update API documentation for PluginManager
refactor: extract common validation logic
style: format code according to Google style guide

# Bad commit messages
fixed stuff
update
new feature
```

### Manual Validation

Since automated commit message validation has been removed, please ensure your commit messages follow the conventional format before committing.

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ClassName"

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Guidelines

1. **Test naming**: Use descriptive names
2. **Test structure**: Follow Given-When-Then pattern
3. **Assertions**: Use meaningful assertion messages
4. **Mocking**: Mock external dependencies
5. **Coverage**: Aim for >80% code coverage

## Build and CI

### Gradle Tasks

```bash
# Compile source code
./gradlew compileJava

# Run full build
./gradlew build

# Run application
./gradlew run

# Clean build artifacts
./gradlew clean

# Run pre-commit checks
./gradlew preCommit

# Run quality gate (all checks)
./gradlew qualityGate
```

### Pre-commit Hooks

The following checks run automatically before each commit:

1. **Code formatting** (Spotless)
2. **Compilation check**
3. **Trailing whitespace removal**
4. **File ending fixes**
5. **JSON/YAML validation**
6. **Merge conflict detection**
7. **Large file detection**
8. **Secret detection**

### Commit Message Validation

Commit messages are validated using commitlint to ensure they follow conventional commits.

## Development Workflow

### Feature Development

1. **Create feature branch**
   ```bash
   git checkout -b feat/your-feature-name
   ```

2. **Develop and test**
   - Write code following standards
   - Add/update tests
   - Run quality checks locally

3. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: add new feature description"
   ```

4. **Push and create PR**
   ```bash
   git push origin feat/your-feature-name
   ```

### Code Review Guidelines

1. **Review for functionality** - Does it work as expected?
2. **Review for style** - Follows code standards?
3. **Review for tests** - Adequate test coverage?
4. **Review for documentation** - Is it documented?
5. **Review for performance** - Any performance concerns?

### Release Process

1. **Version bump** in relevant files
2. **Update changelog**
3. **Create release tag**
4. **Build and test**
5. **Deploy artifacts**

## IDE Configuration

### IntelliJ IDEA

1. **Import project** as Gradle project
2. **Install plugins**:
   - Google Java Format
   - Checkstyle-IDEA
   - PMD Plugin
3. **Configure code style**: Import Google Java Format settings
4. **Enable auto-formatting** on save

### VS Code

1. **Install extensions**:
   - Extension Pack for Java
   - Checkstyle for Java
   - Spotless Gradle
2. **Configure settings** for auto-formatting

## Troubleshooting

### Common Issues

1. **Formatting violations**
   ```bash
   ./gradlew spotlessApply
   ```

2. **Pre-commit hook failures**
   ```bash
   pre-commit run --all-files
   ```

3. **Commit message rejection**
   - Follow conventional commits format
   - Check commitlint rules

4. **Build failures**
   ```bash
   ./gradlew clean build
   ```

### Getting Help

- Check existing documentation
- Review error messages carefully
- Ask team members for guidance
- Create GitHub issues for bugs

## Tools and Dependencies

### Development Tools

- **Gradle**: Build system
- **Spotless**: Code formatting
- **Checkstyle**: Style checking
- **PMD**: Static analysis
- **Pre-commit**: Git hooks management
- **Commitlint**: Commit message validation
- **Husky**: Git hooks (via npm)

### Runtime Dependencies

- **Java 17**: Runtime environment
- **JSoup**: HTML parsing
- **LuaJ**: Lua scripting
- **Gson**: JSON processing
- **zip4j**: Archive handling
- **RSyntaxTextArea**: Syntax highlighting

## Contributing

1. **Fork the repository**
2. **Follow development guidelines**
3. **Write tests for new features**
4. **Ensure all checks pass**
5. **Submit pull request**
6. **Participate in code review**

---

**Note**: This document is living documentation. Please keep it updated as the project evolves.
