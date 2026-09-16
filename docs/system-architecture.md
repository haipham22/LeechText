# System Architecture — LeechText

Kiến trúc hiện tại: **Kotlin + Compose Multiplatform** (desktop + Android). Bản Java Swing cũ ở tag `java-legacy`.

## Tổng quan

5 module Gradle (`settings.gradle`), tách rõ business logic khỏi UI:

```
┌──────────────────────────────────────────────────────────────┐
│  desktop-app            │  android-app                       │
│  (Compose Desktop,      │  (MainActivity, foreground service)│
│   Main.kt 1280x800)     │                                    │
├──────────────────────────────────────────────────────────────┤
│  app-shared — UI Compose dùng chung                          │
│  commonMain (screens, components, theme)                     │
│  jvmMain (ViewModels/state desktop) │ androidMain (glue)     │
├──────────────────────────────────────────────────────────────┤
│  engine — business logic thuần, không Compose                │
│  plugin system (Rhino sandbox) │ download │ export │ settings│
├──────────────────────────────────────────────────────────────┤
│  baselineprofile (Android startup profile)                   │
└──────────────────────────────────────────────────────────────┘
```

Package gốc: `dev.haipham22.leechtext`. Kotlin 2.1.21, Compose Multiplatform 1.8.1, Gradle 8.14.5, JDK 17.

Data dir (`util/EnginePaths.kt`): desktop `~/.leechtext/`, Android files dir của app — chứa `output/` (sách), `tools/plugins/` (plugin), `tools/setting.json`, `tools/repository.json`.

## 1. UI Layer (`app-shared`)

**Screens** (`app-shared/src/jvmMain/.../ui/`): `SliceApp` (khung chính + rail/bottom bar), `LibraryScreen`, `AddBookScreen`, `BookDetailScreen`, `SourceBrowseScreen`, `HistoryScreen`, `SettingsScreen` (+ `SettingsRows/Sections/Extras`), `PluginScreen`, `ChapterReader`, `ExportBookDialog`.

**Components dùng chung** (`commonMain/.../ui/components/`): `AppRail`, `AppBottomBar`, `BookCoverTile`, `DownloadQueuePanel`, `Theme`.

**State**: `BookPipeline` (orchestrate add/fetch/download), `DownloadQueueState` (queue đa sách: enqueue/cancel/retry/clearFinished/pump, phát StateFlow cho UI), `Strings` (i18n vi/en), `CoverImage`.

**Android cụ thể** (`androidMain`): `DownloadService` — foreground service + notification tiến trình download (sống qua doze).

## 2. Business Layer (`engine`)

### Download — `get/`

- **BookDownload**: hàm suspend, tải song song chương bằng coroutines + `Semaphore` theo `AppSettings.maxConn` (map từ `Settings.num_conn` legacy). Resume: chương đã có file → skip, chỉ tải thiếu; trạng thái resume ghi ở `raw/<id>.txt`. Kết quả: `DownloadSummary(total, resumed, ok, empty, error)`.
- **BookFetch**: fetch metadata + TOC; `mergeFetchedChapters` khớp URL giữ id cũ (truyện đang ra → chỉ tải chương mới).
- **ChapterImages** (tải ảnh chương ảnh), **ChapterName**, **ResolvePluginSetting**.

### Export — `action/export/`

`Ebook.kt` (EPUB: cover, TOC, ảnh, nén tùy chọn, chia quyển — qua Calibre/KindleGen nếu có hoặc engine tự chế), `Text.kt` (TXT), `ToC.kt` (mục lục NCX/OPF), `ProgressListener`.

### Lõi khác

`EngineConfig` (settings cached), `EngineLog` (log), `EngineDispatchers` (coroutine dispatchers, `MAX_PARALLELISM = 5`, sized theo Rhino context pool), `action/History.kt` (lịch sử download), `util/SettingsRepository`.

## 3. Plugin System

### PluginManager (`plugin/PluginManager.kt`, Kotlin `object`)

