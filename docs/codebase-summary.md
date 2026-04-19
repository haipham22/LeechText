# Codebase Summary - LeechText

## Project Statistics

- **Total Java Files**: ~140+ files (including JavaScript engine components)
- **Total Lines of Code**: ~15,000+ lines (including recent additions)
- **Main Package**: `dark.leech.text`
- **Build System**: Gradle 8.4
- **Java Version**: 17
- **Application Version**: 1.0.5

## Package Structure

```
src/main/java/dark/leech/text/
├── action/          # Core application actions (7 files, 1419 LOC)
├── animation/       # UI animation system (8 files, 380 LOC)
├── enities/         # Data entities (4 files, 171 LOC)
├── get/             # Content retrieval logic (6 files, 343 LOC)
├── image/           # Image processing with WebP support (4 files, 831 LOC)
├── listeners/       # Event listeners (7 files, 48 LOC)
├── models/          # Data models (7 files, 327 LOC)
├── plugin/          # Plugin management with dual-engine support (19+ files, 1263+ LOC)
│   ├── js/          # JavaScript (Rhino) engine with vBook API compatibility (18 files)
│   ├── security/    # Plugin security validation (7 files)
│   ├── validation/  # Plugin scanning and validation (2 files)
│   ├── sandbox/     # Sandboxed execution environment with resource limits
│   └── vbook/       # vBook plugin support
├── ui/              # User interface (55+ files, 7069 LOC)
└── util/            # Utility classes including ImageConverter, HtmlSanitizer (17+ files, 2127+ LOC)
```

## Package Details

### action/ - Core Actions
**Purpose**: Core business logic and application actions

| File | Description | LOC |
|------|-------------|-----|
| `Download.java` | Main download manager with state tracking | 133 |
| `Config.java` | Application configuration management | 305 |
| `History.java` | Download history tracking | - |
| `Log.java` | Application logging | - |
| `export/Ebook.java` | EPUB export functionality with WebP cover support | 276 |
| `export/Text.java` | Plain text export | 230 |
| `export/ToC.java` | Table of Contents generation | 340 |

### get/ - Content Retrieval
**Purpose**: HTTP content fetching and execution engines

| File | Description |
|------|-------------|
| `PageGetter.java` | Base HTTP page fetching |
| `LoginGetter.java` | Authenticated content retrieval |
| `PageExecute.java` | Page content execution (108 LOC) |
| `ChapExecute.java` | Chapter content execution |
| `ListExecute.java` | List/chapter list execution |
| `InfoExecute.java` | Metadata/info retrieval |

### models/ - Data Models
**Purpose**: Core data structures with Lombok annotations

| File | Description |
|------|-------------|
| `Chapter.java` | Chapter model (URL, name, status, ID) |
| `Pager.java` | Pagination model |
| `Settings.java` | Application settings |
| `Properties.java` | Download properties |
| `Repository.java` | Plugin repository config |
| `Post.java` | Post data model |
| `Trash.java` | Deleted items model |

### plugin/ - Plugin System
**Purpose**: Extensible plugin architecture with Lua and JavaScript engines, plus security validation

| File | Description | LOC |
|------|-------------|-----|
| `PluginManager.java` | Plugin loading and lifecycle | 63 |
| `RepositoryManager.java` | Repository management | - |
| `PluginUpdate.java` | Auto-update functionality | 126 |

