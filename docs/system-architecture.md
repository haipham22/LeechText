# System Architecture - LeechText

## Architectural Overview

LeechText follows a **layered architecture** with clear separation of concerns, designed around a plugin-based extensibility model.

```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                     │
│              (Swing UI with Material Design Components)        │
├─────────────────────────────────────────────────────────────────┤
│  MainUI  │  DownloadUI  │  SettingUI  │  PluginUI            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          Action Layer                          │
│              (Business Logic & Orchestration)                  │
├─────────────────────────────────────────────────────────────────┤
│  Download  │  Export  │  Config  │  History  │  Log           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       Content Retrieval Layer                  │
│              (HTTP, Parsing, Dual Plugin Execution)           │
├─────────────────────────────────────────────────────────────────┤
│  PageGetter  │  ChapExecute  │  ListExecute  │  PluginManager  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          Data Layer                            │
│                  (Models, Entities, Settings)                  │
├─────────────────────────────────────────────────────────────────┤
│  Chapter  │  Pager  │  Properties  │  Settings  │  Repository │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        Utility Layer                           │
│              (File I/O, HTTP, Text Processing, etc.)           │
├─────────────────────────────────────────────────────────────────┤
│  FileUtils  │  Http  │  TextUtils  │  ZipUtils  │  Graphics  │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Presentation Layer

**Responsibility**: User interaction and display

#### Main UI Components
- **MainUI**: Primary application window hosting all panels
- **DownloadUI**: Download queue and progress tracking
- **SettingUI**: Application configuration interface
- **InfoUI**: Book/chapter information display
- **RepositoryUI**: Plugin repository management
- **LoginUI**: Authentication dialogs

#### Material Design Components
Custom UI library providing:
- `JMPanel`, `JMTextField`, `JMDialog`, `JMTable`
- `JMProgressBar`, `JMCheckBox`
- `SelectBox`, `JMPopupMenu`, `JMMenuItem`
- `DropShadowBorder` for depth effects

#### Animation System
- **Animation**: Base animation controller
- **RippleEffect**: Material ripple feedback
- **Timing Framework**: Keyframe-based animations

### 2. Action Layer

**Responsibility**: Business logic orchestration

#### Download Manager
```java
Download implements ChangeListener
├── States: DOWNLOADING, PAUSE, COMPLETED, CHECKING, CANCEL, ERROR
├── Actions: start(), pause(), resume(), cancel()
└── Notifies: DownloadListener callbacks
```

#### Export Handlers
- **Ebook.java**: EPUB generation with proper structure
- **Text.java**: Plain text export with formatting
- **ToC.java**: Table of Contents generation

#### Configuration Management
- **Config.java**: Application-wide settings
- **History.java**: Download history persistence
- **Log.java**: Application logging

### 3. Content Retrieval Layer

**Responsibility**: HTTP communication and content extraction

#### Plugin System Architecture

```
PluginManager (Singleton)
├── Plugin Discovery: Scan tools/plugins directory
├── Plugin Loading: Parse .plugin JSON files
├── Plugin Matching: Regex URL matching
└── Plugin Execution: Lua script execution

PluginEntity
├── name: Plugin name
├── regex: URL pattern
├── chap: Chapter extraction script (Lua)
├── toc: Table of contents script (Lua)
└── detail: Detail page script (Lua)
```

#### Content Execution
- **PageExecute**: Page content execution
- **ChapExecute**: Chapter content extraction
- **ListExecute**: Chapter list extraction
- **InfoExecute**: Metadata/info retrieval
- **InfoExecute**: Book information extraction

#### HTTP Layer
- **PageGetter**: Base HTTP client
- **LoginGetter**: Authenticated requests
- **HttpClient5**: Underlying HTTP library

### 4. JavaScript Integration (vBook Compatibility)

**Purpose**: Extensible content extraction via JavaScript with vBook plugin compatibility

#### JavaScript Engine Architecture

```
JavaScriptEngine (GraalVM)
├── Core GraalVM integration
├── Script execution context
├── ProxyObject pattern for method exposure
└── Value conversion system

