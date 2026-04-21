# Project Overview - LeechText

## Product Development Requirements (PDR)

### Project Identity

**Name**: LeechText
**Version**: 1.0.5
**Type**: Desktop Application
**Language**: Java 17
**License**: MIT

### Product Description

LeechText is a powerful Java-based text extraction and ebook creation application designed to help users download content from various web sources and convert them into multiple formats. The application features a modern, intuitive user interface with support for plugins, custom extraction rules, and batch processing.

### Core Features

#### Text Extraction
- Download and extract content from various web sources
- Multi-threaded download engine for efficiency
- Support for authenticated content via cookie management
- Cloudflare bypass support
- Regex-based content pattern matching
- Dual plugin system: Lua and JavaScript engines with vBook compatibility

#### Export Formats
- **EPUB**: Create ebooks with proper chapter structure, WebP cover conversion, and HTML sanitization
- **Plain Text**: Simple text output with basic formatting
- **Table of Contents**: Generate structured content outlines
- Custom format support via extensible export system
- **HTML Sanitization**: Automatic cleanup for EPUB XML validation (fixes unclosed tags, orphaned closing tags)

#### Plugin System
- Extensible architecture with custom extraction plugins
- Dual engine support: Lua and JavaScript (Rhino) with vBook API compatibility
- Plugin repository management with auto-update
- Comprehensive security validation: network, regex, and archive scanning
- Sandbox execution environment with resource limits
- Pagination support for JavaScript plugins (GenLoader) with cursor/token-based pagination
- Multiple loader types: LIST, DETAIL, TEXT, GEN for different content types

#### User Interface
- Modern Material Design-inspired components
- Dark/Light theme support
- Smooth animations and transitions
- Drag & Drop functionality
- Real-time download progress tracking

### Technical Requirements

#### Core Dependencies
- **JSoup 1.16.1**: HTML parsing and scraping
- **Gson 2.10.1**: JSON serialization/deserialization
- **Rhino 1.7.15**: JavaScript engine with vBook API compatibility
- **Zip4j 2.11.5**: EPUB creation and ZIP handling
- **HttpClient5 5.2.1**: HTTP communication
- **RSyntaxTextArea 3.3.4**: Code editing and syntax highlighting
- **Lombok 1.18.30**: Annotation-based code generation
- **Micrometer Core 1.11.0**: JavaScript engine monitoring

#### Build System
- **Gradle 8.4**: Build automation
- **Java 17**: Target runtime
- **Spotless**: Code formatting (Google Java Format 1.18.1)
- **Checkstyle**: Code style checking
- **PMD**: Code quality analysis
- **JUnit 4.13.2**: Unit testing
- **Mockito 5.4.0**: Mocking framework

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ MainUI   │  │SettingUI │  │DownloadUI│            │
│  └──────────┘  └──────────┘  └──────────┘            │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                    Action Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ Download │  │  Export  │  │  Config  │            │
│  └──────────┘  └──────────┘  └──────────┘            │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  Content Retrieval                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │  Getter  │  │ Execute  │  │  Plugin  │            │
│  └──────────┘  └──────────┘  └──────────┘            │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ Chapter  │  │  Pager   │  │   Model  │            │
│  └──────────┘  └──────────┘  └──────────┘            │
└─────────────────────────────────────────────────────────┘
```

### Key Components

#### Models
- **Chapter**: Represents a content chapter with URL, name, status
- **Pager**: Pagination information for multi-page content
- **Properties**: Download configuration and metadata
- **Repository**: Plugin repository configuration
- **Settings**: Application settings and preferences

#### Actions
- **Download**: Manages download process with status tracking
- **Config**: Application configuration management
- **Export**: Ebook, Text, and ToC export handlers
- **History**: Download history tracking
- **Log**: Application logging

#### UI Components
- **MainUI**: Primary application window
- **DownloadUI**: Download management interface
- **SettingUI**: Settings and preferences
- **RepositoryUI**: Plugin repository management
- **Material Components**: Custom UI component library

#### Plugin System
- **PluginManager**: Plugin loading and lifecycle management
- **Lua API**: HTTP, Text, HTML, JSON, Cloudflare utilities
- **JavaScript Engine**: Rhino-based JavaScript with vBook API compatibility
- **Security Validation**: Network, regex, and archive security scanning
- **Loaders**: Detail, Text, ToC (LIST), and paginated content (GEN) loaders
  - **DetailLoader**: Book metadata and information
  - **ListLoader**: Chapter lists and table of contents
  - **TextLoader**: Chapter content extraction
  - **GenLoader**: Paginated lists (novels, search results)
- **Response Model**: `Response.success(data)` and `Response.success(data, next)` for pagination
- **PaginationResult**: Generic model for paginated responses with items and next page metadata

### User Workflows

#### Basic Download Flow
1. Add URL to download queue
2. System auto-selects matching plugin
3. Configure download options (format, encoding)
4. Start download with progress tracking
5. Export to desired format

#### Plugin Development Flow
1. Create plugin definition JSON
2. Write Lua scripts for content extraction
3. Test with target website
4. Package as .plugin file
5. Deploy to tools/plugins directory

#### Custom Export Flow
1. Configure export settings
2. Apply find/replace patterns
3. Set chapter structure rules
4. Generate formatted output
5. Save to file system

### Non-Functional Requirements

#### Performance
- Support concurrent downloads (configurable thread count)
- Efficient memory usage for large content sets
- Responsive UI during long-running operations

#### Reliability
- Robust error handling for network issues
- Automatic retry mechanism for failed downloads
- State persistence for interrupted downloads

#### Maintainability
- Modular architecture for easy updates
- Clear separation of concerns
- Comprehensive logging for debugging

#### Usability
- Intuitive interface with minimal learning curve
- Keyboard shortcuts for common operations
- Clear visual feedback for all actions

### Future Enhancements

- [ ] Multi-language UI support (Vietnamese, English)
- [ ] Cloud synchronization for settings
- [ ] Mobile companion app
- [ ] Web-based interface
- [ ] Advanced content editing capabilities
- [ ] Integration with ebook management systems
