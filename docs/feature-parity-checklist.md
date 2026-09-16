# Feature Parity Checklist — LeechText Kotlin Rewrite

Gate P2 (design doc): xoá `src/main/java` khi **MUST-tier 100%** (mục "degrade có chủ đích" vẫn tính là đạt). Claude draft — owner duyệt tier trước khi P2 bắt đầu.

> Đã qua gate: `src/main/java` đã xoá, bản Java ở tag `java-legacy`.

Legend: ☐ chưa làm | ✅ xong | ⏸ degrade có chủ đích (ghi lý do)

## MUST — không có thì không phải LeechText

| # | Feature | Tier | Trạng thái | Ghi chú |
|---|---------|------|-----------|---------|
| 1 | Add sách: URL → match plugin (regex) → fetch info (tên/tác giả/cover/mô tả) | MUST | ✅ | AddBookScreen — owner verify sách thật 2026-08-23 |
| 2 | Fetch TOC (danh sách chương), chọn range `1-N` | MUST | ✅ | parseRange port AddDialog.parseListChap |
| 3 | Download chapter text, MAX_CONN song song, progress | MUST | ✅ | get/BookDownload.kt + UI progress; 2 sách tải thật qua app |
| 4 | **Resume: chapter đã có file → skip, chỉ tải thiếu** (fix legacy 2026-08-23) | MUST | ✅ | BookDownloadTest (engine jvmTest) |
| 5 | **Truyện đang ra: re-open → fetch TOC mới → chỉ tải chương mới** | MUST | ✅ | mergeFetchedChapters khớp URL giữ id cũ; nút "Kiểm tra chương mới" |
| 6 | Library sách: lưu/mở lại sách (properties.json) | MUST | ✅ | LibraryScreen scan output/ |
| 7 | Export EPUB (có cover, TOC) | MUST | ✅ | nút EPUB trong Library; smoke thật → out/*.epub |
| 8 | Export TXT | MUST | ✅ | nút TXT (gộp 1 file out/text.txt) |
| 9 | Plugin manager: cài plugin từ file/repository, load khi khởi động, match theo regex | MUST | ✅ | PluginScreen + auto-install theo URL (findAndInstallByUrl) |
| 10 | Data dir ổn định `~/.leechtext` mọi launch mode | MUST | ✅ | EnginePaths; data cũ dùng chung với bản Java |
| 11 | Format compat: đọc setting.json/properties.json/.plugin của bản cũ | MUST | ✅ | FormatCompatTest (engine jvmTest) |
| 12 | Sandboxed JS execution (JsSandbox + security validators) | MUST | ✅ | slice + 22 test |
| 13 | **Queue tải đa sách** + tiến trình từng sách (bắt thiếu P0 — legacy DownloadUI) | MUST | ✅ | DownloadQueueState.kt (enqueue/cancel/retry/pump) + DownloadQueuePanel UI |
| 14 | **Multi-URL**: thêm nhiều URL một lúc (legacy MultiURL) | MUST | ✅ | DownloadQueueState.enqueue(urls: List\<String\>) |
| 15 | **Tải ảnh chương ảnh** (legacy Config.downloadImg) | MUST | ✅ | get/ChapterImages.kt, wired trong BookDownload |
| 16 | **Retry chương lỗi** không fetch lại chương đã có (legacy Config.downloadChap) | MUST | ☐ | re-run downloadChapters — resume ăn sẵn |
| 17 | **Quản lý repo plugin**: thêm/xóa repo, persist repository.json (legacy RepositoryUI) | MUST | ✅ | RepositoryManager write tools/repository.json + RepositoryManagerTest |

### MUST-P3 (Android cụ thể)
| # | Feature | Trạng thái | Ghi chú |
|---|---------|-----------|---------|
| A1 | Download nền: foreground service + notifications | ✅ | androidMain DownloadService.kt — startForegroundService + notification tiến trình |
| A2 | Sống qua doze (download dài không bị hệ thống giết) | ✅ | cùng foreground service |

## NICE — có thì tốt, thiếu vẫn ship P2
| # | Feature | Trạng thái | Ghi chú |
|---|---------|-----------|---------|
| N1 | Pause/resume/cancel download trong phiên | ✅ | Queue ⏸/▶ giữ item, resume tải chương thiếu (2026-08-26) |
| N2 | Xem DS chương + chọn chương lẻ | ✅ | ChapterPickDialog: checkbox + search + range; Android giữ subset qua Intent extra (2026-08-26) |
| N3 | Plugin repository: duyệt + update plugin (PluginUpdate) | ◐ | Duyệt kho ✅, checkUpdate chạy nền; thiếu UI nút update từng plugin |
| N4 | Check app update (GH gradle.properties) + dialog link release | ✅ | AppInfo sinh từ gradle.properties + isNewer per-segment + dialog (2026-08-26) |
| N5 | i18n vi/en | ✅ | Compose Resources 266 key vi/en, live switch — Strings.kt đã xóa (2026-08-26) |
| N6 | Theme màu (theme_color) | ☐ | |
| N7 | Cookies + proxy setting | ☐ | |
| N8 | Trash (xóa chương rác theo regex rule) | ✅ | Reader áp applyTrashRules từ setting trash[] |
| N9 | Toast/notification trong app | ◐ | Inline message UiText có; chưa có toast/snackbar |
| N10 | Cover hiển thị trong library | ✅ | BookCoverTile + CoverImage trong LibraryScreen |
| N11 | Merge sách (MergeUI nếu có ở legacy) | ☐ | check tồn tại |
| N12 | Check for update plugin của truyện đang theo dõi (auto-poll) | ☐ | ý tưởng mới — không có ở legacy |
| N13 | Plugin config (vBook extension-api): inject constants + localConfig API + UI cấu hình | ☐ | chỉ wikidich (thread_num/delay map sẵn) + hako (encrypted) dùng — không blocker |

## CUT — bỏ có chủ đích
| # | Feature | Lý do cắt |
|---|---------|-----------|
| C1 | Plugin editor (rsyntaxtextarea) | owner cắt 2026-08-23; thay = plugin forge (backlog) |
| C2 | Swing material UI port | rewrite bằng Compose — không port giao diện cũ |
| C3 | Micrometer metrics | chưa từng dùng — đã drop |
| C4 | Forum-mode download (PageExecute path) | legacy đã comment-out |

## Desktop-only / degrade có chủ đích
| # | Feature | Nơi hoạt động | Lý do |
|---|---------|--------------|-------|
| D1 | Browser API (Engine.newBrowser — Selenium) | desktop JVM | WebDriver không chạy mobile/wasm; lỗi rõ ràng + test |
| D2 | wasm preview (P4) | KHÔNG platform — CUT | Screens + ViewModels ở jvmMain gọi engine trực tiếp (~6.7k LOC) — migrate chỉ để QA UI không đáng; QA chuyển per-tab screenshot smoke desktop (SliceAppScreenshotTest, CI). ksoup gate PASS nên seam HTML không phải blocker nếu quay lại sau. Quyết định 2026-08-25. |
