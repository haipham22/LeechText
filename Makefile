# LeechText Build System
# Simple Makefile for building native packages

# Read version from gradle.properties (single source of truth)
VERSION := $(shell grep "^app.version" gradle.properties | cut -d'=' -f2 | tr -d ' ')

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

.PHONY: all build clean package package-mac package-windows package-linux help run run-native run-logs info

# Default target
all: build

# Detect OS
UNAME_S := $(shell uname -s 2>/dev/null || echo Windows)

# Gradle wrapper command - use ./gradlew for all platforms (works on Windows too via bash)
GRADLEW := ./gradlew

# Build JAR file
build:
	@echo "$(COLOR_BLUE)Building LeechText v$(VERSION)...$(COLOR_RESET)"
	$(GRADLEW) clean assemble jar -x test -x pmdMain -x pmdTest -x checkstyleMain -x checkstyleTest -Papp.home.dir="$(HOME)/.leechtext"
	@echo "$(COLOR_GREEN)✓ Build complete: $(JAR_FILE)$(COLOR_RESET)"

# Run JAR directly
run: build
	@echo "$(COLOR_BLUE)Running LeechText from JAR...$(COLOR_RESET)"
	java \
		--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED \
		--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED \
		-Dapp.home.dir="$(HOME)/.leechtext" \
		-jar $(JAR_FILE)

# Run native app (cross-platform)
run-native:
	@echo "$(COLOR_BLUE)Running native LeechText app...$(COLOR_RESET)"
	@if [ "$(OS)" = "Darwin" ]; then \
		if [ -d "/Applications/LeechText.app" ]; then \
			LOG_FILE="$${HOME}/.leechtext/app.log"; \
			echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
			echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
			/Applications/LeechText.app/Contents/MacOS/LeechText 2>&1 | tee "$$LOG_FILE"; \
		elif [ -f "$(PACKAGE_DIR)/LeechText-$(VERSION).dmg" ]; then \
			echo "$(COLOR_YELLOW)Installing DMG first...$(COLOR_RESET)"; \
			hdiutil attach "$(PACKAGE_DIR)/LeechText-$(VERSION).dmg" -quiet; \
			cp -r /Volumes/LeechText/LeechText.app /Applications/; \
			hdiutil detach /Volumes/LeechText -quiet; \
			LOG_FILE="$${HOME}/.leechtext/app.log"; \
			echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
			echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
			/Applications/LeechText.app/Contents/MacOS/LeechText 2>&1 | tee "$$LOG_FILE"; \
		else \
			echo "$(COLOR_YELLOW)⚠ No native package found. Run 'make package-mac' first.$(COLOR_RESET)"; \
		fi; \
	elif [ "$(OS)" = "Linux" ]; then \
		$(MAKE) run-native-linux; \
	else \
		echo "$(COLOR_YELLOW)⚠ run-native not supported on this OS. Use 'make run' instead.$(COLOR_RESET)"; \
	fi