- Discovery: scan `<dataDir>/tools/plugins/` cho file `*.plugin` (JSON)
- Match: `get(url)` — so khớp `regex` của plugin với URL
- Auto-install: `VBookPluginService.findAndInstallByUrl(url)` (gọi từ `BookPipeline`) — dò plugin khớp trong repository vBook và cài

### PluginEntity (`entities/PluginEntity.kt`)

Manifest JSON (gson `@SerializedName`): `uuid, name, version, url, regex, chap, toc, page, gen, search, home, genre, tab, detail, extra_scripts, config, config_spec, priority, tag, language, icon, source, author, describe, group, data` — các field script (`chap` → `chapGetter`, `toc` → `tocGetter`, `detail` → `detailGetter`, `gen` → `genGetter`...) là **JavaScript**, chạy trên Rhino. Không còn Lua.

Entities khác: `BookEntity`, `ChapterEntity`, `RepositoryEntity`.

### vBook repository (`plugin/vbook/`)

`VBookPluginService` (kho plugin theo ngôn ngữ, có cache), `VBookRepositoryClient`, `VBookToLeechTextConverter` (convert plugin vBook → PluginEntity), `PluginZipExtractor`. Hỗ trợ thêm/xóa repo qua `plugin/RepositoryManager.kt` (persist `tools/repository.json`).

## 4. JS Engine & Sandbox

### JsScriptEngine (`plugin/js/api/JsScriptEngine.kt`, Rhino 1.7.15)

- Rhino context pool: `RhinoContextPool` / `RhinoPooledContext` (tái sử dụng context, giới hạn song song)
- Expose method trực tiếp (không ProxyObject), method chaining kiểu vBook
- Value conversion hai chiều Java ↔ JS

### Sandbox (`plugin/js/sandbox/JsSandbox.kt`)

Mọi script plugin **bắt buộc** chạy qua JsSandbox: whitelist class/API prefix, chặn truy cập filesystem tùy ý.

### Validators (`plugin/security/`)

- **NetworkSecurityValidator**: HTTPS + localhost + `file://`, giới hạn 50MB, content-type allowlist
- **RegexSecurityValidator**: chống ReDoS (kiểm tra regex của plugin)
- **ZipSecurityValidator**: scan archive (zip-slip, nội dung độc)
- **SecurityConstants**: cấu hình whitelist

### JS API cho plugin (`plugin/js/api/`)

`Html` (parse/select, `JSDocument`/`JSElement`/`JSElements`, chaining kiểu jQuery), `Http` (GET/POST/... trên okhttp, response `html()/json()/string()/bytes()`), `Json`, `JSList`, `Regexp`, `Text`, `Response`, `Browser` + `Engine` (newBrowser — Playwright headless Chromium, desktop-only), `LocalStorage`, `UserAgent`, `Console` (`console.log()`, loader).

```javascript
// pattern plugin vBook
var doc = Html.parse(htmlString);
var title = doc.select('h1.title').text();
return Response.success(data);        // {code: 0, data}
return Response.success(data, data2); // + data2 (pagination token)
```

Response cũ dạng `{data, data2}` không có `code` vẫn được hỗ trợ (backward compat). Utility Kotlin: `Response.isSuccess/getData/getData2/getErrorMessage` (`plugin/js/loader/Response.kt`).

### Loaders (`plugin/js/loader/`)

`AbstractLoader<T>` với `load(url)`; `LoaderType` = LIST / DETAIL / TEXT / PAGE:

- **DetailLoader** → `BookEntity` (metadata)
- **ListLoader** → danh sách chương (TOC)
- **TextLoader** → nội dung chương (String)
- **PageLoader** → dò tất cả page URL (phân trang TOC)
- **BrowseLoader** → catalog nguồn (home/genre/search/tab, script `home`/`genre`/`gen`/`search`)

### Phân trang (`plugin/api/PaginatedResult.kt`)

