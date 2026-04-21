# Project Changelog

This document tracks all significant changes, features, and fixes in the LeechText Java project.

## [Unreleased]

### Features
- **NEW**: Pagination support for JavaScript plugins (GenLoader) - (2026-04-21)
  - Introduced `GenLoader` for handling paginated content (search results, chapter lists)
  - Added `PaginationResult<T>` generic result model with builder pattern
  - Enhanced `Response` class with overloaded `success(data, next)` method for pagination
  - Added support for `LoaderType.GEN` with isolated JavaScript context
  - Enabled cursor-based, token-based, and URL-based pagination patterns
  - See `docs/system-architecture.md` → "Pagination Feature" for details

### Dependencies
- **UPGRADED**: jsoup from 1.16.1 to 1.22.1 (2026-04-20)
  - **Breaking Changes**:
    - Attribute selector values are no longer trimmed (`[attr=" foo "]` now matches literally)
    - HTML parser now defaults to max depth 512 (was unlimited)
    - `<br>` element is now classified as inline (was block)
  - **New Features**:
    - re2j regex engine support for safer regex-based selectors
    - Configurable maximum parser depth (security improvement)
    - Better CSS selector compliance
    - Improved proxy handling
    - Fixed truncation issues in remote document fetching
  - **Impact**: Low - Codebase doesn't rely on trimmed attribute matching, 512 depth limit is generous
  - **Testing**: All existing tests pass with new version

### Bug Fixes
- **FIXED**: Native app working directory issue (2026-04-20)
  - Changed from platform-specific paths to consistent `~/.leech` across all platforms
  - Resolves permission issues in macOS .app bundles, Windows Program Files, Linux /opt
  - See `docs/native-app-working-directory-fix.md` for details

## [1.1.0] - 2026-04-18

### Features
- Added comprehensive vBook plugin system with JavaScript sandboxing
- Implemented plugin repository management
- Added plugin security validation and regex security checks
- Implemented home directory permission handling for native applications

### Infrastructure
- Added CI/CD pipeline improvements
- Enhanced macOS universal binary support
- Updated application icon loading with macOS dock icon support

## [1.0.2] - Earlier

### Initial Features
- Basic HTML parsing and text extraction
- JavaScript plugin API
- HTTP client functionality
- File utilities and configuration management

---

## Version Format

- **[Unreleased]**: Changes that are planned or in development
- **[Version]**: Released versions with dates
- **Categories**: Dependencies, Features, Bug Fixes, Infrastructure, Documentation

## Notes

- This changelog follows the [Keep a Changelog](https://keepachangelog.com/) format
- Version numbers follow semantic versioning (MAJOR.MINOR.PATCH)
- Dates are in YYYY-MM-DD format
