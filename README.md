# LeechText

Tải truyện từ web về đọc offline — xuất EPUB/TXT/HTML. Viết lại bằng **Kotlin + Compose Multiplatform**, chạy chung một codebase trên **Desktop (macOS/Windows/Linux), Android và iOS**.

[![Code Quality](https://github.com/haipham22/LeechText/actions/workflows/code-quality.yml/badge.svg)](https://github.com/haipham22/LeechText/actions/workflows/code-quality.yml)
[![Release](https://img.shields.io/github/v/release/haipham22/LeechText)](https://github.com/haipham22/LeechText/releases)
[![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows%20%7C%20Linux%20%7C%20Android%20%7C%20iOS-6ba7c6)](https://github.com/haipham22/LeechText/releases)

> **Tải bản dựng:** [Releases](https://github.com/haipham22/LeechText/releases) — desktop `jar` / `deb` / `dmg` / `msi`, Android `apk`, iOS `ipa` (unsigned — tự ký qua Sideloadly/AltStore).

<!-- Screenshots: docs/screenshots/{library,reader}.png — chạy `./gradlew :desktop-app:run` rồi Cmd-Shift-4, thêm vào đây -->

## Tính năng

- **Thêm sách bằng URL** — dán link, engine tự dò plugin khớp trong repository vBook và cài (auto-discovery); xem preview metadata + đọc thử chương trước khi thêm.
- **Tải song song + hàng đợi** — nhiều sách/chương cùng lúc, có thể hủy, tải lại, resume chỉ tải chương thiếu.
- **Thư viện** — grid bìa sách, sắp xếp theo mới thêm/mới đọc/tên, kiểm tra chương mới, sửa tên chương hàng loạt (auto-fix/optimize).
- **Đọc sách** — reader tích hợp, **nhớ vị trí đọc từng sách**, chỉnh cỡ chữ, bookmark, lọc rác nội dung (trash rules) ngay khi đọc.
- **Plugin vBook** — hệ plugin JavaScript (Rhino) chạy trong sandbox bảo mật (validator network + regex ReDoS), kho plugin theo ngôn ngữ, pin nguồn, cập nhật version, quản lý repository riêng.
- **Xuất file** — EPUB (kèm ảnh, nén tùy chọn, chia quyển) qua Calibre/KindleGen hoặc engine tự chế, TXT/HTML, mục lục NCX/OPF, bìa WebP→JPEG.
- **Hai ngôn ngữ** — Tiếng Việt/English, đổi trong Cài đặt.
- **Cookies** — lưu theo host cho site cần đăng nhập.

## Kiến trúc

```
engine/        # Core: plugin system (Rhino sandbox), fetch/download, export, settings — thuần JVM/KMP, không UI
app-shared/    # UI Compose Multiplatform dùng chung (screens, state ViewModels, theme)
desktop-app/   # Entry desktop (Compose Desktop, window 1280x800)
android-app/   # Entry Android (share toàn bộ UI + engine với desktop)
iosApp/        # Entry iOS (Xcode project nhúng KMP framework)
```

Kotlin **2.4.10** · Compose Multiplatform **1.12.0** · Gradle **9.7.0** · JDK **17**

Bản Java Swing gốc nằm ở tag `java-legacy` (đã xóa khỏi working tree).

## Chạy & build

```bash
./gradlew :desktop-app:run             # chạy app desktop
./gradlew :android-app:assembleDebug   # APK Android
make ios                               # build + cài + mở trên iOS simulator
./gradlew build                        # build tất cả
```

Dữ liệu app nằm ở `~/.leechtext/` (Android: files dir của app): `output/` chứa sách, `tools/plugins/` chứa plugin, `tools/setting.json` là cài đặt.

## Plugin system

Plugin vBook là JS chạy trong **JsSandbox**: class/API whitelist, chặn network ngoài HTTPS whitelist, validator regex chống ReDoS, giới hạn kích thước file. API cho plugin: `fetch()`, `Html`, `Json`, `Response`, `localStorage`, `Regexp`... — xem [docs/system-architecture.md](docs/system-architecture.md).

Cài plugin: tab **Nguồn** → kho vBook theo ngôn ngữ, hoặc dán URL sách chưa có plugin → engine tự dò + cài.

## Kiểm thử & chất lượng

```bash
./gradlew :engine:jvmTest :app-shared:jvmTest    # unit tests
./gradlew :engine:jacocoJvmReport               # coverage (XML + HTML)
```

SonarQube: **0 issues**, coverage ~37% (gate LeechText: new_coverage ≥ 10%). Scan local
(cần jacoco XML trước — chạy kèm jvmTest):

```bash
./gradlew :engine:jacocoJvmReport :app-shared:jacocoJvmReport
sonar-scanner -Dsonar.projectKey=leechtext -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.sources=engine/src,app-shared/src,desktop-app/src,android-app/src \
  -Dsonar.tests=engine/src/jvmTest,app-shared/src/jvmTest,engine/src/macosTest \
  -Dsonar.test.inclusions="**/*Test.kt" \
  -Dsonar.coverage.jacoco.xmlReportPaths="engine/build/reports/jacoco/jacocoJvmReport/jacocoJvmReport.xml,app-shared/build/reports/jacoco/jacocoJvmReport/jacocoJvmReport.xml" \
  -Dsonar.exclusions="**/build/**,**/generated/**"
```

## Đường dẫn phát triển

Xem `docs/project-roadmap.md` (lộ trình — bản hiện tại trong `docs/designs/kotlin-compose-rewrite-office-hours.md`). Build & release: `docs/deployment-guide.md`, `RELEASE-WORKFLOW.md`.

## Credits

- LongVD (Darkrai9x) — đồng tác giả bản Java gốc (`java-legacy` tag)

## License

[MIT](LICENSE)
