# Release Workflow — LeechText

> Quy trình release hiện tại là **thủ công** — chưa có CI phát hành theo tag. Bản Java cũ (DMG/DEB/checksums tự động) đã xóa, xem tag `java-legacy`.

## Release thủ công

1. **Bump version** — sửa `versionCode` / `versionName` trong `android-app/build.gradle.kts` (desktop jar không gắn version vào tên file).
2. **Build artifacts**:

   ```bash
   ./gradlew :desktop-app:fatJar -x test     # desktop-app/build/libs/leechtext-desktop.jar
   ./gradlew :android-app:assembleRelease    # android-app/build/outputs/apk/release/
   ```

3. **Test trước khi ship**: `./gradlew :engine:jvmTest :app-shared:jvmTest` (CI cũng chạy phần engine).
4. **Tạo GitHub Release thủ công**: tag → upload `leechtext-desktop.jar` + APK. Commit về `main`.

## CI hiện có (không phát hành)

- `build-multi-platform.yml` — push main/PR: fat jar + engine tests trên macOS, upload artifact `leechtext-desktop` (chỉ tải từ trang Actions, không publish).
- `code-quality.yml`, `documentation.yml` — test + validate docs.

## Đã biết thiếu (backlog)

- Workflow trigger theo tag `v*`: build + tạo GitHub Release + checksum tự động.
- Signing key riêng cho Android release (hiện ký debug key).
- Native packaging desktop (jpackage → DMG/DEB/exe) — giao thác có chủ đích.
- Cập nhật `docs/project-changelog.md` mỗi release.