Plugin trả `Response.success(items, nextPage)` → Kotlin nhận `PaginatedResult<T>`: `items, totalCount, page, pageSize, totalPages, hasMore`, factory `PaginatedResult.of(...)` / `empty()`, `getNextPage(): Int` (page+1 hoặc -1), `getPreviousPage()`. Không có builder.

## 5. Data Layer (`models/` + persistence)

Data class Kotlin (gson):

- **Chapter**: `url, partName, chapName, id, completed, error, empty, imageChapter, purchase`
- **Properties**: `chapList, pageList, url, charset, forum` + `name, author, cover, savePath, size, introduce, ongoing, urlList, addGt`
- **Pager**, **Post**, **Settings** (preferences legacy, `num_conn`, trash rules), **Repository**, **Trash**; runtime settings qua `util/SettingsRepository` → `AppSettings` (gồm `maxConn`)

Persist: `properties.json` mỗi sách trong `output/`, `setting.json` + `repository.json` trong `tools/` — format tương thích bản Java cũ (FormatCompatTest).

## 6. Utility Layer (`util/`)

`FileUtils`, `ZipUtils` (ZIP/EPUB), `SyntaxUtils`, `RegexUtils`, `HtmlSanitizer` (`sanitizeHtml()`/`isValidHtml()` — top-level functions), `CookiesUtils` (cookie theo host), `TypeUtils`, `SSLUtils`, `EnginePaths`, `SettingsRepository`.

## Data Flow

### Download

```
AddBookScreen (URL / multi-URL)
  ↓ DownloadQueueState.enqueue(urls)
BookPipeline → PluginManager.get(url) (regex match, auto-install nếu thiếu)
  ↓ BookFetch: DetailLoader + ListLoader → metadata + TOC (mergeFetchedChapters giữ id cũ)
BookDownload (coroutines + Semaphore maxConn): TextLoader từng chương, resume skip chương có sẵn
  ↓ DownloadQueueState (StateFlow) → DownloadQueuePanel / notification (Android)
Export (Ebook/Text/ToC) → output/
```

### Plugin execution

```
URL → PluginManager.get(url) → PluginEntity
  → JsSandbox: Rhino context (pool) + bind JS APIs (Html, Http, Json, Response...)
  → chạy script (detail/toc/chap/gen...) → NativeObject/NativeArray
  → convert về Kotlin (BookEntity, List<BookEntity>, String, PaginatedResult)
```

## Threading

- UI: Compose main dispatcher
- Engine: coroutines qua `EngineDispatchers` (parallelism ≤ 5, khớp Rhino context pool); download fan-out chương bằng `Semaphore(settings.maxConn)`
- Không còn Swing EDT / thread pool thủ công

## Extension Points

- **Export format mới**: thêm class trong `engine/.../action/export/`, nối UI vào `ExportBookDialog`
- **Plugin mới**: file `.plugin` JSON + script JS, đặt vào `tools/plugins/` (hoặc cài từ repository trong app)
- **JS API mới**: class trong `plugin/js/api/`, expose qua JsApiSetup, khai báo whitelist trong SecurityConstants nếu cần

## Security

- Toàn bộ script plugin chạy trong JsSandbox (class/API whitelist)
- Network: HTTPS-only + localhost, giới hạn kích thước, content-type allowlist
- Regex plugin được kiểm ReDoS trước khi dùng; archive plugin được scan trước khi giải nén
- Cookies lưu theo host cho site cần đăng nhập

## Performance

- Download song song theo `maxConn`, resume chỉ tải thiếu
- Rhino context pool tái sử dụng context giữa các lần chạy script
- Cache: plugin repository (VBookPluginService), settings lazy-load (EngineConfig), history persist

## Xem thêm

- `docs/feature-parity-checklist.md` — gate rewrite + trạng thái từng feature
- `docs/designs/kotlin-compose-rewrite-office-hours.md` — design doc rewrite
- `docs/deployment-guide.md`, `RELEASE-WORKFLOW.md` — build & release
