# Code Standards - LeechText

## Java Code Style

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `PluginManager`, `DownloadUI` |
| Methods | camelCase | `getPlugin()`, `startDownload()` |
| Variables | camelCase | `chapList`, `downloadListener` |
| Constants | UPPER_SNAKE_CASE | `MAX_CONN`, `DOWNLOADING` |
| Packages | lowercase | `dark.leech.text.action` |

### Code Formatting (Spotless - Google Java Format)

```bash
# Apply code formatting
./gradlew spotlessApply

# Check code format
./gradlew spotlessCheck
```

**Formatting Rules:**
- Google Java Format 1.18.1 (AOSP variant)
- No wildcard imports
- Import order: `java`, `javax`, `org`, `com`, `dark.leech`
- Trim trailing whitespace
- End files with newline

### Checkstyle Rules

**Configuration**: `config/checkstyle/checkstyle.xml`

```bash
# Run checkstyle
./gradlew checkstyleMain
```

**Key Rules:**
- No wildcard imports
- Proper Javadoc for public APIs
- No line length > 100 characters (recommended)
- Proper modifier order
- Consistent code blocks

### PMD Rules

**Configuration**: `config/pmd/pmd-rules.xml`

```bash
# Run PMD
./gradlew pmdMain
```

**Key Checks:**
- Empty code blocks
- Unused imports/variables
- Duplicate code
- Optimizable code patterns

## Code Organization

### Package Structure

```
dark.leech.text
├── action/         # Business logic actions
├── animation/      # UI animations
├── enities/        # Data entities (note: typo, should be entities)
├── get/            # Content retrieval
├── image/          # Image processing
├── listeners/      # Event listeners
├── lua/            # Lua integration
├── models/         # Data models
├── plugin/         # Plugin system (includes js/, security/, validation/, sandbox/, vbook/ subpackages)
├── ui/             # User interface
└── util/           # Utilities (includes ImageConverter, HtmlSanitizer)
```

### Class Organization

```java
package dark.leech.text.package;

// 1. Standard imports (no wildcards)
import java.util.List;
import javax.swing.*;

// 2. Third-party imports
import org.jsoup.nodes.Document;

// 3. Project imports
import dark.leech.text.models.Chapter;

/**
 * Class description.
 *
 * @author Author Name
 * @since 2019.03.30
 */
public class ClassName {

    // 1. Static constants
    public static final int CONSTANT_NAME = 100;

    // 2. Static fields
    private static Type staticField;

    // 3. Instance fields
    private final Type finalField;
    private Type instanceField;

    // 4. Constructors
    public ClassName() {
        this.finalField = initialize();
    }

    // 5. Public methods
    public void publicMethod() {
        // Implementation
    }

    // 6. Protected methods
    protected void protectedMethod() {
        // Implementation
    }

    // 7. Package-private methods
    void packageMethod() {
        // Implementation
    }

    // 8. Private methods
    private void privateMethod() {
        // Implementation
    }
}
```

## Lombok Usage Guidelines

### Recommended Annotations

```java
@Getter@Setter  // For simple POJOs
@Data          // For comprehensive data classes
@AllArgsConstructor  // When all-args constructor needed
@NoArgsConstructor   // When no-args constructor needed
@Builder       // For builder pattern
@Slf4j         // For logging
```

### Examples

**Models with Lombok:**
```java
@Getter
@Setter
public class Chapter implements Cloneable {
    private String url;
    private String partName;
    private String chapName;
    private boolean completed;
    // ...
}
```

**Builder Pattern:**
```java
@Builder
public class DownloadConfig {
    private int maxConnections;
    private String outputFormat;
    private boolean includeImages;
}
```

## Comment Standards

### Javadoc for Public APIs

```java
/**
 * Manages plugin loading and lifecycle.
 *
 * <p>Plugins are loaded from the tools/plugins directory
 * and are matched against URLs using regex patterns.
 *
 * @author LeechText Team
 * @since 2019.03.30
 */
public class PluginManager {
    /**
     * Gets a plugin matching the given URL.
     *
     * @param url the URL to match against plugin regex
     * @return matching PluginEntity, or null if no match
     */
    public PluginEntity get(String url) {
        // Implementation
    }
}
```

### Inline Comments

```java
// Start the download process
status = DOWNLOADING;
next = next + MAX_CONN - 1;

// Check if we've downloaded all chapters
if (index >= size) {
    update();  // Update progress UI
}
```

## Error Handling

### Exception Handling Pattern

```java
public void downloadContent(String url) {
    try {
        // Attempt download
        PageGetter getter = new PageGetter(url);
        String content = getter.get();
    } catch (IOException e) {
        Log.add(e);  // Log the error
        status = ERROR;  // Update status
        downloadListener.updateDownload(downloaded, status);
    } catch (Exception e) {
        Log.add(e);  // Catch unexpected errors
    }
}
```

