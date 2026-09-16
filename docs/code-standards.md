# Code Standards - LeechText

Kotlin + Compose Multiplatform. Bản Java (Spotless/Checkstyle/PMD/Lombok) đã xóa cùng legacy — xem tag `java-legacy`.

## Kotlin Code Style

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes / Objects | PascalCase | `PluginManager`, `BookDownload`, `LibraryScreen` |
| Functions | camelCase | `getPlugin()`, `enqueue()` |
| Variables | camelCase | `chapList`, `downloadQueue` |
| Constants | UPPER_SNAKE_CASE | `MAX_PARALLELISM`, `NOTIFICATION_ID` |
| Packages | lowercase | `dev.haipham22.leechtext.action` |

### Formatting

Không có formatter plugin (Spotless/Checkstyle/PMD đã bỏ). Theo quy ước:

- No wildcard imports
- 4 space indent, không tab
- Line length ~120
- End files with newline

Quality gate thực tế: **SonarQube** (0 issues) + tests — xem README phần "Kiểm thử & chất lượng".

## Code Organization

### Module / Package Structure

```
engine/src/jvmMain/kotlin/dev/haipham22/leechtext/
├── action/          # Business actions (History) + export/ (Ebook, Text, ToC)
├── entities/        # BookEntity, ChapterEntity, PluginEntity, RepositoryEntity
├── get/             # Download/fetch (BookDownload, BookFetch, ChapterImages...)
├── models/          # Data classes (Chapter, Properties, Settings, Pager, Post, Repository, Trash)
├── plugin/
│   ├── api/         # Plugin API models (PaginatedResult, PluginMetadata...)
│   ├── js/
│   │   ├── api/     # JS APIs (Html, Http, Json, Regexp, Browser, Engine, LocalStorage...)
│   │   ├── loader/  # Loaders (Detail, List, Text, Page, Browse) + Response
│   │   └── sandbox/ # JsSandbox
│   ├── security/    # Network/Regex/Zip validators
│   ├── util/        # PluginPersistence
│   └── vbook/       # VBookPluginService, converter, repository client
└── util/            # FileUtils, ZipUtils, HtmlSanitizer, EnginePaths...

app-shared/src/
├── commonMain/.../ui/          # components (AppRail, BookCoverTile, Theme)
├── jvmMain/.../ui/             # screens + state (SliceApp, DownloadQueueState, Strings)
└── androidMain/                # DownloadService, Android glue

desktop-app/  android-app/  baselineprofile/
```

**Nguyên tắc**: business logic nằm ở `:engine` (không phụ thuộc Compose); UI dùng chung ở `:app-shared`; code platform-specific đúng source set của nó (jvmMain/androidMain).

### File / Class Layout

```kotlin
package dev.haipham22.leechtext.plugin

// 1. Kotlin stdlib / coroutine imports
// 2. Third-party (org.jsoup, com.google.gson...)
// 3. Project (dev.haipham22.leechtext...)

/** Mô tả class (KDoc). */
class ClassName {
    // companion object / constants trước
    // properties
    // init / constructors
    // public → private
}
```

- 1 class chính mỗi file, tên file = tên class
- Data class cho pure data (`models/`), không viết getter/setter tay
- Singleton bằng `object` (vd `PluginManager`, `EngineLog`) — không tự viết double-checked locking

## Comments & KDoc

```kotlin
/**
 * Quản lý load plugin: scan tools/plugins, match URL theo regex.
 */
object PluginManager {
    /** Trả plugin khớp [url], null nếu không match. */
    fun get(url: String): PluginEntity? = ...
}
```

Comment tiếng Việt OK (khớp code hiện tại). Giải thích "tại sao", không giải thích "cái gì".

## Error Handling & Logging

```kotlin
suspend fun downloadChapters(...) {
    runCatching { fetch(url) }
        .onSuccess { ... }
        .onFailure { e ->
            EngineLog.add(e)              // log exception
            EngineLog.warn("fetch failed: $url")
        }
}
```

