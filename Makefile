# LeechText Build System
# Simple Makefile for building native packages

# Read config from gradle.properties (single source of truth)
VERSION := $(shell grep "^app.version" gradle.properties | cut -d'=' -f2 | tr -d ' ')
MAIN_CLASS := $(shell grep "^app.mainClass" gradle.properties | cut -d'=' -f2 | tr -d ' ')
APP_NAME := $(shell grep "^app.name" gradle.properties | cut -d'=' -f2 | tr -d ' ')
APP_HOME_DIR := $(shell grep "^app.home.dir" gradle.properties | cut -d'=' -f2 | tr -d ' ')
LINUX_PACKAGE_NAME := $(shell echo $(APP_NAME) | tr '[:upper:]' '[:lower:]')

# Build configuration - match Gradle's artifact naming (rootProject.name from settings.gradle)
GRADLE_PROJECT_NAME := $(shell grep "^rootProject.name" settings.gradle | cut -d'=' -f2 | tr -d " '")
JAR_FILE := build/libs/$(GRADLE_PROJECT_NAME)-$(VERSION).jar
LINUX_DEB_PATTERN := $(PACKAGE_DIR)/$(LINUX_PACKAGE_NAME)_$(VERSION)*.deb
PACKAGE_DIR := build/jpackage

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
	$(GRADLEW) clean assemble jar -x test -x pmdMain -x pmdTest -x checkstyleMain -x checkstyleTest -Papp.home.dir="$(HOME)/$(APP_HOME_DIR)"
	@echo "$(COLOR_GREEN)✓ Build complete: $(JAR_FILE)$(COLOR_RESET)"

# Run JAR directly
run: build
	@echo "$(COLOR_BLUE)Running LeechText from JAR...$(COLOR_RESET)"
	java \
		--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED \
		--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED \
		-Dapp.home.dir="$(HOME)/$(APP_HOME_DIR)" \
		-jar $(JAR_FILE)

