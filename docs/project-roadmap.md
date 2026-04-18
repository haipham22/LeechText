# Project Roadmap - LeechText

## Current Status

**Version**: 2019.03.30
**Status**: Stable Release
**Last Update**: Gradle migration completed, code style improvements applied

## Development Phases

### Phase 1: Core Foundation ✅ COMPLETED
**Status**: Complete
**Deliverables**:
- ✅ Basic Swing UI framework
- ✅ Plugin system architecture
- ✅ HTTP content retrieval
- ✅ Multi-format export (EPUB, Text, ToC)
- ✅ Lua script engine integration

### Phase 2: UI Enhancement ✅ COMPLETED
**Status**: Complete
**Deliverables**:
- ✅ Material Design components
- ✅ Animation system
- ✅ Theme support (dark/light)
- ✅ Responsive layout
- ✅ Progress tracking UI

### Phase 3: Build Modernization ✅ COMPLETED
**Status**: Complete
**Deliverables**:
- ✅ Migrated to Gradle 8.4
- ✅ Updated to Java 17
- ✅ Added Spotless code formatting
- ✅ Integrated Checkstyle and PMD
- ✅ Configured pre-commit hooks

### Phase 4: Quality Improvements ✅ COMPLETED
**Status**: Complete (100% complete)
**Deliverables**:
- ✅ Code style improvements
- ✅ Remove unused code
- ✅ Refactor duplicated variables
- ✅ JavaScript API Layer implementation with vBook compatibility
- ✅ WebP cover image conversion fix
- ✅ Plugin security validation system
- ✅ HTML sanitization for EPUB
- ✅ Comprehensive documentation updates

**Completed Features**:
- **WebP Image Converter**: Automatic conversion of WebP covers to JPEG for EPUB compatibility
- **JavaScript Engine**: Rhino-based JavaScript support with vBook API compatibility
- **Plugin Security**: Network validation, regex scanning, and archive security
- **HTML Sanitizer**: EPUB XML validation and tag cleanup utilities

### Phase 5: Feature Enhancements 📋 IN PROGRESS
**Status**: In Progress (20% complete)
**Deliverables**:
- ✅ JavaScript Plugin System (completed April 2026)
- ✅ WebP Cover Image Fix (completed April 2026)
- ✅ Plugin Security Validation (completed April 2026)
- ✅ HTML Sanitization Utilities (completed April 2026)
- ⏳ Multi-language UI support (Vietnamese, English)
- ⏳ Cloud sync for settings
- ⏳ Advanced content editor
- ⏳ Batch download improvements
- ⏳ Custom plugin repository browser

**Recent Completions (April 2026)**:
- Added Rhino JavaScript engine with vBook API compatibility
- Implemented WebP to JPEG conversion for EPUB cover images
- Created comprehensive plugin security validation system
- Added HTML sanitization for EPUB XML validation

**Estimated Timeline**: Q2-Q3 2026

### Phase 6: Platform Expansion 📋 PLANNED
**Status**: Planned
**Priority**: Low
**Deliverables**:
- ⏳ Web-based interface
- ⏳ Mobile companion app
- ⏳ CLI interface
- ⏳ Headless mode for servers

**Estimated Timeline**: Q3-Q4 2026

## Short-Term Goals (Next 1-2 Months)

### Priority 1: Complete Documentation
- [ ] Finalize API documentation
- [ ] Complete plugin development guide
- [ ] User manual translation
- [ ] Developer setup guide

### Priority 2: Testing
- [ ] Unit tests for core components (target: 70% coverage)
- [ ] Integration tests for plugin system
- [ ] UI automation tests
- [ ] Performance benchmarks

### Priority 3: Code Quality
- [ ] Address Checkstyle warnings
- [ ] Resolve PMD violations
- [ ] Security audit
- [ ] Dependency updates

## Medium-Term Goals (Next 3-6 Months)