### Logging

```java
// Use the Log class for application logging
Log.add(exception);
Log.add("Error message: " + details);
```

## Thread Safety Guidelines

### Runnable Pattern for UI Operations

```java
// Run UI updates on Event Dispatch Thread
SwingUtilities.invokeLater(() -> {
    mainFrame.setVisible(true);
});

// Run long tasks on separate thread
new Thread(() -> {
    // Background work
    doHeavyWork();

    // Update UI on EDT
    SwingUtilities.invokeLater(() -> {
        updateUI();
    });
}).start();
```

### Synchronization

```java
public class PluginManager {
    private static volatile PluginManager manager;
    private static final Object lock = new Object();

    public static PluginManager getManager() {
        if (manager == null) {
            synchronized (lock) {
                if (manager == null) {
                    manager = new PluginManager();
                }
            }
        }
        return manager;
    }
}
```

## Plugin Development Standards

### Plugin File Structure

#### Lua Plugin Structure

```json
{
  "name": "Lua Plugin Name",
  "version": "1.0",
  "regex": "example\\.com",
  "chap": "chapter extraction script",
  "toc": "table of contents script",
  "detail": "detail page script"
}
```

#### JavaScript Plugin Structure (NEW)

```json
{
  "name": "JavaScript Plugin",
  "version": "1.0",
  "regex": "example\\.com",
  "chap": "chapter extraction script",
  "toc": "table of contents script",
  "detail": "detail page script",
  "javascript": true
}
```

**Security Requirements for JavaScript Plugins:**
- All network requests must use HTTPS (except localhost)
- Maximum download size: 50MB per plugin
- Allowed content types: ZIP, ZIP-compressed, octet-stream
- Script execution with resource limits
- Validation of all user inputs and URLs

### JavaScript Plugin Development Standards

#### Basic Plugin Structure

```javascript
// Basic JavaScript plugin structure
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

#### API Usage Guidelines

1. **Always use context constructor**:
   ```javascript
   const html = new Html(context);  // Correct
   const html = new Html();        // Incorrect - throws exception
   ```

2. **Handle null values properly**:
   ```javascript
   const data = http.get(url).json();
   if (data === null) {
       Log.add('Request failed');
       return null;
   }
   ```

3. **Use method chaining**:
   ```javascript
   const title = html.parse(htmlString)
       .select('h1.title')
       .text()
       .trim();
   ```

4. **Implement proper error handling**:
   ```javascript
   try {
       const data = http.get(url).json();
       // Process data
   } catch (e) {
       Log.add('API error: ' + e.message);
       return null;
   }
   ```

#### Plugin File Structure

```javascript
// Complete plugin example
function extractData(html, context) {
    const htmlApi = new Html(context);
    const httpApi = new Http(context);

    // Extract metadata
    const metadata = {
        title: htmlApi.parse(html).select('h1.novel-title').text(),
        author: htmlApi.parse(html).select('.author').text()
    };

    // Extract chapters
    const chapters = htmlApi.parse(html).select('.chapter-list li').map(function(chapter) {
        return {
            title: chapter.select('a').text(),
            url: chapter.select('a').attr('href')
        };
    });

    return {
        metadata: metadata,
        chapters: chapters
    };
}
```

#### Testing Guidelines

```javascript
// JavaScript plugin unit test
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

#### Performance Considerations

1. **Reuse contexts when possible**:
   ```javascript
   // Reuse context for multiple operations
   const context = getContext();
   const html = new Html(context);
   const http = new Http(context);
   ```

2. **Minimize object creation**:
   ```javascript
   // Avoid unnecessary temporary objects
   const title = html.parse(html).select('h1').text();
   ```

3. **Use efficient selectors**:
   ```javascript
   // Use specific selectors
   const content = html.parse(html).select('.chapter-content').html();
   ```

#### Security Guidelines

1. **Validate input data**:
   ```javascript
   if (!html || html.trim().isEmpty()) {
       return null;
   }
   ```

2. **Handle external requests safely**:
   ```javascript
   const url = httpApi.get(url).json();
   if (url === null) {
       Log.add('Invalid URL');
       return null;
   }
   ```

3. **Implement proper error boundaries**:
   ```javascript
   try {
       // Plugin execution
   } catch (e) {
       Log.add('Plugin error: ' + e.message);
       return null;
   }
   ```

#### Migration from vBook

When migrating vBook plugins to JavaScript:

```javascript
// vBook plugin (original)
function extractData(html) {
    const htmlApi = new Html();
    const doc = htmlApi.parse(html);

    return {
        title: doc.select('h1.title').text(),
        chapters: doc.select('.chapter-list li').map(function(chapter) {
            return {
                title: chapter.select('a').text(),
                url: chapter.select('a').attr('href')
            };
        })
    };
}

// Migrated JavaScript plugin
function extractData(html, context) {
    const htmlApi = new Html(context);
    const doc = htmlApi.parse(html);

    return {
        title: doc.select('h1.title').text(),
        chapters: doc.select('.chapter-list li').map(function(chapter) {
            return {
                title: chapter.select('a').text(),
                url: chapter.select('a').attr('href')
            };
        })
    };
}
```

