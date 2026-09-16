# LeechText — bản Kotlin/Compose (legacy Java đã xoá, tag java-legacy)

GRADLEW := ./gradlew
JAR_FILE := desktop-app/build/libs/leechtext-desktop.jar
PKG := dev.haipham22.leechtext
IOS_SIM := iPhone 17 Pro

.PHONY: all build run dev stop test clean android android-run android-log ios ios-build ios-run ios-log help

all: build

build:
	$(GRADLEW) :desktop-app:fatJar -x test

run: build
	java -jar $(JAR_FILE)

# Dev: tự compile + restart khi code đổi (không giữ state, nhưng khỏi tắt/mở tay)
dev:
	$(GRADLEW) :desktop-app:run --continuous

# Dừng toàn bộ daemon chạy ngầm (Gradle + Kotlin compile daemon sau Ctrl-C make dev)
stop:
	$(GRADLEW) --stop
	pkill -f '[K]otlinCompileDaemon' || true

test:
	$(GRADLEW) :engine:jvmTest

# Android: build + cài + mở app trên emulator/máy đang kết nối (adb)
android:
	$(GRADLEW) :android-app:assembleDebug
	adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk
	adb shell monkey -p $(PKG) -c android.intent.category.LAUNCHER 1

# Chỉ mở app đã cài
android-run:
	adb shell monkey -p $(PKG) -c android.intent.category.LAUNCHER 1

# Log engine (bật debugLog trong Cài đặt trước)
android-log:
	adb logcat -s Engine

# iOS: build (Kotlin framework + Xcode) + cài + mở app trên simulator
ios: ios-build
	xcrun simctl boot "$(IOS_SIM)" 2>/dev/null || true
	open -a Simulator
	xcrun simctl install booted iosApp/build/Build/Products/Debug-iphonesimulator/LeechText.app
	xcrun simctl launch booted $(PKG)

# Chỉ build app iOS (xcodebuild chạy gradle embedAndSignAppleFrameworkForXcode bên trong)
ios-build:
	xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
		-destination 'platform=iOS Simulator,name=$(IOS_SIM)' \
		-derivedDataPath iosApp/build \
		-configuration Debug build

# Chỉ mở app đã cài trên simulator đang boot
ios-run:
	xcrun simctl launch booted $(PKG)

# Log engine trên simulator
ios-log:
	xcrun simctl spawn booted log stream --predicate 'eventMessage CONTAINS "Engine"'

clean:
	$(GRADLEW) clean
