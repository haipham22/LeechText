# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## Project-Specific Context

Kotlin + Compose Multiplatform (desktop + Android). Bản Java Swing gốc ở tag `java-legacy`.

### Quick Start

```bash
./gradlew :desktop-app:run             # Run desktop app
./gradlew :android-app:assembleDebug   # Build Android APK
./gradlew :engine:jvmTest              # Engine unit tests
./gradlew :app-shared:jvmTest          # Shared UI tests
./gradlew build                        # Build everything
```

### Project Structure

```
engine/src/jvmMain/kotlin/dev/haipham22/leechtext/
├── plugin/js/
│   ├── api/           # JavaScript APIs (Browser, Engine, Html, Http, Json, LocalStorage, Regexp, UserAgent)
│   ├── loader/        # Loader types (Detail, List, Text, Page, Browse) + Response
│   └── sandbox/       # JsSandbox security layer
├── plugin/security/   # Network / Regex (ReDoS) / Zip validators
├── plugin/vbook/      # vBook plugin repository service + converter
├── entities/          # BookEntity, ChapterEntity, PluginEntity, RepositoryEntity
├── action/export/     # EPUB/TXT/HTML export
├── get/               # Fetch/download pipeline
├── models/, util/

app-shared/src/
├── commonMain/        # Shared UI — atomic layers (không phụ thuộc :engine)
│   └── ui/
│       ├── theme/     # LeechTextTheme, DisplayFont/MonoFont, AppThemePref
│       ├── model/     # UiText, QueueItemUi, RailItem, QueuePanelCallbacks
│       └── components/
│           ├── atoms/       # widget lá stateless, nhận Modifier từ ngoài
│           ├── molecules/   # AppRail, AppBottomBar, BookCoverTile
│           └── organisms/   # DownloadQueuePanel
├── sharedMain/        # UI cần :engine + Decompose; screens (Pages) + state components
│   └── ui/
│       ├── components/{atoms,organisms}/  # CoverImage; BookDetailBody
│       ├── library|sources|history|settings|addbook|reader/  # Pages + feature-local helpers
│       └── SliceApp.kt                    # Template shell (rail/bottom bar + content)
├── sharedJvmMain/, jvmMain/, androidMain/, iosMain/  # Platform actuals

desktop-app/           # Compose Desktop entry
android-app/           # Android entry
```

Tests: `engine/src/jvmTest/`, `app-shared/src/jvmTest/` (package `dev.haipham22.leechtext`).

### UI Convention (Atomic Design)

Phân lớp Compose: **Atoms** = widget lá stateless, nhận `Modifier` từ ngoài; **Molecules** = kết hợp atoms cho 1 chức năng đơn giản; **Organisms** = section hoàn chỉnh của màn hình; **Templates** = bộ khung layout (SliceApp hiện đóng vai trò này — chỉ tách package riêng khi có layout thứ 2); **Pages** = screen entry `*Screen` nhận state từ Decompose component — state class (`*State`) nằm file riêng, không nhúng trong file screen. Component chung để `commonMain/ui/components/`; component phụ thuộc kiểu `:engine` (vd `Properties`) để `sharedMain`. UI thuần Compose + Material 3 — không thêm thư viện UI View-based (RikkaUi v.v.).

### Key Concepts

**vBooks Plugin System**
- JavaScript-based plugin architecture using Rhino engine
- Plugins execute in secure JsSandbox with restricted access
- Response wrapper pattern for vBooks compatibility: `{code: 0/1, data: ..., data2: ...}`

**Response API**
```javascript
Response.success(data)              // Success response
Response.success(data, data2)       // Success with pagination token
Response.error(data)                // Error with default code 1
Response.error(code, data)          // Error with custom code
```

**Utility Methods (Kotlin — `plugin/js/loader/Response.kt`)**
```kotlin
Response.isSuccess(result)          // Check if response is successful
Response.getData(result)            // Extract data field
Response.getData2(result)           // Extract data2 (pagination token)
Response.getErrorMessage(result)    // Extract error message
```

**Loader Types**
- **DetailLoader**: Fetches book metadata (name, author, description, cover, etc.)
- **ListLoader**: Fetches table of contents (chapter list)
- **TextLoader**: Fetches chapter content
- **PageLoader**: Discovers all page URLs for pagination
- **BrowseLoader**: Fetches source catalog (home/genre/search tabs)

### Gotchas

**Response Handling**
- Always use `Response.isSuccess()` before processing results
- Use `Response.getData()` to unwrap Response objects
- Backward compatibility: Old dual format `{data: ..., data2: ...}` without code field is supported

**Security**
- All plugin execution MUST use JsSandbox for security
- Never execute raw plugin scripts without sandbox
- Browser automation requires Engine API (Playwright-based)

**Testing**
- E2E tests in `engine/src/jvmTest/.../plugin/vbook/` use real plugin patterns
- `PluginEntity` is a Kotlin data class — use named args / `copy()`, no builder
- Test both Response.success() and Response.error() scenarios

**Multiplatform**
- Business logic lives in `:engine` (JVM) — no Compose dependency
- Shared UI in `:app-shared` commonMain; platform-specific code in jvmMain/androidMain

**DI (Koin)**
- `startAppKoin()` gọi ở platform entry (desktop Main / MainActivity / MainViewController) TRƯỚC khi tạo RootComponent
- Logger: inject `EngineLogger` qua constructor (Koin single `platformEngineLogger()`), KHÔNG dùng static `EngineLog` facade (deprecated — chỉ còn cho vài `object` engine chưa convert: PluginManager, PluginUpdate...)
- State Decompose nhận `log` từ RootComponent → thread xuống engine qua param `log`
- Thêm dependency mới: đăng ký trong `di/AppModule.kt` (`single { X() }`)

### Documentation

Project documentation in `./docs/`:
- `./docs/system-architecture.md` — System design and component interactions
- `./docs/code-standards.md` — Coding conventions and style guide
- `./docs/project-roadmap.md` — Development phases and milestones
- `./docs/designs/` — Design docs (Kotlin Compose rewrite office hours)

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore
- Author a backlog-ready spec/issue → invoke /spec
