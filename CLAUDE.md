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

### Quick Start

```bash
./gradlew build                    # Build project
./gradlew test --tests "*Loader*"  # Run loader tests
./gradlew test                     # Run all tests
./gradlew run                      # Run application
```

### Project Structure

```
src/main/java/dark/leech/text/
├── plugin/js/
│   ├── api/           # JavaScript APIs (Browser, Engine, Html, LocalStorage, UserAgent)
│   ├── loader/        # Loader types (Detail, List, Text, Page)
│   └── sandbox/       # JsSandbox security layer
└── entities/          # Data entities (BookEntity, ChapterEntity, PluginEntity)

src/test/java/dark/leech/text/plugin/
├── js/                # Unit tests for APIs and loaders
└── vbook/             # E2E tests with real plugin structures
```

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

**Utility Methods (Java)**
```java
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

### Gotchas

**Response Handling**
- Always use `Response.isSuccess()` before processing results
- Use `Response.getData()` to unwrap Response objects
- Backward compatibility: Old dual format `{data: ..., data2: ...}` without code field is supported

**Security**
- All plugin execution MUST use JsSandbox for security
- Never execute raw plugin scripts without sandbox
- Browser automation requires Engine API (WebDriver-based)

**Testing**
- E2E tests in `src/test/java/dark/leech/text/plugin/vbook/` use real plugin patterns
- Use `PluginEntity.builder()` to create test plugins with custom scripts
- Test both Response.success() and Response.error() scenarios

### Documentation

Comprehensive project documentation in `./docs/`:
- `./docs/system-architecture.md` — System design and component interactions
- `./docs/code-standards.md` — Coding conventions and style guide
- `./docs/design-guidelines.md` — Design principles and patterns
- `./docs/project-roadmap.md` — Development phases and milestones