**js/** - JavaScript Engine with vBook API Support:
| File | Purpose |
|------|---------|
| `api/Html.java` | HTML parsing and selection (vBook-compatible, method chaining) |
| `api/Http.java` | HTTP requests with method chaining |
| `api/Json.java` | JSON parsing and serialization |
| `api/JSList.java` | Array-like operations for vBook compatibility |
| `JsScriptEngine.java` | Rhino JavaScript engine wrapper |
| `loader/TextLoader.java` | Text content loading with vBook API setup |
| `loader/DetailLoader.java` | Detail page loading |
| `loader/ListLoader.java` | Chapter list extraction |

**security/** - Plugin Security Validation:
| File | Purpose |
|------|---------|
| `NetworkSecurityValidator.java` | HTTPS validation and content-type checking |
| `RegexSecurityValidator.java` | Regex pattern validation |
| `ZipSecurityValidator.java` | Archive security scanning |
| `SecurityConstants.java` | Security configuration |

**lua/api/** - Lua Script APIs:
| File | Purpose |
|------|---------|
| `Http.java` | HTTP requests for Lua scripts (171 LOC) |
| `Text.java` | Text processing utilities (161 LOC) |
| `Html.java` | HTML parsing |
| `Json.java` | JSON handling |
| `Cloudflare.java` | Cloudflare bypass |
| `Regexp.java` | Regex operations |
| `CookieManager.java` | Cookie management |
| `LuaScriptEngine.java` | Lua engine wrapper |
| `Core.java`, `Error.java`, `Num.java` | Core utilities |

**lua/loader/**:
| File | Purpose |
|------|---------|
| `TextLoader.java` | Text content loading |
| `TocLoader.java` | Table of Contents loading (127 LOC) |
| `DetailLoader.java` | Detail page loading |

### ui/ - User Interface
**Purpose**: Swing-based Material Design UI components

#### Main UI (ui/main/)
| File | Description | LOC |
|------|-------------|-----|
| `App.java` | Application entry point | 40 |
| `MainUI.java` | Main window | 416 |
| `SettingUI.java` | Settings panel | 214 |
| `DownloadUI.java` | Download manager (112 LOC) |
| `InfoUI.java` | Book info display | 297 |
| `RepositoryUI.java` | Plugin repository UI | 207 |
| `LoginUI.java` | Authentication dialog | 133 |
| `HelpUI.java` | Help documentation | 113 |
| `UpdateUI.java` | Update manager | 116 |

#### Download UI (ui/download/)
| File | Description | LOC |
|------|-------------|-----|
| `DownloadLabel.java` | Download item widget | 273 |
| `AddURL.java` | URL input dialog | 146 |
| `AddDialog.java` | Add download dialog | 277 |
| `MultiURL.java` | Batch URL input | - |

#### Material Components (ui/material/)
| File | Description |
|------|-------------|
| `JMPanel.java` | Custom panel |
| `JMTextField.java` | Text field (136 LOC) |
| `JMDialog.java` | Dialog wrapper (129 LOC) |
| `JMScrollPane.java` | Scroll pane |
| `JMTable.java` | Table component |
| `JMProgressBar.java` | Progress bar |
| `JMCheckBox.java` | Checkbox |
| `JMPopupMenu.java`, `JMMenuItem.java` | Menu components |
| `SelectBox.java` | Select box (156 LOC) |
| `DropShadowBorder.java` | Shadow effects (421 LOC) |

#### Button Components (ui/button/)
| File | Description |
|------|-------------|
| `BasicButton.java` | Base button |
| `SelectButton.java` | Selection button |
| `CircleButton.java` | Circular button |
| `CloseButton.java` | Close button |
| `ButtonUI.java` | Button UI delegate |

#### Settings UI (ui/setting/)
| File | Description |
|------|-------------|
| `Theme.java` | Theme management (125 LOC) |
| `ToolPane.java` | Tools panel |
| `ItemStyle.java` | Style items (110 LOC) |
| `ItemCheckBox.java` | Checkbox items |
| `ItemConn.java` | Connection items |
| `trash/` | Trash management UI |

#### Other UI
| File | Description |
|------|-------------|
| `notification/Notification.java` | Notifications (146 LOC) |
| `notification/Toast.java` | Toast messages |
| `notification/Alert.java` | Alert dialogs |
| `repository/RepositoryTile.java` | Repository tiles |
| `repository/AddRepositoriesDialog.java` | Add repo dialog |

### util/ - Utilities
**Purpose**: Common utility functions

| File | Description | LOC |
|------|-------------|-----|
| `GraphicsUtils.java` | Graphics utilities | 824 |
| `SettingUtils.java` | Settings management | 222 |
| `FileUtils.java` | File operations with WebP conversion support | 192 |
| `Http.java` | HTTP client wrapper | 130 |
| `AppUtils.java` | Application utilities | 150 |
| `ZipUtils.java` | ZIP operations | 106 |
| `ImageConverter.java` | **NEW**: WebP to JPEG conversion utility | - |
| `HtmlSanitizer.java` | **NEW**: HTML sanitization for EPUB | - |
| `TextUtils.java` | Text processing | - |
| `StringUtils.java` | String helpers | - |
| `ColorUtils.java` | Color utilities | - |
| `FontUtils.java` | Font utilities | - |
| `CookiesUtils.java` | Cookie management | - |
| `RegexUtils.java` | Regex utilities | - |
| `SyntaxUtils.java` | Syntax highlighting | - |
| `TypeUtils.java` | Type conversions | - |
| `Base64.java` | Base64 encoding | - |
| `SafePropertySetter.java` | Safe property setting (125 LOC) |

### animation/ - Animation System
**Purpose**: UI animations and transitions

| File | Description |
|------|-------------|
| `Animation.java` | Animation controller |
| `RippleEffect.java` | Material ripple effect (150 LOC) |
| `timing/Animator.java` | Core animator |
| `timing/KeyFrames.java` | Keyframe animation |
| `timing/TimingTarget.java` | Animation target |
| `timing/SplineInterpolator.java` | Spline interpolation |
| `timing/AccelerationInterpolator.java` | Acceleration |
| `timing/SwingTimerTimingSource.java` | Timer source |

### image/ - Image Processing
**Purpose**: Image utilities and effects with WebP support

| File | Description | LOC |
|------|-------------|-----|
| `AbstractBean.java` | Image bean base | 436 |
| `ImageLabel.java` | Image display component with WebP conversion | 122 |
| `GaussianBlurFilter.java` | Blur effect | 188 |
| `AbstractFilter.java` | Filter base class |

### listeners/ - Event Listeners
**Purpose**: Event handling interfaces

| File | Description |
|------|-------------|
| `ChangeListener.java` | State change listener |
| `DownloadListener.java` | Download progress |
| Other specialized listeners | - |

### enities/ - Data Entities
**Purpose**: Entity classes with Lombok

| File | Description |
|------|-------------|
| `PluginEntity.java` | Plugin definition |
| Other entity classes | - |

## External Libraries Used

```
net/java/balloontip/          # Balloon tip library
├── BalloonTip.java           # Main tooltip component
├── styles/                   # Style implementations
└── utils/TimingUtils.java    # Timing utilities
```

## Key Design Patterns

1. **Singleton Pattern**: PluginManager, DownloadManager
2. **Observer Pattern**: ChangeListener, DownloadListener
3. **Strategy Pattern**: Plugin-based content extraction
4. **Factory Pattern**: Plugin creation and loading
5. **Builder Pattern**: Configuration builders

## Entry Points

| Class | Purpose |
|-------|---------|
| `dark.leech.text.ui.main.App` | Main application entry |
| `dark.leech.text.ui.main.MainUI` | Primary UI window |
| `dark.leech.text.action.Download` | Download orchestrator |
| `dark.leech.text.plugin.PluginManager` | Plugin system |

## Dependencies Overview

### Core Dependencies
- **JSoup 1.16.1**: HTML parsing and scraping
- **Gson 2.10.1**: JSON serialization/deserialization
- **Rhino 1.7.15**: JavaScript engine with vBook API compatibility
- **Zip4j 2.11.5**: EPUB creation and ZIP handling
- **HttpClient5 5.2.1**: HTTP communication
- **RSyntaxTextArea 3.3.4**: Code editing and syntax highlighting
- **Lombok 1.18.30**: Annotation-based code generation
- **Micrometer Core 1.11.0**: JavaScript engine monitoring
- **Apache Commons**: Lang3 (3.12.0), Collections4 (4.4) for utility functions

### Quality Tools
- **Spotless**: Code formatting (Google Java Format 1.18.1)
- **Checkstyle**: Style checking with 10.12.5
- **PMD**: Code quality with 6.55.0
- **JUnit 4.13.2**: Unit testing
- **Mockito 5.4.0**: Mocking framework

### Recent Enhancements (v1.0.5)
- **Rhino JavaScript Engine**: Native JavaScript support with vBook API compatibility (replaced GraalVM)
- **WebP Image Converter**: Automatic conversion of WebP covers to JPEG for EPUB compatibility
- **Plugin Security Validation**: Network, regex, and archive security scanning
- **HTML Sanitizer**: EPUB XML validation and tag cleanup utilities

## Build Configuration

### Gradle Properties
```properties
# Project configuration
org.gradle.jvmargs=-Xmx2g -Xms512m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Java configuration
java.sourceCompatibility=17
java.targetCompatibility=17

# Application properties
app.name=LeechText
app.version=1.0.5
app.mainClass=dark.leech.text.ui.main.App
```

### Application Configuration
```properties
# JVM arguments for the application
-Xmx1g
-Dapp.home.dir=${project.hasProperty('app.home.dir') ? project.getProperty('app.home.dir') : System.getProperty('user.dir')}
--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED
--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED
```

## Key Features

### 1. Dual Plugin Engine System
- **Lua Engine**: Traditional Lua script support for content extraction
- **JavaScript Engine**: Rhino-based JavaScript with vBook API compatibility
- **Auto-detection**: Automatic plugin selection based on URL patterns
- **Security Validation**: Comprehensive plugin scanning and sandboxing

### 2. Advanced Export Capabilities
- **EPUB Generation**: Structured ebook creation with WebP→JPEG conversion
- **HTML Sanitization**: Automatic cleanup for EPUB XML validation
- **Multiple Formats**: Plain text, structured text, custom formats
- **Metadata Support**: Chapter information, cover images, table of contents

### 3. Modern UI Framework
- **Material Design**: Custom Swing components with Material-inspired design
- **Theme Support**: Dark/light theme switching
- **Animation System**: Smooth transitions and visual feedback
- **Responsive Layout**: Adaptive design for different screen sizes

### 4. Robust Plugin Security
- **Network Validation**: HTTPS enforcement and content-type checking
- **Regex Security**: Pattern validation to prevent malicious plugins
- **Archive Scanning**: ZIP file security validation for plugin packages
- **Resource Limits**: Execution time and memory constraints for plugins

### 5. Multi-Platform Support
- **macOS**: Native application support with dock integration
- **Windows**: Native executable support
- **Linux**: Debian package support
- **Cross-Platform**: Java-based architecture for consistent behavior

## Development Workflow

### Build Commands
```bash
# Clean build
./gradlew clean build

# Build distribution
./gradlew jar

# Run application
./gradlew run

# Run with custom configuration
./gradlew runDev
```

### Quality Assurance
```bash
# Code formatting
./gradlew spotlessApply

# Format check
./gradlew spotlessCheck

# Style checks
./gradlew checkstyleMain

# Code quality analysis
./gradlew pmdMain

# All quality checks
./gradlew qualityGate
```

## Performance Optimizations

### Concurrency
- Multi-threaded download engine (configurable thread count)
- Parallel plugin execution
- Connection pooling for HTTP requests

### Memory Management
- Stream-based content processing
- Lazy loading of large content
- Proper resource cleanup and garbage collection

### Caching
- Plugin repository caching
- Download history persistence
- Settings lazy loading

## Integration Points

### Plugin System
- Plugin discovery and auto-loading
- JSON configuration validation
- Script execution with error handling
- API exposure (HTTP, HTML, JSON, Regex)

### Export System
- EPUB generation with proper structure
- HTML sanitization for XML compliance
- Image format conversion (WebP→JPEG)
- Metadata preservation and formatting

### UI Framework
- Material Design components
- Event-driven architecture
- Animation system with timing framework
- Theme and styling system

## Technical Debt

### Known Issues
1. **Package Naming**: `enities` should be `entities`
2. **Test Coverage**: Currently minimal, needs comprehensive suite
3. **Thread Safety**: Some areas may need additional synchronization
4. **Memory Management**: Potential leaks in long-running operations

### Future Improvements
1. **Java 21 Migration**: Upgrade to latest LTS version
2. **Native Packaging**: Use jpackage for native installers
3. **Enhanced Testing**: Unit and integration test coverage
4. **Performance Profiling**: Optimize hot paths and memory usage

## Version History

### v1.0.5 (Current)
- Added Rhino JavaScript engine with vBook API compatibility
- Implemented WebP to JPEG conversion for EPUB covers
- Created comprehensive plugin security validation system
- Added HTML sanitization for EPUB XML validation
- Updated build system and dependencies

### v1.0.0
- Gradle 8.4 migration
- Java 17 upgrade
- Code quality improvements with Spotless, Checkstyle, PMD
- Multi-platform CI/CD pipeline

## Continuous Integration

### GitHub Actions Workflows
- **Multi-platform builds**: Automated builds for macOS, Windows, Linux
- **Code quality**: Automated checks for style, formatting, quality
- **Documentation validation**: Automated documentation updates
- **Release automation**: Automated releases on version tags

### Build Artifacts
- `LeechText-mac.jar`: macOS application
- `LeechText-windows.jar`: Windows application
- `LeechText-linux.jar`: Linux application
- All artifacts include checksums for verification

---

This documentation provides a comprehensive overview of the LeechText codebase structure, components, and architecture. For more detailed information on specific areas, refer to the related documentation files in the `docs/` directory.
