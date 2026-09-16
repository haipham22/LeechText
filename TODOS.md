# TODOS

## TODO: Android chapter storage — research trước P3
- **What:** Benchmark file-per-chapter (như bản Java: ChapExecute ghi file rời) vs single-store (SQLite/Room hoặc 1 zip per book) trên Android với ~2.000 chapter.
- **Why:** Nhiều file nhỏ trên Android internal storage làm sync/backup/quét thư mục chậm (confidence 6/10 — verify bằng số liệu).
- **Pros:** Quyết định có data; tránh refactor storage sau khi ship P3.
- **Cons:** Thêm 1 bước research trước P3.
- **Context:** Eng review 2026-08-23 performance note. Bản desktop giữ file-per-chapter (đã ổn). Chỉ đổi actual của Storage seam trên Android nếu benchmark nói rõ.
- **Depends on:** P1 (Storage seam tồn tại).

## TODO: Slim fat jar desktop — Playwright driver-bundle (193MB) + icons-extended (36MB)
- **What:** Cân nhắc exclude `com.microsoft.playwright:driver-bundle` khỏi fat jar (jar 281MB → ~90MB, deb 289MB → ~100MB); thay `material-icons-extended` bằng ~33 icon vector XML tự giữ (0.3MB).
- **Why:** 246/275MB runtime deps là 2 thứ này; code app chỉ ~2MB. Driver-bundle chứa Chromium/Firefox/WebKit driver cho cả 3 OS.
- **Tradeoff driver-bundle:** exclude → Engine API (browser automation) cần cài driver tay (env `PLAYWRIGHT_DRIVER_PATH` hoặc `playwright install`) — degrade UX cho user thường; giữ → chấp nhận nặng. Quyết định theo mức độ dùng Engine API thực tế.
- **Context:** Soi release v2.0.0 260916 — jar 281MB, deb 289MB.
