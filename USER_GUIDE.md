# LeechText User Guide

A comprehensive guide to using the LeechText application for text extraction and ebook creation.

## Table of Contents

- [Getting Started](#getting-started)
- [Installation](#installation)
- [First Launch](#first-launch)
- [Basic Usage](#basic-usage)
- [Plugin Management](#plugin-management)
- [Download Management](#download-management)
- [Export Options](#export-options)
- [Settings and Configuration](#settings-and-configuration)
- [Troubleshooting](#troubleshooting)
- [Advanced Features](#advanced-features)
- [Tips and Tricks](#tips-and-tricks)

## Getting Started

### What is LeechText?

LeechText is a powerful Java-based application designed to help you extract text content from websites, novels, and other online sources. It can download content chapter by chapter and convert it into various formats including ebooks (EPUB), plain text, and structured documents.

### Key Features

- **Multi-source Support**: Extract content from various websites
- **Plugin System**: Extensible architecture for different content sources
- **Batch Downloading**: Download multiple chapters simultaneously
- **Multiple Export Formats**: EPUB, plain text, and structured formats
- **Progress Tracking**: Real-time download progress monitoring
- **Modern Interface**: Clean, intuitive user experience

### System Requirements

- **Operating System**: Windows 7+, macOS 10.12+, or Linux
- **Java Runtime**: Java 8 or higher
- **Memory**: Minimum 2GB RAM (4GB recommended)
- **Storage**: 100MB for application, additional space for downloads
- **Network**: Internet connection for content downloading

## Installation

### Download

1. Visit the [LeechText releases page](https://github.com/your-username/LeechText/releases)
2. Download the latest version for your operating system
3. Choose between JAR file or platform-specific installer

### Java Installation

If you don't have Java installed:

1. **Windows**: Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
2. **macOS**: Use Homebrew: `brew install openjdk@11`
3. **Linux**: Use package manager: `sudo apt install openjdk-11-jdk`

### Application Setup

#### Option 1: JAR File
1. Download the `LeechText.jar` file
2. Place it in a dedicated folder
3. Double-click to run (if Java is properly installed)
4. Or run from command line: `java -jar LeechText.jar`

#### Option 2: Platform Installer
1. Run the installer executable
2. Follow the installation wizard
3. Launch from Start Menu/Applications

## First Launch

### Initial Setup

When you first launch LeechText, the application will:

1. **Create Directories**: Set up necessary folders for downloads and configuration
2. **Load Default Settings**: Apply default configuration values
3. **Initialize Plugins**: Scan for available extraction plugins
4. **Display Main Interface**: Show the main application window

### First Run Experience

The application window will appear with:
- **Status Bar**: Top bar showing current time and exit button
- **App Bar**: Main toolbar with logo and action buttons
- **Download Area**: Main content area (initially empty)
- **Settings Panel**: Configuration options (accessible via menu)

## Basic Usage

### Adding Your First URL

1. **Click the Add Button** (+ icon) in the app bar
2. **Enter the URL** of the content you want to extract
3. **Select a Plugin** (if multiple are available)
4. **Click Download** to start the extraction process

### Understanding the Interface

#### Main Window Components
- **Status Bar**: Shows current time and provides window dragging
- **App Bar**: Contains logo, add button, and menu button
- **Download List**: Shows all active and completed downloads
- **Progress Indicators**: Visual feedback for download progress

#### Navigation
- **Drag Window**: Click and drag the status bar to move the window
- **Menu Access**: Click the menu button (⋮) for additional options
- **Settings**: Access configuration via the menu

### Download Workflow

1. **Add URL**: Enter the source URL
2. **Plugin Selection**: Choose appropriate extraction plugin
3. **Content Preview**: Review detected content structure
4. **Download**: Start the extraction process
5. **Monitor Progress**: Track download status and progress
6. **Export**: Convert downloaded content to desired format

## Plugin Management

### What are Plugins?

Plugins are configuration files that tell LeechText how to extract content from specific websites. Each plugin contains:
- **Extraction Rules**: How to find and extract content
- **URL Patterns**: Which websites the plugin can handle
- **Scripts**: Lua scripts for complex extraction logic

### Installing Plugins

1. **Download Plugin**: Get `.plugin` files from trusted sources
2. **Place in Directory**: Copy to `tools/plugins/` folder
3. **Restart Application**: Reload to detect new plugins
4. **Verify Installation**: Check plugin list in settings

### Plugin Configuration

Each plugin defines:
- **Name and Version**: Plugin identification
- **Source Website**: Target website information
- **Extraction Rules**: Content parsing instructions
- **Supported Features**: Available extraction methods

### Finding Compatible Plugins

- **Official Repository**: Check the LeechText plugin repository
- **Community Sources**: Look for user-created plugins
- **Custom Creation**: Create your own plugins for specific sites

## Download Management

### Adding Downloads

#### Single URL
1. Click the **Add Button** (+)
2. Enter the **URL** in the input field
3. Select the **appropriate plugin**
4. Click **Download**

#### Multiple URLs
1. Use the **Multi-URL** option from the add menu
2. Enter **multiple URLs** (one per line)
3. Choose **batch settings**
4. Start **batch download**

#### From File
1. Select **Import URLs** from file
2. Choose a **text file** with URLs
3. Configure **import options**
4. Process the **URL list**

### Download Status

#### Status Indicators
- **Pending**: Waiting to start
- **Downloading**: Currently in progress
- **Completed**: Successfully finished
- **Error**: Failed to download
- **Paused**: Temporarily stopped

#### Progress Information
- **Percentage**: Overall completion
- **Speed**: Download rate
- **ETA**: Estimated time remaining
- **Size**: Content size information

### Managing Downloads

#### Pause/Resume
- **Pause**: Click pause button to stop temporarily
- **Resume**: Click play button to continue
- **Stop**: Click stop button to cancel permanently

#### Batch Operations
- **Select Multiple**: Use Ctrl+Click or Shift+Click
- **Bulk Actions**: Pause, resume, or delete multiple downloads
- **Priority Management**: Reorder download queue

## Export Options

### Available Formats

#### Plain Text (.txt)
- **Simple Format**: Basic text with minimal formatting
- **Universal Compatibility**: Works with any text editor
- **Small File Size**: Minimal storage requirements
- **Loss of Formatting**: No styling or structure preserved

#### EPUB (.epub)
- **Ebook Format**: Standard digital book format
- **Rich Formatting**: Preserves text styling and structure
- **Chapter Navigation**: Built-in table of contents
- **Ebook Reader Support**: Works with most ebook applications

#### HTML (.html)
- **Web Format**: Preserves formatting and links
- **Browser Viewing**: Open in any web browser
- **Rich Content**: Maintains images and styling
- **Larger File Size**: More storage space required

### Export Configuration

#### Text Export Options
- **Encoding**: Choose character encoding (UTF-8 recommended)
- **Line Endings**: Windows (CRLF) or Unix (LF) style
- **Chapter Separators**: Custom separators between chapters

#### EPUB Export Options
- **Title**: Book title for metadata
- **Author**: Author information
- **Cover Image**: Custom cover image
- **Chapter Structure**: Automatic or manual chapter organization

#### General Options
- **Output Directory**: Choose where to save exported files
- **File Naming**: Customize output file names
- **Overwrite Protection**: Prevent accidental file overwrites

### Export Process

1. **Select Content**: Choose chapters or content to export
2. **Choose Format**: Select desired output format
3. **Configure Options**: Set format-specific settings
4. **Export**: Start the conversion process
5. **Monitor Progress**: Track export completion
6. **Locate Files**: Find exported files in output directory

## Settings and Configuration

### General Settings

#### Appearance
- **Theme**: Choose between dark and light themes
- **Language**: Select interface language (English/Vietnamese)
- **Font Size**: Adjust text size for better readability
- **Window Position**: Remember or reset window location

#### Download Settings
- **Default Directory**: Set default download location
- **Concurrent Downloads**: Maximum simultaneous downloads
- **Timeout Settings**: Network operation timeouts
- **Retry Attempts**: Number of retry attempts for failed downloads

#### Export Settings
- **Default Format**: Choose preferred export format
- **Output Directory**: Set default export location
- **File Naming**: Configure automatic file naming
- **Quality Settings**: Balance between quality and file size

### Advanced Configuration

#### Network Settings
- **Proxy Configuration**: Set up proxy server if needed
- **User Agent**: Customize browser identification
- **Cookie Management**: Handle website authentication
- **Rate Limiting**: Control download speed

#### Plugin Settings
- **Plugin Directory**: Customize plugin location
- **Auto-updates**: Enable automatic plugin updates
- **Plugin Priority**: Set plugin preference order
- **Debug Mode**: Enable detailed plugin logging

#### Performance Settings
- **Memory Management**: Configure memory usage limits
- **Thread Count**: Set number of worker threads
- **Cache Settings**: Configure content caching
- **Cleanup Options**: Automatic temporary file cleanup

## Troubleshooting

### Common Issues

#### Application Won't Start
**Problem**: Application fails to launch
**Solutions**:
- Verify Java installation: `java -version`
- Check Java version compatibility (Java 8+ required)
- Ensure sufficient system memory
- Try running from command line for error messages

#### Download Failures
**Problem**: Downloads fail or show errors
**Solutions**:
- Check internet connection
- Verify URL is accessible
- Ensure compatible plugin is available
- Check website accessibility and terms of service
- Try different plugin or manual extraction

#### Plugin Issues
**Problem**: Plugins not working or loading
**Solutions**:
- Verify plugin file format (.plugin extension)
- Check plugin compatibility with current version
- Ensure plugin files are in correct directory
- Restart application after plugin changes
- Check plugin error logs

#### Export Problems
**Problem**: Export fails or produces errors
**Solutions**:
- Verify output directory permissions
- Check available disk space
- Ensure content is fully downloaded
- Try different export format
- Check export error logs

### Error Messages

#### Common Error Types
- **Network Errors**: Connection timeouts, HTTP errors
- **Plugin Errors**: Extraction failures, script errors
- **File System Errors**: Permission denied, disk full
- **Memory Errors**: Out of memory, heap space issues

#### Getting Help
- **Error Logs**: Check application logs for details
- **Community Support**: Ask questions in community forums
- **Issue Reporting**: Report bugs with detailed information
- **Documentation**: Consult this guide and API documentation

## Advanced Features

### Custom Extraction

#### Manual Content Extraction
- **HTML Parsing**: Use custom selectors for content
- **Regex Patterns**: Apply regular expressions for text extraction
- **JavaScript Execution**: Handle dynamic content loading
- **Custom Scripts**: Write Lua scripts for complex extraction

#### Content Processing
- **Text Cleaning**: Remove unwanted formatting and characters
- **Chapter Detection**: Automatic chapter boundary detection
- **Content Validation**: Verify extracted content quality
- **Duplicate Detection**: Identify and handle duplicate content

### Batch Operations

#### Multiple Source Management
- **URL Lists**: Process multiple URLs from text files
- **Website Crawling**: Extract content from entire websites
- **Incremental Updates**: Download only new or changed content
- **Scheduled Downloads**: Set up automatic download schedules

#### Content Organization
- **Automatic Categorization**: Organize content by source or type
- **Metadata Extraction**: Extract author, date, and other information
- **Content Tagging**: Apply custom tags for organization
- **Search and Filter**: Find specific content quickly

### Integration Features

#### External Tools
- **Calibre Integration**: Direct export to Calibre library
- **Cloud Storage**: Upload to cloud storage services
- **Email Export**: Send content via email
- **API Access**: Programmatic access to application features

#### Automation
- **Command Line Interface**: Script-based operation
- **Scheduled Tasks**: Automatic content updates
- **Web Interface**: Remote access and management
- **Mobile Companion**: Mobile app for monitoring

## Tips and Tricks

### Performance Optimization

#### Download Speed
- **Adjust Thread Count**: Find optimal concurrent download limit
- **Network Optimization**: Use wired connection when possible
- **Plugin Selection**: Choose efficient plugins for your sources
- **Content Filtering**: Download only necessary content

#### Memory Usage
- **Limit Concurrent Downloads**: Reduce memory pressure
- **Regular Cleanup**: Clear temporary files and cache
- **Content Preview**: Preview before downloading large content
- **Batch Processing**: Process content in smaller batches

### Content Quality

#### Better Extraction
- **Plugin Updates**: Keep plugins current for best results
- **Source Selection**: Choose high-quality content sources
- **Content Validation**: Verify extracted content manually
- **Format Preservation**: Use appropriate export formats

#### Organization
- **Consistent Naming**: Use consistent file naming conventions
- **Directory Structure**: Organize content logically
- **Metadata Management**: Maintain accurate content information
- **Backup Strategy**: Regular backups of downloaded content

### Workflow Efficiency

#### Time Saving
- **Batch Operations**: Process multiple items simultaneously
- **Template Usage**: Save and reuse common configurations
- **Keyboard Shortcuts**: Learn and use keyboard shortcuts
- **Automation**: Set up automated workflows

#### Quality Assurance
- **Preview Downloads**: Check content before full download
- **Incremental Updates**: Download only new content
- **Error Handling**: Set up automatic retry for failures
- **Content Verification**: Validate downloaded content

---

For technical details and API information, see the [API Documentation](API_DOCUMENTATION.md). For development information, see the [Development Guide](DEVELOPMENT.md).

## Support and Community

### Getting Help
- **Documentation**: This guide and related documentation
- **Community Forums**: User discussions and support
- **Issue Tracker**: Report bugs and request features
- **Wiki**: Community-maintained knowledge base

### Contributing
- **Plugin Development**: Create plugins for new sources
- **Documentation**: Improve and expand documentation
- **Bug Reports**: Help identify and fix issues
- **Feature Requests**: Suggest new functionality

### Resources
- **Source Code**: [GitHub Repository](https://github.com/your-username/LeechText)
- **Releases**: [Latest Downloads](https://github.com/your-username/LeechText/releases)
- **Discussions**: [Community Forum](https://github.com/your-username/LeechText/discussions)
- **Issues**: [Bug Reports](https://github.com/your-username/LeechText/issues)