JavaScript API (Available to Scripts)
├── Html: HTML parsing and selection (vBook-compatible)
├── Http: HTTP requests with method chaining
├── Json: JSON parsing and serialization
├── JSList: Array-like operations for vBook compatibility
└── Consumer: Functional interface for callbacks
```

#### JavaScript Loaders
- **JsDetailLoader**: Load detail page information with vBook API setup
- **JsTocLoader**: Load table of contents
- **JsTextLoader**: Load chapter text content

#### Context Management

```
Execution Context Flow
┌─────────────────────────────┐
│  Plugin Execution Context   │
├─────────────────────────────┤
│  - GraalVM Context         │
│  - API Bindings (Html, Http)│
│  - vBook API Compatibility │
│  - Value Conversion        │
└─────────────────────────────┘
```

#### ProxyObject Pattern

The JavaScript API uses ProxyObject pattern to expose Java methods to JavaScript with method chaining support:

```javascript
// Method chaining pattern (obj:method() instead of obj.method())
const title = html.parse(htmlString)
    .select('h1.title')
    .text()
    .trim();
```

#### Value Conversion System

- **Java to JavaScript**: Automatic conversion of primitive types, objects, and arrays
- **JavaScript to Java**: Conversion of objects, arrays, and primitives across language boundaries
- **Null handling**: Proper null value handling and error propagation
- **Type safety**: Type checking and conversion between Java and JavaScript types

#### Plugin Loading Flow

```
Plugin Loading Process
┌─────────────────────────────┐
│  Plugin Discovery           │
├─────────────────────────────┤
│  - Scan tools/plugins       │
│  - Parse .plugin JSON files │
│  - Match URL patterns      │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Context Initialization     │
├─────────────────────────────┤
│  - Create GraalVM Context  │
│  - Setup vBook API         │
│  - Bind API objects        │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Script Execution          │
├─────────────────────────────┤
│  - Parse JavaScript code   │
│  - Execute with context    │
│  - Handle API calls        │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Result Processing         │
├─────────────────────────────┤
│  - Convert to Java objects │
│  - Populate models        │
│  - Return to application  │
└─────────────────────────────┘
```

#### vBook API Compatibility

The JavaScript API is designed to be compatible with existing vBook plugins:

- **API Mapping**: Methods map directly to vBook equivalents
- **Method Chaining**: Supports vBook's method chaining pattern
- **Error Handling**: Returns null on errors instead of throwing exceptions
- **Context Management**: Explicit context requirement for proper Value creation

#### Performance Considerations

- **Connection Pooling**: Reuse HTTP connections for multiple requests
- **Memory Management**: Proper resource cleanup and context management
- **Script Execution**: Optimized GraalVM execution with caching
- **Value Conversion**: Efficient type conversion between Java and JavaScript

#### Security Features

- **Sandboxed Execution**: Limited access to system resources
- **Input Validation**: URL and content validation before processing
- **Error Isolation**: Isolated error handling for each plugin execution
- **Resource Limits**: Configurable execution time and memory limits

#### Extension Points

- **New JavaScript APIs**: Add custom API classes with ProxyObject implementation
- **Plugin Development**: Create new plugins with JavaScript extraction scripts
- **API Enhancement**: Extend existing APIs with additional functionality
- **Integration**: Connect JavaScript plugins with existing Lua-based plugins

#### Migration Path

For developers migrating from vBook:

1. **Update API Calls**: Replace vBook API calls with LeechText equivalents
2. **Context Handling**: Add context parameters to API constructors
3. **Method Chaining**: Adapt to LeechText's method chaining pattern
4. **Error Handling**: Implement proper error handling for null returns
5. **Testing**: Update tests for new API behavior

#### JavaScript API Classes

##### Html API
- HTML parsing, selection, and manipulation
- XPath support via JAXP with W3C DOM conversion
- Method chaining for jQuery-like syntax
- Element manipulation and cleaning

##### Http API
- HTTP request capabilities with method chaining
- Support for GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- Form encoding and query parameter handling
- Response parsing (HTML, JSON, text, bytes)

##### Json API
- JSON parsing and serialization
- Path-based value extraction
- Type conversion between JavaScript and Java
- Pretty printing and validation

##### JSList API
- Array-like functionality for plugin development
- ProxyArray implementation for JavaScript array operations
- Map and forEach methods for transformation
- Conversion to JavaScript arrays

##### Consumer Interface
- Functional interface for callback operations
- Integration with map and forEach methods
- JavaScript-style function handling

#### JavaScript Plugin Development Standards

```javascript
// Recommended plugin structure
function extractData(html, context) {
    // Initialize APIs with context
    const htmlApi = new Html(context);
    const httpApi = new Http(context);

    try {
        // Parse HTML
        const doc = htmlApi.parse(html);

        // Extract data using method chaining
        const title = doc.select('h1.title').text();
        const chapters = doc.select('.chapter-list').map(function(chapter) {
            return {
                title: chapter.select('h3').text(),
                url: chapter.select('a').attr('href')
            };
        });

        return { title, chapters };

    } catch (e) {
        Log.add('Extraction failed: ' + e.message);
        return null;
    }
}
```

#### Testing JavaScript Plugins

```javascript
// Unit test for JavaScript plugin
function testPlugin() {
    const context = getContext(); // Get execution context
    const htmlApi = new Html(context);

    // Test HTML parsing
    const testHtml = '<html><body><h1>Test</h1><p>Hello</p></body></html>';
    const doc = htmlApi.parse(testHtml);
    assertEquals('Test', doc.select('h1').text());

    // Test HTTP requests (mocked)
    const mockResponse = http.get('https://test.com').string();
    assertEquals('Mock response', mockResponse);
}
```

#### JavaScript Plugin Examples

- **Chapter List Extraction**: Extract chapter lists from novel websites
- **Content Extraction**: Extract chapter content with image processing
- **API Authentication**: Handle login and session management
- **Form Submission**: Submit forms and handle responses
- **JSON Processing**: Extract and process API data

#### JavaScript Plugin Deployment

1. Create JavaScript plugin file with `.js` extension
2. Place in `tools/plugins/` directory
3. Define plugin configuration in `.plugin` JSON file
4. Test with LeechText's plugin browser
5. Deploy to production environment

#### JavaScript Plugin Configuration

```json
{
  "name": "Example JavaScript Plugin",
  "version": "1.0",
  "regex": "example\\.com",
  "chap": "chapter extraction script",
  "toc": "table of contents script",
  "detail": "detail page script",
  "javascript": true
}
```

#### JavaScript Plugin Execution

```
Plugin Execution Flow
┌─────────────────────────────┐
│  Plugin Discovery           │
├─────────────────────────────┤
│  - Scan tools/plugins       │
│  - Match URL patterns      │
│  - Load JavaScript files   │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Context Setup             │
├─────────────────────────────┤
│  - Create GraalVM Context  │
│  - Bind vBook APIs        │
│  - Initialize plugin       │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Script Execution          │
├─────────────────────────────┤
│  - Parse JavaScript code   │
│  - Execute with context    │
│  - Call API methods       │
└─────────────────────────────┘
          ↓
