# TODOS

## TODO: Android chapter storage — research trước P3
- **What:** Benchmark file-per-chapter (như bản Java: ChapExecute ghi file rời) vs single-store (SQLite/Room hoặc 1 zip per book) trên Android với ~2.000 chapter.
- **Why:** Nhiều file nhỏ trên Android internal storage làm sync/backup/quét thư mục chậm (confidence 6/10 — verify bằng số liệu).
- **Pros:** Quyết định có data; tránh refactor storage sau khi ship P3.
- **Cons:** Thêm 1 bước research trước P3.
- **Context:** Eng review 2026-08-23 performance note. Bản desktop giữ file-per-chapter (đã ổn). Chỉ đổi actual của Storage seam trên Android nếu benchmark nói rõ.
- **Depends on:** P1 (Storage seam tồn tại).