# Run native app (cross-platform)
run-native:
	@echo "$(COLOR_BLUE)Running native LeechText app...$(COLOR_RESET)"
	@if [ "$(OS)" = "Darwin" ]; then \
		if [ -d "/Applications/$(APP_NAME).app" ]; then \
			LOG_FILE="$${HOME}/$(APP_HOME_DIR)/app.log"; \
			echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
			echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
			/Applications/$(APP_NAME).app/Contents/MacOS/$(APP_NAME) 2>&1 | tee "$$LOG_FILE"; \
		elif [ -f "$(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).dmg" ]; then \
			echo "$(COLOR_YELLOW)Installing DMG first...$(COLOR_RESET)"; \
			hdiutil attach "$(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).dmg" -quiet; \
			cp -r /Volumes/$(APP_NAME)/$(APP_NAME).app /Applications/; \
			hdiutil detach /Volumes/$(APP_NAME) -quiet; \
			LOG_FILE="$${HOME}/$(APP_HOME_DIR)/app.log"; \
			echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
			echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
			/Applications/$(APP_NAME).app/Contents/MacOS/$(APP_NAME) 2>&1 | tee "$$LOG_FILE"; \
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
		LOG_FILE="$${HOME}/$(APP_HOME_DIR)/app.log"; \
		echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
		echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
		$$BINARY_NAME 2>&1 | tee "$$LOG_FILE"; \
	elif ls $(PACKAGE_DIR)/$(LINUX_PACKAGE_NAME)_$(VERSION)*.deb 2>/dev/null; then \
		DEB_FILE=$$(ls $(PACKAGE_DIR)/$(LINUX_PACKAGE_NAME)_$(VERSION)*.deb 2>/dev/null | head -1); \
		echo "$(COLOR_YELLOW)Installing DEB package first...$(COLOR_RESET)"; \
		cd $(PACKAGE_DIR) && sudo apt install -y ./*.deb; \
		LOG_FILE="$${HOME}/$(APP_HOME_DIR)/app.log"; \
		echo "$(COLOR_GREEN)Logging to: $$LOG_FILE$(COLOR_RESET)"; \
		echo "$(COLOR_YELLOW)Press Ctrl+C to stop the app$(COLOR_RESET)"; \
		echo "$(COLOR_BLUE)Searching for installed binary...$(COLOR_RESET)"; \
		if [ -f "/opt/$(APP_NAME)/bin/$(APP_NAME)" ]; then \
			echo "$(COLOR_GREEN)Found binary at /opt/$(APP_NAME)/bin/$(APP_NAME)$(COLOR_RESET)"; \
			/opt/$(APP_NAME)/bin/$(APP_NAME) 2>&1 | tee "$$LOG_FILE"; \
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
			--name $(APP_NAME) \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type dmg \
			--icon src/main/resources/icons/leechtext.icns \
			--input build/libs/ \
			--main-jar $(GRADLE_PROJECT_NAME)-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--java-options "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED" \
			--java-options "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED" \
			--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"; \
	else \
		jpackage \
			--name $(APP_NAME) \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type dmg \
			--input build/libs/ \
			--main-jar $(GRADLE_PROJECT_NAME)-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--java-options "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED" \
			--java-options "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED" \
			--dest $(PACKAGE_DIR)/ || echo "jpackage failed - may need manual invocation"; \
		echo "$(COLOR_YELLOW)⚠ Using default Java icon (custom icon not found)$(COLOR_RESET)"; \
	fi
	@if [ -f "$(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).dmg" ]; then \
		echo "$(COLOR_GREEN)✓ macOS package created: $(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).dmg$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Windows (requires Windows)
package-windows: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Windows EXE...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)⚠ Windows packaging requires Windows OS$(COLOR_RESET)"
	@ls -la build/libs/
	jpackage \
		--name $(APP_NAME) \
		--vendor "LeechText Team" \
		--description "Text extraction and ebook creation tool" \
		--copyright "MIT License" \
		--app-version "$(VERSION)" \
		--type msi \
		--input build/libs/ \
		--main-jar $(GRADLE_PROJECT_NAME)-$(VERSION).jar \
		--main-class $(MAIN_CLASS) \
		--win-menu \
		--win-dir-chooser \
		--win-shortcut \
		--dest $(PACKAGE_DIR)/
	@ls -la $(PACKAGE_DIR)/
	@if [ -f "$(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).exe" ]; then \
		echo "$(COLOR_GREEN)✓ Windows package created: $(PACKAGE_DIR)/$(APP_NAME)-$(VERSION).exe$(COLOR_RESET)"; \
	else \
		echo "$(COLOR_YELLOW)⚠ Package may not have been created$(COLOR_RESET)"; \
	fi

# Package for Linux (requires Linux)
package-linux: build $(PACKAGE_DIR)
	@echo "$(COLOR_BLUE)Creating Linux DEB...$(COLOR_RESET)"
	@if [ -f "src/main/resources/icons/leechtext.png" ]; then \
		echo "Using custom icon: src/main/resources/icons/leechtext.png"; \
		jpackage --verbose \
			--name $(APP_NAME) \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type deb \
			--icon src/main/resources/icons/leechtext.png \
			--input build/libs/ \
			--main-jar $(GRADLE_PROJECT_NAME)-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--linux-shortcut \
			--linux-package-name $(LINUX_PACKAGE_NAME) \
			--dest $(PACKAGE_DIR) 2>&1; \
	else \
		echo "$(COLOR_YELLOW)Using default Java icon (custom icon not found)$(COLOR_RESET)"; \
		jpackage --verbose \
			--name $(APP_NAME) \
			--vendor "LeechText Team" \
			--description "Text extraction and ebook creation tool" \
			--copyright "MIT License" \
			--app-version "$(VERSION)" \
			--type deb \
			--input build/libs/ \
			--main-jar $(GRADLE_PROJECT_NAME)-$(VERSION).jar \
			--main-class $(MAIN_CLASS) \
			--linux-shortcut \
			--linux-package-name $(LINUX_PACKAGE_NAME) \
			--dest $(PACKAGE_DIR) 2>&1; \
	fi
	@if ls $(PACKAGE_DIR)/$(LINUX_PACKAGE_NAME)_$(VERSION)*.deb 2>/dev/null; then \
		DEB_FILE=$$(ls $(PACKAGE_DIR)/$(LINUX_PACKAGE_NAME)_$(VERSION)*.deb 2>/dev/null | head -1); \
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
	@echo "$(COLOR_YELLOW)Log file: $$HOME/$(APP_HOME_DIR)/app.log$(COLOR_RESET)"
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
	@echo "  Home: $$HOME/$(APP_HOME_DIR)"
	@echo "  Working: $$(pwd)"
	@echo "  Cache: $$HOME/$(APP_HOME_DIR)/cache"
	@echo ""
	@echo "$(COLOR_GREEN)Files:$(COLOR_RESET)"
	@if [ -f "$$HOME/$(APP_HOME_DIR)/app.log" ]; then \
		echo "  Log file: $$HOME/$(APP_HOME_DIR)/app.log ($$(wc -l < $$HOME/$(APP_HOME_DIR)/app.log) lines)"; \
	else \
		echo "  Log file: $$HOME/$(APP_HOME_DIR)/app.log (not created yet)"; \
	fi
	@if [ -d "$$HOME/$(APP_HOME_DIR)" ]; then \
		echo "  Config dir exists: ✓"; \
		echo "  Config dir size: $$(du -sh $$HOME/$(APP_HOME_DIR) 2>/dev/null | cut -f1)"; \
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