┌─────────────────────────────┐
│  Result Processing         │
├─────────────────────────────┤
│  - Convert to Java objects │
│  - Populate models        │
│  - Return to application  │
└─────────────────────────────┘
```

#### JavaScript Plugin Benefits

- **vBook Compatibility**: Seamless migration from existing vBook plugins
- **Modern JavaScript**: Leverage modern JavaScript features and patterns
- **Performance**: Optimized execution with GraalVM
- **Extensibility**: Easy to extend and customize
- **Maintainability**: Clear separation of concerns and proper error handling

### 5. Data Layer

**Purpose**: Data structures and persistence

#### Core Models
```java
Chapter {
    String url;
    String partName;
    String chapName;
    String id;
    boolean completed;
    boolean error;
    boolean empty;
    boolean imageChapter;
    boolean purchase;
}

Pager {
    // Pagination information
}

Properties {
    List<Chapter> chapList;
    List<Pager> pageList;
    String url;
    String charset;
    boolean isForum;
    // Download configuration
}
```

#### Settings & Configuration
- **Settings**: Application preferences
- **Repository**: Plugin repository configuration
- **Trash**: Deleted items management

### 6. Utility Layer

**Purpose**: Cross-cutting utilities

#### File Operations
- **FileUtils**: File I/O operations with WebP conversion support
- **ZipUtils**: ZIP/EPUB creation
- **AppUtils**: Application-level utilities

#### Text Processing
- **TextUtils**: Text manipulation
- **StringUtils**: String utilities
- **SyntaxUtils**: Syntax highlighting

#### Graphics & UI
- **GraphicsUtils**: Graphics operations (824 LOC)
- **ColorUtils**: Color manipulation
- **FontUtils**: Font handling
- **ImageLabel**: Image display with WebP conversion

#### Image Utilities (NEW)
- **ImageConverter**: WebP to JPEG conversion utility
- **HtmlSanitizer**: HTML sanitization for EPUB validation

#### HTTP & Network
- **Http**: HTTP client wrapper
- **CookiesUtils**: Cookie management

#### Other Utilities
- **RegexUtils**: Regular expression helpers
- **Base64**: Encoding/decoding
- **TypeUtils**: Type conversions
- **SafePropertySetter**: Safe property setting

## Data Flow

### Download Flow

```
User Action (Add URL)
    ↓
