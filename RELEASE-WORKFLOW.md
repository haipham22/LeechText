# Release Workflow — LeechText

> Hoàn toàn tự động theo tag. Bản Java cũ (release thủ công) xem tag `java-legacy`.

## Mô hình branch

| Branch | Vai trò | Version format | Release |
|---|---|---|---|
| `dev` (default) | development, beta | `x.y.z-rcN` / `x.y.z-betaN` | GitHub **prerelease** |
| `main` | ổn định | `x.y.z` | GitHub release **Latest** |

## Quy trình

1. **Dev**: merge/bump `app.version=2.0.1-rc1` trong `gradle.properties` → push `dev`
2. **Auto Release** (tự): đọc `app.version` → tag `v2.0.1-rc1` (nếu chưa có)
   - Guard: `main` chỉ tag bản **không hậu tố**; `dev` chỉ tag bản **có hậu tố** — bump sai branch sẽ bị skip với log giải thích
3. **Build Desktop** (tự, qua `workflow_run`): test → jar / apk / ipa / deb / dmg / msi → GitHub Release
   - Tag có `-` → `prerelease: true` (không chiếm chỗ Latest); bản final → Latest
4. **Ship final**: merge `dev` → `main` + bump `app.version=2.0.1` (bỏ hậu tố) → push `main` → release Latest

## Artifacts mỗi release

`jar` (desktop chạy mọi OS, Java 17+) · `deb` / `dmg` / `msi` (installer, packageVersion tự tách hậu tố rc) · `apk` (Android, ký debug-key) · `ipa` (iOS unsigned, tự ký qua Sideloadly/AltStore)

## CI khác

- `code-quality.yml` — lint (ktlint + detekt) + unit tests, chạy trên push `main`/`dev` + PR
- `documentation.yml` — validate docs khi đổi `docs/**` / `README.md`

## Đã biết thiếu (backlog)

- Signing key riêng cho Android release (hiện ký debug key — mỗi build key khác nhau, cài bản mới phải gỡ bản cũ)
- iOS signed IPA / TestFlight (cần Apple Developer account)
- Xem thêm `TODOS.md` (slim fat jar: Playwright driver-bundle 193MB + icons-extended 36MB)
