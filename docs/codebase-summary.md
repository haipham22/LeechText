# Codebase Summary - LeechText

## Project Statistics

- **Total Java Files**: ~135+ files (updated to include JavaScript engine)
- **Total Lines of Code**: ~14,500+ lines (updated to include recent additions)
- **Main Package**: `dark.leech.text`
- **Build System**: Gradle 8.4
- **Java Version**: 17

## Package Structure

```
src/main/java/dark/leech/text/
├── action/          # Core application actions (7 files, 1419 LOC)
├── animation/       # UI animation system (8 files, 380 LOC)
├── enities/         # Data entities (4 files, 171 LOC)
├── get/             # Content retrieval logic (6 files, 343 LOC)
├── image/           # Image processing (4 files, 831 LOC)
├── listeners/       # Event listeners (7 files, 48 LOC)
├── lua/             # Lua script engine integration (16 files)
├── models/          # Data models (7 files, 327 LOC)
├── plugin/          # Plugin management (19+ files, 1263+ LOC)
│   ├── js/          # JavaScript engine with vBook API support (18 files)
│   ├── security/    # Plugin security validation (7 files)
│   ├── validation/  # Plugin scanning and validation (2 files)
│   └── sandbox/     # Sandboxed execution environment
├── ui/              # User interface (55 files, 7069 LOC)
└── util/            # Utility classes (17 files, 2127+ LOC)
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
| `api/JsScriptEngine.java` | Rhino JavaScript engine wrapper |
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
- **JSoup**: HTML parsing and scraping
- **Gson**: JSON serialization/deserialization
- **LuaJ**: Lua script execution
- **Zip4j**: EPUB creation and ZIP handling
- **HttpClient5**: HTTP communication
- **RSyntaxTextArea**: Code editing
- **Lombok**: Annotation-based code generation

### Quality Tools
- **Spotless**: Code formatting
- **Checkstyle**: Style checking
- **PMD**: Code quality
- **JUnit**: Unit testing
- **Mockito**: Mocking

### Recent Enhancements
- **Rhino JavaScript Engine**: GraalVM-based JavaScript support with vBook API compatibility
- **WebP Image Converter**: Automatic conversion of WebP covers to JPEG for EPUB compatibility
- **Plugin Security Validation**: Network, regex, and archive security scanning
- **HTML Sanitizer**: EPUB XML validation and tag cleanup