RepositoryUI/DownloadUI
    ↓
Download.start()
    ↓
PluginManager.get(url) → Match Plugin
    ↓
ListExecute → Get Chapter List
    ↓
ChapExecute (Multi-threaded) → Download Chapters
    ↓
DownloadListener.updateDownload() → Progress Updates
    ↓
Export Handler → Generate Output
    ↓
File Saved
```

### Plugin Execution Flow

```
URL Input
    ↓
PluginManager.get(url) → Regex Match
    ↓
Load PluginEntity
    ↓
Execute Lua Script
    ↓
Lua API Calls (Http, Html, Text)
    ↓
Return Parsed Data
    ↓
Populate Models (Chapter, Pager, etc.)
    ↓
Update UI
```

## Threading Model

### UI Thread (Event Dispatch Thread)
- All Swing UI updates
- Event listener callbacks
- Animation rendering

### Worker Threads
- Download operations (configurable thread pool)
- Plugin execution
- File I/O operations
- Export generation

### Thread Coordination
```java
// Example: Download threading
public void startDownload() {
    status = DOWNLOADING;
    next = next + MAX_CONN - 1;
    update();
    for (int i = 0; i < MAX_CONN; i++) {
        if (properties.isForum())
            forum(downloaded + i);  // Spawn thread
        else
            web(downloaded + i);    // Spawn thread
    }
}
```

## Extension Points

### Adding New Export Format
1. Create new export class in `action/export/`
2. Implement export logic
3. Add UI option in Export dialogs
4. Register with Export handlers

### Adding New Plugin
1. Create `.plugin` JSON file
2. Write Lua extraction scripts
3. Place in `tools/plugins/` directory
4. Plugin auto-discovery on startup

### Adding New Lua API
1. Create class in `plugin/lua/api/`
2. Implement functionality
3. Expose to Lua via LuaScriptEngine
4. Update plugin documentation

## Security Considerations

### Input Validation
- URL validation before processing
- Plugin regex validation
- File path sanitization

### Sandboxing
- Lua scripts run in LuaJ sandbox
- Limited Lua API exposure
- No direct filesystem access from Lua

### JavaScript Sandboxing (NEW)
- Rhino JavaScript engine with limited context
- API access validation through ProxyObject pattern
- Resource limits for script execution

### Plugin Security (NEW)
- NetworkSecurityValidator: HTTPS validation and content-type checking
- RegexSecurityValidator: Plugin pattern validation
- ZipSecurityValidator: Archive scanning for malicious content
- SecurityConstants: Security configuration parameters

### Network Security
- Cookie management for authenticated sites
- HTTPS support
- Cloudflare bypass (careful implementation)

## Performance Optimizations

### Concurrent Downloads
- Configurable thread pool (MAX_CONN setting)
- Parallel chapter downloading
- Efficient connection reuse

### Memory Management
- Stream-based content processing
- Lazy loading of large content
- Proper resource cleanup

### Caching
- Plugin repository caching
- Download history persistence
- Settings lazy loading
