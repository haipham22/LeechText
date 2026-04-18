# LeechText Build System
# Simple Makefile for building native packages

# Read version from build.gradle
VERSION := $(shell grep "^version " build.gradle | awk -F"'" '{print $$2}')

# Build configuration
JAR_FILE := build/libs/leechtext-java-$(VERSION).jar
PACKAGE_DIR := build/jpackage
MAIN_CLASS := dark.leech.text.ui.main.App

# Colors for output
COLOR_RESET := \033[0m
COLOR_BOLD := \033[1m
COLOR_GREEN := \033[32m
COLOR_YELLOW := \033[33m
COLOR_BLUE := \033[34m

.PHONY: all build clean package package-mac package-windows package-linux help

# Default target
all: build

# Build JAR file
build:
	@echo "$(COLOR_BLUE)Building LeechText v$(VERSION)...$(COLOR_RESET)"
	./gradlew clean assemble jar -x test -x pmdMain -x pmdTest -x checkstyleMain -x checkstyleTest
	@echo "$(COLOR_GREEN)✓ Build complete: $(JAR_FILE)$(COLOR_RESET)"

# Clean build artifacts
clean:
	@echo "$(COLOR_YELLOW)Cleaning build artifacts...$(COLOR_RESET)"
	./gradlew clean
	rm -rf $(PACKAGE_DIR)
	@echo "$(COLOR_GREEN)✓ Clean complete$(COLOR_RESET)"

# Create package directory
$(PACKAGE_DIR):
	@mkdir -p $(PACKAGE_DIR)

# Detect OS
OS := $(shell uname -s)

# Package for current platform
package: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating native package for $(OS)...$(COLOR_RESET)"
	@if [ "$(OS)" = "Darwin" ]; then \
		$(MAKE) package-mac; \
	elif [ "$(OS)" = "Linux" ]; then \
		$(MAKE) package-linux; \
	else \
		echo "$(COLOR_YELLOW)Unknown OS: $(OS)$(COLOR_RESET)"; \
		exit 1; \
	fi

# Package for macOS
package-mac: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating macOS DMG...$(COLOR_RESET)"
	jpackage \
		--name LeechText \
		--vendor "LeechText Team" \
		--description "Text extraction and ebook creation tool" \
		--copyright "MIT License" \
		--app-version "$(VERSION)" \
		--type dmg \
		--input build/libs/ \
		--main-jar leechtext-java-$(VERSION).jar \
		--main-class $(MAIN_CLASS) \
		--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"
	@if [ -f "$(PACKAGE_DIR)/LeechText-$(VERSION).dmg" ]; then \
		echo "$(COLOR_GREEN)✓ macOS package created: $(PACKAGE_DIR)/LeechText-$(VERSION).dmg$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Windows (requires Windows)
package-windows: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Windows EXE...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)⚠ Windows packaging requires Windows OS$(COLOR_RESET)"
	jpackage \
		--name LeechText \
		--vendor "LeechText Team" \
		--description "Text extraction and ebook creation tool" \
		--copyright "MIT License" \
		--app-version "$(VERSION)" \
		--type exe \
		--input build/libs/ \
		--main-jar leechtext-java-$(VERSION).jar \
		--main-class $(MAIN_CLASS) \
		--win-menu \
		--win-dir-chooser \
		--win-shortcut \
		--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"
	@if [ -f "$(PACKAGE_DIR)/LeechText-$(VERSION).exe" ]; then \
		echo "$(COLOR_GREEN)✓ Windows package created: $(PACKAGE_DIR)/LeechText-$(VERSION).exe$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Linux (requires Linux)
package-linux: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Linux DEB...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)⚠ Linux packaging requires Linux OS$(COLOR_RESET)"
	jpackage \
		--name LeechText \
		--vendor "LeechText Team" \
		--description "Text extraction and ebook creation tool" \
		--copyright "MIT License" \
		--app-version "$(VERSION)" \
		--type deb \
		--input build/libs/ \
		--main-jar leechtext-java-$(VERSION).jar \
		--main-class $(MAIN_CLASS) \
		--linux-shortcut \
		--linux-package-name leechtext \
		--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"
	@if [ -f "$(PACKAGE_DIR)/leechtext_$(VERSION)_amd64.deb" ]; then \
		echo "$(COLOR_GREEN)✓ Linux package created: $(PACKAGE_DIR)/leechtext_$(VERSION)_amd64.deb$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Show help
help:
	@echo "$(COLOR_BOLD)LeechText Build System$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_GREEN)Available targets:$(COLOR_RESET)"
	@echo "  $(COLOR_BLUE)make$(COLOR_RESET)           - Build JAR file (default)"
	@echo "  $(COLOR_BLUE)make build$(COLOR_RESET)     - Build JAR file"
	@echo "  $(COLOR_BLUE)make clean$(COLOR_RESET)     - Clean build artifacts"
	@echo "  $(COLOR_BLUE)make package$(COLOR_RESET)   - Create native package for current OS"
	@echo "  $(COLOR_BLUE)make package-mac$(COLOR_RESET) - Create macOS DMG"
	@echo "  $(COLOR_BLUE)make package-windows$(COLOR_RESET) - Create Windows EXE"
	@echo "  $(COLOR_BLUE)make package-linux$(COLOR_RESET) - Create Linux DEB"
	@echo "  $(COLOR_BLUE)make help$(COLOR_RESET)      - Show this help message"
	@echo ""
	@echo "$(COLOR_YELLOW)Version: $(VERSION)$(COLOR_RESET)"
