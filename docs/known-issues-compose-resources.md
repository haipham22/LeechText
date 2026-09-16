# Known issue: Res.string accessors không compile cho file Kotlin mới

**Verdict CI (PR #6, 2026-08-26, đã đóng):** máy sạch CI **đỏ** với file mới
reference thậm chí key CŨ (`sources_no_match`) — cấu hình bằng hệt main đang
xanh. Đã thử compose 1.8.1 / 1.8.2 / 1.9.3 — như nhau.

**Bối cảnh:** bug có từ N5 i18n (535d8c1 dựng Compose Resources + custom
`sharedMain` intermediate). Các commit N5 chưa từng chạy CI riêng (không có
runs trong khung đó). CI main vẫn xanh vì các file dùng Res.string đã được
compile từ trước; gradle cache trong workflow (từ c007fc9) che thêm.

**Symptom:** file Kotlin MỚI/sửa reference `Res.string.<bất kỳ>` →
`Unresolved reference`, dù accessor files có đầy đủ key trên disk và nằm
trong compiler args (-Xfragment-sources). Thêm string mới vào strings.xml
cũng vậy. Compile pass chỉ khi không file nào dirty.

**Workaround:** string mới → `UiText.Raw("…")` (xem LibraryScreen.ensureToc).
Không thêm file mới dùng Res.string cho tới khi fix.

**Đã loại trừ:** task ordering (dependsOn tay — giữ trong build.gradle.kts,
semantics đúng), config/build cache, daemon IC, clean build, compose
1.8.2/1.9.3, srcDir tĩnh.

**Hướng tiếp theo (ưu tiên):**
1. Repro tối thiểu: KMP + custom intermediate source set + compose resources
   → nếu repro được → report JetBrains compose-resources.
2. Bump Kotlin 2.1.21 → 2.2.x + Compose 1.9+ đồng loạt (P5.0 từng thử 2.4.10
   + Compose 1.12 rồi revert 6303575 "chưa owner duyệt" — hỏi owner duyệt lại,
   có thể fix sẵn ở đó).
3. Hoặc bỏ intermediate: gộp sharedMain về commonMain (mất tách biệt nhưng
   là cấu trúc chuẩn mà compose-resources test hàng ngày).

Related: engine/generateAppInfo pattern (Kotlin code gen + srcDir tĩnh) hoạt
động bình thường — chỉ compose-resources accessors gãy.