### Feature Enhancements
- **Multi-language Support**: Full i18n implementation
- **Advanced Editor**: Built-in content editor with preview
- **Smart Detection**: Automatic plugin suggestion
- **Download Scheduler**: Schedule downloads for off-peak hours
- **Cloudflare V2**: Updated bypass mechanism

### User Experience
- **Tutorial Mode**: First-run guided tour
- **Keyboard Shortcuts**: Comprehensive keyboard navigation
- **Custom Themes**: User-created theme support
- **Plugin Marketplace**: In-app plugin browser

## Long-Term Vision (6-12 Months)

### Platform Expansion
- **Web Version**: Browser-based interface
- **Mobile Apps**: iOS and Android companions
- **CLI Tool**: Command-line interface for power users
- **Docker Image**: Containerized deployment

### Advanced Features
- **AI Integration**: Content summarization and cleanup
- **Cloud Sync**: Cross-device synchronization
- **Collaborative Editing**: Share and edit downloads
- **Version Control**: Track content changes

## Technical Debt

### High Priority
1. **Test Coverage**: Currently minimal, needs comprehensive suite
2. **Error Handling**: Inconsistent error handling patterns
3. **Thread Safety**: Review concurrent access patterns
4. **Memory Management**: Potential memory leaks in long-running operations

### Medium Priority
1. **Package Naming**: `enities` should be `entities`
2. **Deprecated Code**: Remove unused dependencies
3. **Logging**: Implement proper logging framework (SLF4J)
4. **Configuration**: Externalize hardcoded values

### Low Priority
1. **UI Consistency**: Standardize component usage
2. **Documentation**: Inline code comments
3. **Performance**: Profile and optimize hot paths
4. **Accessibility**: Add ARIA labels and keyboard support

## Release Schedule

### v2025.1.0 (Planned - Q1 2025)
**Focus**: Testing and Documentation
- Comprehensive test suite
- Complete API documentation
- Plugin development guide
- Bug fixes and stability improvements

### v2025.2.0 (Planned - Q2 2025)
**Focus**: Feature Enhancements
- Multi-language UI support
- Advanced content editor
- Improved plugin management
- Performance optimizations

### v2026.1.0 (Planned - Q3 2026)
**Focus**: Platform Expansion
- Web-based interface
- Mobile companion apps
- CLI interface
- Cloud sync beta

## Success Metrics

### Code Quality
- **Test Coverage**: Target 70%+
- **Checkstyle**: Zero violations
- **PMD**: Zero high-priority violations
- **Spotless**: Always passing

### User Experience
- **Startup Time**: < 2 seconds
- **Download Speed**: Maximize bandwidth usage
- **Memory Usage**: < 512MB for typical workload
- **Crash Rate**: < 0.1%

### Community
- **Plugin Ecosystem**: 50+ community plugins
- **Active Users**: Monthly active users tracking
- **Issue Response**: < 48 hours for bugs
- **Documentation**: 100% feature coverage

## Dependencies Roadmap

### Upcoming Updates
| Dependency | Current | Target | Priority |
|------------|---------|--------|----------|
| Java | 17 | 21 (LTS) | Medium |
| Gradle | 8.4 | 8.5+ | Low |
| JSoup | 1.16.1 | 1.17+ | Low |
| HttpClient5 | 5.2.1 | 5.3+ | Low |
| LuaJ | 3.0.1 | 3.0.2+ | Low |

### Deprecation Timeline
- **Java 8 Support**: Dropped (migrated to Java 17)
- **Legacy HTTP Client**: Replaced with HttpClient5
- **Old Build System**: Ant/Maven replaced with Gradle

## Contribution Guidelines

We welcome contributions in the following areas:

1. **Plugin Development**: Create extraction plugins for new sites
2. **Bug Fixes**: Help squash bugs and improve stability
3. **Documentation**: Improve guides and translate documentation
4. **Testing**: Write tests for untested components
5. **Features**: Propose and implement new features

See `CONTRIBUTING.md` for detailed guidelines.

## License

This project is licensed under the MIT License. See `LICENSE` file for details.
