# Deployment Guide — LeechText (Kotlin Compose Multiplatform)

> Bản Java Swing cũ (pipeline DMG/DEB/release-by-tag) đã xóa — xem tag `java-legacy` nếu cần lịch sử.

Hướng dẫn build và phân phối hiện tại: **desktop fat jar + Android APK**. Chưa có pipeline phát hành tự động theo tag.

## Yêu cầu

- JDK 17 (Temurin)
- Android SDK (chỉ cần cho APK)

## Desktop (macOS / Windows / Linux)

```bash
./gradlew :desktop-app:fatJar -x test   # build fat jar (bỏ test)
./gradlew :desktop-app:run              # chạy trực tiếp
make build && make run                  # tương đương qua Makefile
```

Artifact: `desktop-app/build/libs/leechtext-desktop.jar` — jar tự chứa deps, Main-Class `dev.haipham22.leechtext.desktop.MainKt`. Chạy bằng `java -jar` (cần JDK/JRE 17).

Chưa có jpackage/DMG/DEB — giao thác có chủ đích (xem comment trong `desktop-app/build.gradle.kts`). Muốn đóng gói native thì thêm jpackage sau.

## Android

```bash
./gradlew :android-app:assembleDebug     # APK debug
./gradlew :android-app:assembleRelease   # APK release
```

- `applicationId` `dev.haipham22.leechtext`, minSdk 26, targetSdk 35
- versionCode / versionName nằm ở `android-app/build.gradle.kts`
- Release build: R8 + shrinkResources, **ký bằng debug key** — muốn phát hành lên Store cần cấu hình signing riêng

## CI (GitHub Actions)

| Workflow | Trigger | Việc |
|---|---|---|
| `build-multi-platform.yml` (Build Desktop) | push main + PR | macOS runner: fat jar + `:engine:jvmTest`, upload artifact `leechtext-desktop` |
| `code-quality.yml` | push main + PR | `:engine:jvmTest` trên ubuntu |
| `documentation.yml` | push main/dev | validate docs |

Artifact tải từ trang Actions run → artifact `leechtext-desktop`.

## Chất lượng

```bash
./gradlew :engine:jvmTest :app-shared:jvmTest   # unit tests
./gradlew :engine:jacocoJvmReport               # coverage (XML + HTML)
```

SonarQube scan local (xem README).

## Data dir khi chạy

Desktop: `~/.leechtext/` (`output/` sách, `tools/plugins/` plugin, `tools/setting.json` cài đặt). Android: files dir của app.