#### Plugin Configuration

JavaScript plugins use the same `.plugin` configuration format as Lua plugins, with an additional `javascript` flag:

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

#### Best Practices

1. **Keep plugins focused**: Each plugin should handle a specific website or type of content
2. **Use descriptive variable names**: Clear and meaningful variable names
3. **Add comments for complex logic**: Explain non-obvious code sections
4. **Handle edge cases**: Consider various website structures and error conditions
5. **Test thoroughly**: Validate with different inputs and scenarios
6. **Follow existing patterns**: Consistent with other plugins in the codebase
7. **Document usage**: Include comments explaining plugin functionality
8. **Handle encoding properly**: Use proper character encoding for text processing

#### Common Patterns

##### Method Chaining Pattern

```javascript
// Chain methods for cleaner code
const title = html.parse(htmlString)
    .select('h1.title')
    .text()
    .trim();
```

##### Array Processing Pattern

```javascript
// Process collections with map
const chapters = html.select('.chapter-list li').map(function(chapter) {
    return {
        title: chapter.select('a').text(),
        url: chapter.select('a').attr('href')
    };
});
```

##### Error Handling Pattern

```javascript
// Comprehensive error handling
function safeExtract(html, context) {
    try {
        const htmlApi = new Html(context);
        const doc = htmlApi.parse(html);

        if (doc.select('.content').isEmpty()) {
            Log.add('No content found');
            return null;
        }

        return {
            title: doc.select('h1.title').text(),
            content: doc.select('.content').html()
        };

    } catch (e) {
        Log.add('Extraction failed: ' + e.message);
        return null;
    }
}
```

##### API Integration Pattern

```javascript
// Integrate with HTTP API
function fetchAdditionalData(url, context) {
    const httpApi = new Http(context);
    const data = httpApi.get(url).json();

    if (data === null) {
        Log.add('Failed to fetch additional data');
        return null;
    }

    return data;
}
```

## Image Processing Standards (NEW)

### WebP Conversion Guidelines

**Automatic Conversion Requirements:**
- Cover images named `cover.jpg` are automatically converted from WebP to JPEG
- Use `ImageConverter.downloadAndConvertToJpeg()` for cover images
- Use `ImageConverter.isWebP()` to detect WebP format by magic bytes
- Conversion tools (ffmpeg/ImageMagick) are optional but recommended

**Implementation Pattern:**
```java
// For cover images - use WebP-aware conversion
if (pathImage.endsWith("cover.jpg")) {
    FileUtils.url2fileConvertWebP(urlImage, pathImage);
} else {
    FileUtils.url2file(urlImage, pathImage);
}
```

**External Tool Support:**
- **ffmpeg** (primary): `ffmpeg -i input.webp -qscale:v 2 output.jpg`
- **ImageMagick** (fallback): `convert input.webp -quality 95 output.jpg`
- **Fallback behavior**: Save as-is with warning logged

### HTML Sanitation Guidelines (NEW)

**Purpose**: Ensure EPUB XML validation by fixing common HTML issues

**Use Cases:**
- EPUB export preprocessing
- Content cleanup before XML generation

**Implementation:**
```java
// Sanitize HTML for EPUB compatibility
String cleanHtml = HtmlSanitizer sanitize(dirtyHtml);
boolean isValid = HtmlSanitizer isValid(cleanHtml);
```

**Fixed Issues:**
- Unclosed inline tags (<strong>, <em>, <b>, <i>, <span>, <a>)
- Orphaned closing tags without opening tags
- Tag balance validation for common elements

## Testing Guidelines

### Unit Test Structure

```java
public class DownloadTest {

    @Test
    public void testDownloadStart() {
        // Arrange
        Properties props = new Properties();
        Download download = new Download(props);

        // Act
        download.start();

        // Assert
        assertEquals(Download.DOWNLOADING, download.getStatus());
    }
}
```

## Build and Quality Gate

### Pre-commit Checklist

```bash
# Format code
./gradlew spotlessApply

# Compile
./gradlew compileJava

# Run quality checks
./gradlew checkstyleMain pmdMain test
```

### Quality Gate Task

```bash
# Run all quality checks
./gradlew qualityGate
```

This runs:
- Spotless format check
- Checkstyle validation
- PMD analysis
- Unit tests

## Code Review Checklist

- [ ] Code follows naming conventions
- [ ] No wildcard imports
- [ ] Proper error handling
- [ ] Thread-safe where applicable
- [ ] Javadoc on public methods
- [ ] No TODO/FIXME in production code
- [ ] Consistent with existing patterns
- [ ] Tests added for new functionality