- `EngineLog` (`add(msg)`, `add(e)`, `debug`, `warn`) — không dùng println
- Không nuốt exception im lặng ở trust boundary (network, file IO, parse)
- UI error → state (StateFlow), không throw lên Compose

## Concurrency

```kotlin
// Engine work qua dispatcher chung (parallelism khớp Rhino context pool)
withContext(EngineDispatchers.engine) { ... }

// Giới hạn song song bằng Semaphore
val sem = Semaphore(settings.maxConn)
```

- Không dùng raw `Thread`. Coroutines + `EngineDispatchers`
- UI update từ background: qua StateFlow/state holder, Compose tự recompose
- Plugin script luôn chạy trong JsSandbox, không bao giờ chạy raw

## Plugin Development Standards

Plugin là JavaScript (Rhino). Không còn Lua.

### Plugin Manifest (`.plugin` JSON)

```json
{
  "uuid": "unique-id",
  "name": "Example Plugin",
  "version": 1.0,
  "url": "https://example.com",
  "regex": "example\\.com",
  "detail": "detail extraction script (JS)",
  "toc": "table of contents script (JS)",
  "chap": "chapter extraction script (JS)",
  "gen": "pagination script (JS)",
  "search": "search script (JS)"
}
```

Các key khác: `page, home, genre, tab, extra_scripts, config, config_spec, priority, tag`. **Không có cờ `javascript`** — mọi plugin đều là JS.

**Security requirements:**
- Network request phải HTTPS (except localhost)
- Maximum download size: 50MB
- Allowed content types: ZIP, ZIP-compressed, octet-stream
- Script chạy trong JsSandbox với resource limits
- Regex của plugin được kiểm ReDoS trước khi dùng

### Basic Plugin Structure

```javascript
function extractData(html) {
    try {
        const doc = Html.parse(html);
        const title = doc.select('h1.title').text();
        const chapters = doc.select('.chapter-list li').map(function(chapter) {
            return {
                title: chapter.select('a').text(),
                url: chapter.select('a').attr('href')
            };
        });
        return Response.success({ title: title, chapters: chapters });
    } catch (e) {
        console.log('Extraction failed: ' + e.message);
        return Response.error('extract failed');
    }
}
```

### API Usage Guidelines

1. **Constructors**: `new Html()` (no-arg) và `new Html(context, scope)` đều chạy (constructor nhận context giữ để compat vBook).
2. **Handle null values properly**:
   ```javascript
   const data = http.get(url).json();
   if (data === null) {
       console.log('Request failed');
       return Response.error('request failed');
   }
   ```
3. **Use method chaining**:
   ```javascript
   const title = Html.parse(htmlString).select('h1.title').text().trim();
   ```
4. **Implement proper error handling** — bọc `try/catch`, trả `Response.error(...)`, không trả throw.

### Response Format

```javascript
Response.success(data)        // {code: 0, data}
Response.success(data, data2) // {code: 0, data, data2} — data2 = pagination token
Response.error(data)          // {code: 1, data2: data} — message nằm ở data2
```

Kotlin side: `Response.isSuccess(result)`, `Response.getData(result)`, `Response.getData2(result)`, `Response.getErrorMessage(result)`.

### Pagination Guidelines

Phân trang dùng script `gen` (chạy qua **BrowseLoader** — không còn "GenLoader"):

```javascript
// Script gen: trả items + next page token
function execute(url, page) {
    const items = getItems(url, page);
    const nextPage = getNextPageIdentifier(url, page); // null khi hết
    return Response.success(items, nextPage);
}
```

**Metadata rules:**
- Page identifier nhất quán (URL path, số trang, hoặc token)
- Trang cuối: trả `null`/`undefined` cho next page
- Items luôn là array/object, không null

**Kotlin consume** — `PaginatedResult<T>` (`plugin/api/PaginatedResult.kt`):

