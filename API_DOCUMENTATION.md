# API Documentation

This document provides comprehensive API documentation for the LeechText project, covering all public classes, methods, and their usage.

## Table of Contents

- [Core Application](#core-application)
- [User Interface](#user-interface)
- [Models and Entities](#models-and-entities)
- [Plugin System](#plugin-system)
- [Utilities](#utilities)
- [Event System](#event-system)
- [Export System](#export-system)

## Core Application

### App Class
**Package**: `dark.leech.text.ui.main`

Main application entry point and lifecycle manager.

#### Methods

##### `main(String[] args)`
Main entry point for the LeechText application.

**Parameters**:
- `args` - Command line arguments (currently not used)

**Description**: Initializes the application by setting up the Swing look and feel, loading utilities and settings, creating the main UI, and applying startup animations.

**Usage**:
```java
// Application entry point
public static void main(String[] args) {
    // Called automatically when JAR is executed
}
```

##### `getMain()`
Gets the main user interface instance.

**Returns**: The main user interface instance, or null if not yet initialized

**Usage**:
```java
MainUI mainUI = App.getMain();
if (mainUI != null) {
    // Access main UI components
}
```

## User Interface

### MainUI Class
**Package**: `dark.leech.text.ui.main`

Primary user interface window providing access to all major application features.

#### Constructor

##### `MainUI()`
Constructs the main user interface with custom styling and positioning.

**Description**: Initializes the main window with undecorated appearance, custom positioning based on screen dimensions, and background initialization.

#### Key Components

- **Status Bar**: Displays current time and provides window dragging
- **App Bar**: Contains logo and action buttons (add, menu)
- **Download UI**: Main content area for managing downloads
- **Settings UI**: Configuration and preferences panel
- **Plugin Management**: Interface for managing text extraction plugins

#### Methods

##### `onCreate()`
Initializes all UI components and layouts.

**Description**: Called in a separate thread to ensure smooth startup and prevent blocking the main thread during component initialization.

##### `onCreateStatusBar()`
Creates the status bar at the top of the window.

**Description**: Contains exit button, status label with current time, and provides window dragging functionality.

##### `onCreateAppBar()`
Creates the main application bar containing logo and action buttons.

**Description**: Positioned below the status bar with application logo, add button for new downloads, and menu button.

##### `createPanelHeaderUI()`
Creates the header panel for secondary views.

**Description**: Contains navigation elements like back button and is used when switching between different functional areas.

### DownloadUI Class
**Package**: `dark.leech.text.ui.download`

Manages the download interface and content extraction workflow.

#### Features
- URL input and validation
- Download progress tracking
- Content preview and management
- Batch download operations

### SettingUI Class
**Package**: `dark.leech.text.ui.setting`

Provides configuration and preferences management interface.

#### Features
- Theme selection (dark/light)
- Language preferences
- Download settings
- Export configuration

## Models and Entities

### Chapter Class
**Package**: `dark.leech.text.models`

Represents a chapter in a book or text document with metadata and status information.

#### Properties

- **url**: The URL where chapter content can be downloaded
- **partName**: Name of the part/volume this chapter belongs to
- **chapName**: Name/title of the chapter
- **completed**: Whether the chapter has been completely downloaded
- **error**: Whether an error occurred during download
- **empty**: Whether the chapter content is empty
- **imageChapter**: Whether this chapter contains images
- **id**: Unique identifier for the chapter
- **purchase**: Whether this chapter requires purchase

#### Constructors

##### `Chapter()`
Default constructor creating a chapter with an empty URL.

##### `Chapter(String url)`
Creates a chapter with the specified URL.

**Parameters**:
- `url` - The URL where the chapter content can be found

##### `Chapter(String url, String name)`
Creates a chapter with the specified URL and name.

**Parameters**:
- `url` - The URL where the chapter content can be found
- `name` - The name of the chapter

##### `Chapter(String url, int id, String partName, String chapName)`
Creates a chapter with URL, ID, part name, and chapter name.

**Parameters**:
- `url` - The URL where the chapter content can be found
- `id` - The numeric ID of the chapter
- `partName` - The name of the part/volume this chapter belongs to
- `chapName` - The name of the chapter

#### Methods

##### `getId()`
Gets the unique identifier for this chapter.

**Returns**: The chapter ID as a string

##### `setId(String id)`
Sets the unique identifier for this chapter.

**Parameters**:
- `id` - The new chapter ID

##### `setId(int id)`
Sets the unique identifier using a numeric value with "C" prefix.

**Parameters**:
- `id` - The numeric ID for the chapter

##### `getUrl()`
Gets the URL where the chapter content can be downloaded.

**Returns**: The chapter URL

##### `setUrl(String url)`
Sets the URL where the chapter content can be downloaded.

**Parameters**:
- `url` - The new chapter URL

##### `getChapName()`
Gets the name of the chapter.

**Returns**: The chapter name, or empty string if null

##### `setChapName(String chapName)`
Sets the name of the chapter.

**Parameters**:
- `chapName` - The new chapter name

##### `getPartName()`
Gets the name of the part/volume this chapter belongs to.

**Returns**: The part name, or empty string if null

##### `setPartName(String partName)`
Sets the name of the part/volume this chapter belongs to.

**Parameters**:
- `partName` - The new part name

##### `isError()`
Checks if an error occurred during the download.

**Returns**: true if an error occurred, false otherwise

##### `setError(boolean error)`
Sets the error status for this chapter.

**Parameters**:
- `error` - true if an error occurred, false otherwise

##### `isCompleted()`
Checks if the chapter has been completely downloaded.

**Returns**: true if completed, false otherwise

##### `setCompleted(boolean completed)`
Sets the completion status for this chapter.

**Parameters**:
- `completed` - true if completed, false otherwise

### PluginEntity Class
**Package**: `dark.leech.text.enities`

Represents a text extraction plugin with configuration and extraction rules.

#### Properties

- **uuid**: Unique identifier for the plugin
- **name**: Display name of the plugin
- **version**: Plugin version number
- **url**: Plugin source URL
- **language**: Supported language (vi, en)
- **icon**: Plugin icon in base64 format
- **source**: Source website the plugin handles
- **regex**: Regular expression for URL matching
- **author**: Plugin author information
- **describe**: Plugin description
- **group**: Plugin category (dich, convert, truyentranh)
- **data**: Plugin data in base64 format
- **supportUpdate**: Whether the plugin supports updates

#### Extraction Scripts

- **chap**: Chapter content extraction script
- **toc**: Table of contents extraction script
- **page**: Page list extraction script
- **search**: Search functionality script
- **detail**: Detail information extraction script

## Plugin System

### PluginManager Class
**Package**: `dark.leech.text.plugin`

Manages text extraction plugins with singleton pattern implementation.

#### Methods

##### `getManager()`
Gets the singleton instance of the plugin manager.

**Returns**: The singleton PluginManager instance

**Description**: Creates a new instance if one doesn't exist, implementing lazy initialization.

##### `add(String path)`
Adds a new plugin from the specified file path.

**Parameters**:
- `path` - The file path to the .plugin file

**Description**: Loads the plugin file and adds it to the internal plugin list for dynamic plugin addition.

##### `get(String url)`
Finds a plugin that can handle the specified URL.

**Parameters**:
- `url` - The URL to find a matching plugin for

**Returns**: The matching PluginEntity, or null if no plugin matches

**Description**: Iterates through all loaded plugins and checks if any regex patterns match the given URL.

##### `list()`
Gets a list of all loaded plugins.

**Returns**: An ArrayList containing all PluginEntity objects

##### `createPlugin(String path)`
Creates a PluginEntity from a plugin file.

**Parameters**:
- `path` - The file path to the plugin file

**Returns**: A PluginEntity object representing the loaded plugin

**Description**: Reads the plugin file content and deserializes it from JSON format using Gson.

## Utilities

### AppUtils Class
**Package**: `dark.leech.text.util`

Utility class providing core application functionality and constants.

#### Constants

- **VERSION**: Current application version (2019.03.30)
- **TIME**: Default time string for initialization
- **SEPARATOR**: System-specific file separator character
- **width**: Width of the primary display in pixels
- **height**: Height of the primary display in pixels
- **curDir**: Current working directory of the application
- **cacheDir**: Directory for caching application data
- **LOCATION**: Current location of the application window

#### Methods

##### `doLoad()`
Loads and initializes application configuration and syntax rules.

**Description**: Performs initialization tasks including directory path normalization, syntax configuration loading, chapter and part name pattern initialization, and text optimization rule setup.

**Throws**: RuntimeException if configuration loading fails (currently silently handled)

##### `getX()`
Gets the X coordinate of the application's current location.

**Returns**: The X coordinate of the application window

##### `getY()`
Gets the Y coordinate of the application's current location.

**Returns**: The Y coordinate of the application window

##### `getLocation()`
Gets the current location of the application window.

**Returns**: A Point object representing the current window location

##### `pause(int milliseconds)`
Pauses the current thread execution for the specified duration.

**Parameters**:
- `milliseconds` - The number of milliseconds to pause

**Description**: Provides a simple way to pause execution without handling InterruptedException in calling code.

### FileUtils Class
**Package**: `dark.leech.text.util`

Provides file and directory manipulation utilities.

#### Methods

##### `init()`
Initializes file utilities and creates necessary directories.

##### `validate(String path)`
Validates and normalizes file paths.

**Parameters**:
- `path` - The path to validate

**Returns**: Normalized and validated path string

##### `file2string(String path)`
Reads a file and returns its content as a string.

**Parameters**:
- `path` - The file path to read

**Returns**: File content as string

**Throws**: IOException if file cannot be read

##### `stream2string(String resourcePath)`
Reads a resource stream and returns its content as a string.

**Parameters**:
- `resourcePath` - The resource path to read

**Returns**: Resource content as string

**Throws**: IOException if resource cannot be read

### StringUtils Class
**Package**: `dark.leech.text.util`

Provides string manipulation and utility methods.

#### Constants

- **ADD**: Add button text
- **BACK**: Back button text
- **MORE**: More options button text
- **CHECK**: Check/confirm button text

#### Methods

##### `cleanText(String text)`
Cleans and normalizes text content.

**Parameters**:
- `text` - The text to clean

**Returns**: Cleaned text string

##### `extractChapterName(String html)`
Extracts chapter name from HTML content.

**Parameters**:
- `html` - HTML content to parse

**Returns**: Extracted chapter name

## Event System

### Listeners

#### DownloadListener
**Package**: `dark.leech.text.listeners`

Interface for download progress and completion events.

##### `onProgress(int progress)`
Called when download progress updates.

**Parameters**:
- `progress` - Progress percentage (0-100)

##### `onComplete(Chapter chapter)`
Called when a chapter download completes.

**Parameters**:
- `chapter` - The completed chapter

##### `onError(Chapter chapter, String error)`
Called when a download error occurs.

**Parameters**:
- `chapter` - The chapter that encountered an error
- `error` - Error description

#### ProgressListener
**Package**: `dark.leech.text.listeners`

Interface for general progress tracking.

##### `onProgress(int current, int total)`
Called when progress updates.

**Parameters**:
- `current` - Current progress value
- `total` - Total progress value

## Export System

### Export Classes

#### Text Class
**Package**: `dark.leech.text.action.export`

Handles plain text export functionality.

#### Ebook Class
**Package**: `dark.leech.text.action.export`

Handles EPUB ebook creation and export.

#### ToC Class
**Package**: `dark.leech.text.action.export`

Generates table of contents for exported content.

## Animation System

### Animation Class
**Package**: `dark.leech.text.ui.animation`

Provides UI animation utilities.

#### Methods

##### `fadeIn(Component component)`
Applies fade-in animation to a component.

**Parameters**:
- `component` - The component to animate

##### `fadeOut(Component component)`
Applies fade-out animation to a component.

**Parameters**:
- `component` - The component to animate

## Usage Examples

### Basic Plugin Usage
```java
// Get plugin manager instance
PluginManager manager = PluginManager.getManager();

// Find plugin for a URL
PluginEntity plugin = manager.get("https://example.com/novel/123");

if (plugin != null) {
    // Use plugin for content extraction
    String content = plugin.extractChapter("https://example.com/novel/123/chapter/1");
}
```

### Chapter Management
```java
// Create a new chapter
Chapter chapter = new Chapter("https://example.com/chapter/1", "Chapter 1");

// Set chapter properties
chapter.setPartName("Volume 1");
chapter.setCompleted(false);

// Check chapter status
if (chapter.isCompleted()) {
    System.out.println("Chapter downloaded successfully");
}
```

### Application Utilities
```java
// Get application dimensions
int screenWidth = AppUtils.width;
int screenHeight = AppUtils.height;

// Get current location
Point location = AppUtils.getLocation();

// Pause execution
AppUtils.pause(1000); // Pause for 1 second
```

## Error Handling

### Common Exceptions

- **IOException**: File and network operation failures
- **IllegalArgumentException**: Invalid parameter values
- **RuntimeException**: General runtime errors
- **InterruptedException**: Thread interruption during operations

### Best Practices

1. **Always check return values** for null or error conditions
2. **Use try-catch blocks** for I/O operations
3. **Validate input parameters** before processing
4. **Handle plugin loading errors** gracefully
5. **Provide meaningful error messages** to users

## Performance Considerations

### Memory Management
- Reuse objects when possible
- Properly close streams and connections
- Avoid memory leaks in long-running operations

### UI Responsiveness
- Use background threads for long operations
- Update UI components on EDT
- Implement progress indicators for user feedback

### Network Operations
- Implement connection pooling
- Set appropriate timeouts
- Handle network failures gracefully

---

For more detailed information about specific components, see the individual class documentation and the [Development Guide](DEVELOPMENT.md).