# Run native app (Linux)
run-native-linux: package-linux
	@echo "$(COLOR_BLUE)Running native LeechText app on Linux...$(COLOR_RESET)"
	@BINARY_NAME="$$(which leechtext 2>/dev/null)"; \
	if [ -n "$$BINARY_NAME" ]; then \
		LOG_FILE="$${HOME}/.leechtext/app.log"; \
		echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
		echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
		$$BINARY_NAME 2>&1 | tee "$$LOG_FILE"; \
	elif ls $(PACKAGE_DIR)/leechtext_$(VERSION)*.deb 2>/dev/null; then \
		DEB_FILE=$$(ls $(PACKAGE_DIR)/leechtext_$(VERSION)*.deb 2>/dev/null | head -1); \
		echo "$(COLOR_YELLOW)Installing DEB package first...$(COLOR_RESET)"; \
		cd $(PACKAGE_DIR) && sudo apt install -y ./*.deb; \
		LOG_FILE="$${HOME}/.leechtext/app.log"; \
		echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
		echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
		echo "$(COLOR_BLUE)Searching for installed binary...$(COLOR_RESET)"; \
		if [ -f "/opt/LeechText/bin/LeechText" ]; then \
			echo "$(COLOR_GREEN)Found binary at /opt/LeechText/bin/LeechText$(COLOR_RESET)"; \
			/opt/LeechText/bin/LeechText 2>&1 | tee "$$LOG_FILE"; \
		elif [ -f "/usr/bin/leechtext" ]; then \
			echo "$(COLOR_GREEN)Found binary at /usr/bin/leechtext$(COLOR_RESET)"; \
			/usr/bin/leechtext 2>&1 | tee "$$LOG_FILE"; \
		elif [ -f "/usr/local/bin/leechtext" ]; then \
			echo "$(COLOR_GREEN)Found binary at /usr/local/bin/leechtext$(COLOR_RESET)"; \
			/usr/local/bin/leechtext 2>&1 | tee "$$LOG_FILE"; \
		else \
			echo "$(COLOR_YELLOW)⚠ Could not find leechtext binary. Searched: /opt/LeechText/bin/, /usr/bin/, /usr/local/bin/$(COLOR_RESET)"; \
			echo "$(COLOR_YELLOW)Try running: sudo updatedb && locate leechtext$(COLOR_RESET)"; \
		fi; \
	else \
		echo "$(COLOR_YELLOW)⚠ No native package found. Run 'make package-linux' first.$(COLOR_RESET)"; \
	fi

# Run native app with logging
run-logs: run-native

# Clean build artifacts
clean:
	@echo "$(COLOR_YELLOW)Cleaning build artifacts...$(COLOR_RESET)"
	$(GRADLEW) clean
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
	@echo "$(COLOR_BLUE)Creating macOS Universal DMG...$(COLOR_RESET)"
	@if [ -f "src/main/resources/icons/leechtext.icns" ]; then \
		jpackage \
			--name LeechText \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type dmg \
			--icon src/main/resources/icons/leechtext.icns \
			--input build/libs/ \
			--main-jar leechtext-java-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--java-options "-Dapp.home.dir=$$HOME/.leechtext" \
			--java-options "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED" \
			--java-options "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED" \
			--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"; \
	else \
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
			--java-options "-Dapp.home.dir=$$HOME/.leechtext" \
			--java-options "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED" \
			--java-options "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED" \
			--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"; \
		echo "$(COLOR_YELLOW)⚠ Using default Java icon (custom icon not found)$(COLOR_RESET)"; \
	fi
	@if [ -f "$(PACKAGE_DIR)/LeechText-$(VERSION).dmg" ]; then \
		echo "$(COLOR_GREEN)✓ macOS package created: $(PACKAGE_DIR)/LeechText-$(VERSION).dmg$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Windows (requires Windows)
package-windows: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Windows EXE...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)⚠ Windows packaging requires Windows OS$(COLOR_RESET)"
	@ls -la build/libs/
	jpackage \
		--name LeechText \
		--vendor "LeechText Team" \
		--description "Text extraction and ebook creation tool" \
		--copyright "MIT License" \
		--app-version "$(VERSION)" \
		--type msi \
		--input build/libs/ \
		--main-jar leechtext-java-$(VERSION).jar \
		--main-class $(MAIN_CLASS) \
		--java-options "-Dapp.home.dir=%USERPROFILE%\.leechtext" \
		--win-menu \
		--win-dir-chooser \
		--win-shortcut \
		--dest $(PACKAGE_DIR)/
	@ls -la $(PACKAGE_DIR)/
	@if [ -f "$(PACKAGE_DIR)/LeechText-$(VERSION).exe" ]; then \
		echo "$(COLOR_GREEN)✓ Windows package created: $(PACKAGE_DIR)/LeechText-$(VERSION).exe$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Linux (requires Linux)
package-linux: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Linux DEB...$(COLOR_RESET)"
	@if [ -f "src/main/resources/icons/leechtext.png" ]; then \
		echo "Using custom icon: src/main/resources/icons/leechtext.png"; \
		jpackage --verbose \
			--name LeechText \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type deb \
			--icon src/main/resources/icons/leechtext.png \
			--input build/libs/ \
			--main-jar leechtext-java-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--java-options "-Dapp.home.dir=$$HOME/.leechtext" \
			--linux-shortcut \
			--linux-package-name leechtext \
			--dest $(PACKAGE_DIR) 2>&1; \
	else \
		echo "$(COLOR_YELLOW)Using default Java icon (custom icon not found)$(COLOR_RESET)"; \
		jpackage --verbose \
			--name LeechText \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type deb \
			--input build/libs/ \
			--main-jar leechtext-java-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--java-options "-Dapp.home.dir=$$HOME/.leechtext" \
			--linux-shortcut \
			--linux-package-name leechtext \
			--dest $(PACKAGE_DIR) 2>&1; \
	fi
	@if ls $(PACKAGE_DIR)/leechtext_$(VERSION)*.deb 2>/dev/null; then \
		DEB_FILE=$$(ls $(PACKAGE_DIR)/leechtext_$(VERSION)*.deb 2>/dev/null | head -1); \
		echo "$(COLOR_GREEN)✓ Linux package created: $$DEB_FILE$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package was not created. Check jpackage output above.$(COLOR_RESET)"; \
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
	@echo "  $(COLOR_BLUE)make run$(COLOR_RESET)           - Run from JAR"
	@echo "  $(COLOR_BLUE)make run-native$(COLOR_RESET)   - Run installed native app (macOS & Linux)"
	@echo "  $(COLOR_BLUE)make run-logs$(COLOR_RESET)     - Run with verbose logging to console & file"
	@echo "  $(COLOR_BLUE)make info$(COLOR_RESET)         - Show system information and settings location"
	@echo "  $(COLOR_BLUE)make help$(COLOR_RESET)      - Show this help message"
	@echo ""
	@echo "$(COLOR_YELLOW)Version: $(VERSION)$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)Log file: $$HOME/.leechtext/app.log$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_BLUE)Run 'make info' for more system information$(COLOR_RESET)"

# Show system information
info:
	@echo "$(COLOR_BOLD)LeechText System Information$(COLOR_RESET)"
	@echo ""
	@echo "$(COLOR_GREEN)Application:$(COLOR_RESET)"
	@echo "  Version: $(VERSION)"
	@echo "  Build time: $(shell grep "^app.buildTime" gradle.properties | cut -d'=' -f2 | tr -d ' ')"
	@echo "  Copyright: $(shell grep "^app.copyright" gradle.properties | cut -d'=' -f2 | tr -d ' ')"
	@echo ""
	@echo "$(COLOR_GREEN)Directories:$(COLOR_RESET)"
	@echo "  Home: $$HOME/.leechtext"
	@echo "  Working: $$(pwd)"
	@echo "  Cache: $$HOME/.leechtext/cache"
	@echo ""
	@echo "$(COLOR_GREEN)Files:$(COLOR_RESET)"
	@if [ -f "$$HOME/.leechtext/app.log" ]; then \
		echo "  Log file: $$HOME/.leechtext/app.log ($$(wc -l < $$HOME/.leechtext/app.log) lines)"; \
	else \
		echo "  Log file: $$HOME/.leechtext/app.log (not created yet)"; \
	fi
	@if [ -d "$$HOME/.leechtext" ]; then \
		echo "  Config dir exists: ✓"; \
		echo "  Config dir size: $$(du -sh $$HOME/.leechtext 2>/dev/null | cut -f1)"; \
	else \
		echo "  Config dir exists: ✗ (will be created on first run)"; \
	fi
	@echo ""
	@echo "$(COLOR_GREEN)Java:$(COLOR_RESET)"
	@echo "  Version: $$(java -version 2>&1 | head -n 1)"
	@echo "  Home: $$JAVA_HOME"
	@echo ""
	@echo "$(COLOR_GREEN)System:$(COLOR_RESET)"
	@echo "  OS: $(UNAME_S)"
	@echo "  User: $$USER"
	@echo "  Architecture: $$(uname -m)"