```kotlin
val result: PaginatedResult<BookEntity> = browseLoader.load(url)
val items = result.items
if (result.hasMore) {
    val next = browseLoader.load(url + result.getNextPage()) // page+1, -1 nếu hết
}
```

`PaginatedResult` fields: `items, totalCount, page, pageSize, totalPages, hasMore`; factory `PaginatedResult.of(...)`, `PaginatedResult.empty()`. Không có builder.

### Testing Guidelines

Plugin engine test bằng kotlin.test trong `engine/src/jvmTest/` (vd `plugin/vbook/` E2E với plugin pattern thật):

```kotlin
class VBookSandboxCompatibilityTest {
    @Test
    fun `plugin vBook chạy được trong sandbox`() {
        val result = JsSandbox.execute(plugin, url)
        assertTrue(Response.isSuccess(result))
    }
}
```

### Best Practices

1. **Keep plugins focused**: mỗi plugin một site/một loại nội dung
2. **Descriptive variable names**
3. **Comments cho logic phức tạp**
4. **Handle edge cases**: cấu trúc site lạ, encoding, error
5. **Test với input thật**
6. **Follow existing patterns** — tham khảo plugin trong repository vBook
7. **Plan for pagination** khi site có multi-page

### Common Patterns

```javascript
// Method chaining
const title = Html.parse(html).select('h1.title').text().trim();

// Array processing
const chapters = doc.select('.chapter-list li').map(function(ch) {
    return { title: ch.select('a').text(), url: ch.select('a').attr('href') };
});

// Safe extraction
function safeExtract(html) {
    try {
        const doc = Html.parse(html);
        if (doc.select('.content').isEmpty()) return Response.error('no content');
        return Response.success({ title: doc.select('h1').text() });
    } catch (e) {
        return Response.error('extract failed: ' + e.message);
    }
}
```

### Migration from vBook

Plugin vBook chạy được gần như nguyên vẹn: API mapping trực tiếp, method chaining giữ nguyên, `Response` cùng format. Khác biệt chính: script chạy trong JsSandbox (HTTPS-only, whitelist), và Lua không còn được hỗ trợ.

## HTML Sanitation

`util/HtmlSanitizer.kt` — top-level functions, sửa HTML lỗi (tag không đóng, orphan closing tag) trước khi xuất EPUB:

```kotlin
val clean = sanitizeHtml(dirtyHtml)
val ok = isValidHtml(clean)
```

## Testing Guidelines

kotlin.test, đặt ở `engine/src/jvmTest/` và `app-shared/src/jvmTest/`:

```kotlin
class BookDownloadTest {
    @Test
    fun `chapter đã có file thì skip`() {
        // arrange + act + assert
        assertEquals(expected, actual)
    }
}
```

- Test đặt cùng package với class được test
- Feature mới MUST kèm test (xem quy ước trong CLAUDE.md)
- Coverage: `./gradlew :engine:jacocoJvmReport`

## Build & Quality

```bash
./gradlew :engine:jvmTest :app-shared:jvmTest    # unit tests
./gradlew :engine:jacocoJvmReport               # coverage
./gradlew :desktop-app:run                      # chạy app
./gradlew build                                 # build tất cả
```

Không có spotless/checkstyle/pmd/qualityGate task — quality gate là SonarQube (0 issues) + tests.

## Code Review Checklist

- [ ] Code theo naming conventions
- [ ] Đúng source set/module (business → engine, UI → app-shared, platform → đúng source set)
- [ ] Proper error handling (không nuốt exception ở trust boundary)
- [ ] Coroutine dùng EngineDispatchers, không raw Thread
- [ ] Plugin execution qua JsSandbox
- [ ] KDoc cho public API phức tạp
- [ ] No TODO/FIXME trong production code
- [ ] Consistent với patterns hiện có
- [ ] Tests cho functionality mới
