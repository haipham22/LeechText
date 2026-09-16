---
name: dogfood-squad
description: Spawn đội agent dogfood/QA test app qua mobile hub (Android/iOS) và desktop hub (Xvfb+xdotool Linux) — người khó tính, người mới, breaker (phá app), fix agent. Chạy song song hoặc tuần tự. Dùng khi user nói "spawn agent test/đogfood/phá app", "kiểm thử app", "dogfood squad".
---

# Dogfood Squad — đội agent kiểm thử app qua mobile + desktop hub

Orchestration pattern: main agent chuẩn bị hub + app → spawn các agent kiểm thử
(background) → tổng hợp feedback → spawn fix agent → verify → Sonar.

Hai lane: **mobile** (Android/iOS qua mobile-mcp) và **desktop** (app Linux qua
Xvfb + xdotool). Cùng một CLI `tools/dogfood.py` — tool `mobile_*` đi gateway
mobile, tool `desktop_*` đi gateway desktop. Có thể chạy song song 2 lane.

## 0. Kiến trúc hub (BẮT BUỘC — mọi thao tác thiết bị đi qua đây)

- Gateway: `tools/mobile_gateway.py` — spawn mobile-mcp (stdio) + tool bổ sung.
  `.mcp.json` đã đăng ký stdio — KHÔNG cần HTTP server.
- Helper cho agent: `python3 tools/dogfood.py <tool> '<json>'`
- **TUYỆT ĐỐI CẤM** gọi tool MCP mobile trực tiếp (mcp__mobile-mcp__*) từ main
  hay từ subagent. Thiếu tool gì → **viết thêm tool vào gateway** rồi dùng hub.
- Ảnh/luồng minh chứng tự lưu `.dogfood/` (đã git-ignore) — chỉ Read ảnh khi
  cần phân tích (tiết kiệm token).

### Tool của hub
Upstream: `@mobilenext/mobile-mcp` (mobile-next/mobile-mcp) — **Android + iOS sim cùng bộ tool**.
LƯU Ý: npm package `mobile-mcp` (không có @) là package cũ runablehq — Android-only, KHÔNG dùng.

| Tool | Dùng |
|---|---|
| mobile_list_available_devices | liệt kê thiết bị → lấy `id` (UDID/serial) truyền vào `device` |
| mobile_take_screenshot / mobile_save_screenshot / mobile_list_elements_on_screen | màn hình + cây accessibility (tọa độ point, iOS sim 402x874) |
| mobile_click_on_screen_at_coordinates / mobile_double_tap / mobile_long_press | tap theo tọa độ |
| mobile_swipe_on_screen / mobile_type_keys / mobile_press_button / mobile_open_url | input |
| mobile_launch_app / mobile_terminate_app / mobile_list_apps / mobile_get_screen_size | app |
| mobile_set_orientation / mobile_start_screen_recording / mobile_stop_screen_recording | phụ |
| mobile_list_crashes / mobile_get_crash | crash log iOS |
| mobile_scenario | chạy cả chuỗi steps 1 lệnh: `{"label":"...","steps":[{"tool":"mobile_click_on_screen_at_coordinates","device":"<id>","x":1,"y":2},{"sleep":1000},{"tool":"mobile_take_screenshot","device":"<id>"}]}` — ảnh + log + logcat lưu `.dogfood/` |
| android_wake / android_wifi / android_rotate / android_force_stop / android_logcat / adb_shell | system-level wrap adb (không dùng adb Bash trực tiếp) |

Mẹo tiết kiệm token: gom thao tác vào `mobile_scenario`; ảnh chỉ Read khi cần
phân tích; `mobile_list_elements_on_screen` để TÌM nút (có sẵn tọa độ), không tap mù.

## 0b. Desktop hub (Linux — Xvfb + xdotool)

Gateway: `tools/desktop_gateway.py` — quản lý Xvfb (`:99`) + process app,
chụp `xwd→ffmpeg` (crop đúng khung cửa sổ), click/typing `xdotool`.
State (pid Xvfb/app) lưu `/tmp/desktop-gw-state.json` — mỗi lệnh CLI là process
mới nhưng nối lại được app đang chạy. Cần: `Xvfb`, `xdotool`, `ffmpeg`
(`sudo apt-get install -y xvfb xdotool ffmpeg`; check: `--check`).

Tại sao Xvfb: GNOME Wayland chặn screenshot của process ngoài + AT-SPI tree
của Compose Desktop chỉ có title bar → không đọc/điều khiển qua accessibility
được. Xvfb là X11 thuần — chụp + click đều được, không cần quyền.

| Tool | Dùng |
|---|---|
| desktop_start | start Xvfb + chạy app: `{"app": "./gradlew :desktop-app:run"}` (chờ cửa sổ tối đa 180s) |
| desktop_screenshot | chụp cửa sổ app → ảnh lưu `.dogfood/<label>-stepNN-<ts>.png` |
| desktop_click | click tọa độ **cục bộ cửa sổ** (0,0 = góc trái trên): `{"x":245,"y":385}` |
| desktop_type / desktop_key | gõ text (clear:true = xóa trước) / phím: `{"text":"..."} / {"key":"Return"}` |
| desktop_scroll | cuộn notch: `{"y":5}` (xuống), `{"x":3}` (phải) |
| desktop_windows | liệt kê cửa sổ trong Xvfb (debug, tìm title) |
| desktop_app_log | đọc log app (stdout+stderr): `{"grep":"Exception","lines":200}` |
| desktop_scenario | chuỗi steps 1 lệnh — `{"label":"ten","steps":[{"tool":"desktop_click","x":..,"y":..},{"sleep":1000},{"screenshot":true}]}`; cuối phiên tự dump app log + log bước vào `.dogfood/` |
| desktop_stop | tắt app + Xvfb |

Mẹo: **desktop không có accessibility tree** (Compose Desktop expose rất nông)
→ bắt buộc đo tọa độ từ screenshot: chụp → Read ảnh → đọc vị trí nút → click →
chụp lại xác nhận. Nhóm bước lặp vào `desktop_scenario`.

## 0c. Gọi từ CLI (cả 2 lane dùng chung)

```bash
python3 tools/dogfood.py desktop_start '{"app":"./gradlew :desktop-app:run"}'   # tự routing desktop_*
python3 tools/dogfood.py desktop_screenshot '{"label":"home"}'
python3 tools/dogfood.py mobile_list_available_devices                          # mobile_* như cũ
```
(`tools/dogfood.py` tự chọn gateway theo tiền tố tool.)

## 1. Chuẩn bị trước khi spawn (main agent tự làm)

1. Build + cài app mới nhất lên thiết bị mục tiêu:
   - Android: `./gradlew :android-app:assembleDebug` → `adb -s <serial> install -r <apk>`
   - iOS sim: xcodebuild → `xcrun simctl install`
2. Kiểm tra hub sống: `python3 tools/dogfood.py mobile_list_available_devices`
3. Ghi vào prompt mỗi agent: `device id` (từ list trên), kích thước màn hình
   (iOS sim tính theo **point** từ `mobile_get_screen_size`, Android theo px),
   tên package/bundle app.
4. Màn hình tắt sau 30s (Android) — mỗi agent phải biết: nếu screenshot đen →
   `android_wake` rồi làm tiếp.

## 2. Lên lịch chạy

**Quy tắc vàng: 1 thiết bị = 1 agent tại 1 thời điểm.** Nhiều agent cùng device
sẽ tap giành nhau. Song song chỉ khi mỗi agent một thiết bị (điện thoại + iOS sim).

- **Song song:** spawn nhiều Agent (subagent_type=general-purpose,
  run_in_background=true) trong CÙNG 1 message — mỗi agent 1 thiết bị.
  Ưu tiênDifferentDevice: phone → breaker/stress (hành vi nặng); simulator →
  newbie/picky (tap nhẹ nhàng).
- **Tuần tự:** test agents (picky → newbie → breaker) → main tổng hợp →
  spawn fix agent → fix agent verify → (tùy chọn) spawn lại 1 test agent chạy
  regression trên các phần đã sửa.
- Agent đang chạy có thể được nhắn thêm tin bằng SendMessage (id agent trả về
  khi spawn) — dùng để bổ sung ràng buộc/thông báo thay đổi môi trường.

## 3. Các vai trò + prompt template

Khung prompt chung (điền biến `{DEVICE}`, `{SIZE}`, `{PACKAGE}`, `{REPORT}`, checklist):

```
Bạn vào vai <PERSONA>. Nhiệm vụ: <mục tiêu>. KHÔNG sửa code, KHÔNG gỡ plugin/nguồn gốc.

## Thiết bị
- Thiết bị: Android thật `<SERIAL>` (tọa độ px) HOẶC iOS sim `<UDID>`
  (tọa độ **point** — xem `mobile_get_screen_size`). Device id truyền vào
  `"device"` MỌI tool mobile_*.
- App `<PACKAGE>` đã cài. Có sẵn: <mô tả dữ liệu test>.
- Màn tắt sau 30s (Android) → screenshot đen thì gọi android_wake rồi chờ 1s.
- Có thể có người khác chạm máy: trước mỗi thao tác kiểm tra app còn foreground
  (list_elements_on_screen / launch_app lại) rồi làm tiếp.

## Cách điều khiển (CHỈ qua hub — cấm MCP trực tiếp)
python3 tools/dogfood.py mobile_list_elements_on_screen '{"device":"<id>"}'   # tìm nút + tọa độ
python3 tools/dogfood.py mobile_click_on_screen_at_coordinates '{"device":"<id>","x":..,"y":..}'
python3 tools/dogfood.py mobile_scenario '{"label":"ten","steps":[{"tool":"mobile_click_on_screen_at_coordinates","device":"<id>","x":..,"y":..},{"sleep":1000},{"tool":"mobile_take_screenshot","device":"<id>"}]}'
python3 tools/dogfood.py android_wake / android_force_stop / android_logcat ...   # Android
Ảnh minh chứng tự lưu .dogfood/<label>-*.png — Read file .png khi cần xem.

## Checklist
<checklist từng màn/tính năng>

## Đầu ra (BẮT BUỘC)
1. Báo cáo `plans/reports/<tên-file>.md`: ## Tóm tắt / ## Phát hiện ([CAO/TRUNG BÌNH/THẤP] + repro + ảnh) / ## Điểm tốt / ## Ưu tiên sửa.
2. Message cuối: TOP phát hiện (≤12 mục, gọn).
```

### Persona 1 — `picky` (người dùng khó tính)
Power user lâu năm, ghét UI vụng về. Checklist: phủ HẾT tính năng (mỗi tab,
mỗi dialog, từng setting, đa ngôn ngữ VI↔EN), đánh giá theo chuẩn app mẫu
(Kindle/Mihon). Kèm mức độ [CAO/TRUNG BÌNH/THẤP] + repro + ảnh. Có quyền thêm
sách test; không gỡ nguồn gốc.

### Persona 2 — `newbie` (người mới 0%)
Chưa biết gì: đi tự nhiên từ màn đầu tới chỗ đọc được chương, thử icon/nhãn.
Ghi lại MỖI lúc đứng hình >5s, phải đoán mò, icon không tự giải thích, jargon.
Chấm điểm /10 từng bước. Đầu ra thêm mục "Icon/nhãn khó hiểu + đề xuất thay thế".

### Persona 3 — `breaker` (người phá app)
Stress test: spam tap (10 lần/s) các nút chuyển chương/bookmark; xoay màn liên
tục trong reader; tắt wifi giữa lúc tải; force-stop giữa thao tác rồi mở lại;
emoji + ký tự đặc biệt vào ô search/URL; đổi ngôn ngữ 3 lần liên tục nhanh;
tải range rồi force-stop. Mục tiêu: crash / ANR / mất dữ liệu / state sai.
Sau phiên: khôi phục (wifi bật lại, app về Thư viện, dọn sách/rule test).
Tool hệ thống: android_wake/wifi/rotate/force_stop/logcat/adb_shell.
Bắt log crash: `android_logcat '{"grep":"FATAL","lines":200}'`.

### Persona 4 — `fix` (kỹ sư sửa bug)
Input: bản triage từ các báo cáo test (main tổng hợp). Được sửa code + build +
cài APK + verify qua hub. Ràng buộc: thay đổi tối thiểu, Kotlin idiomatic, chạy
xanh `:app-shared:compileKotlinIosSimulatorArm64` + `:android-app:assembleDebug`,
tự verify luồng liên quan trên thiết bị, báo cáo từng việc đã-làm/không-làm + lý do.

## 4. Tổng hợp + vòng lặp

1. Thu các message/báo cáo của test agents (`plans/reports/*.md` + `.dogfood/`).
2. Triage: gộp trùng, đánh độ ưu tiên, tách "đã fix sẵn" để agent fix không làm trùng.
3. Spawn `fix` agent với bản triage + yêu cầu riêng của owner (nếu có).
4. Fix xong → main rà diff → chạy regression (spawn lại 1 test agent) → Sonar
   scan nếu owner yêu cầu:
   ```bash
   docker run --rm -e SONAR_HOST_URL=<url> -e SONAR_TOKEN=<token> \
     -v "$(pwd):/usr/src" sonarsource/sonar-scanner-cli:latest
   ```

## Tham số khi gọi skill (ví dụ)
- "/dogfood-squad" → mặc định: picky + newbie song song (phone + sim), rồi fix.
- "/dogfood-squad breaker" → chỉ breaker.
- "/dogfood-squad picky, breaker tuần tự" → lần lượt trên cùng thiết bị.
- Thêm yêu cầu riêng của owner ghi ở cuối brief cho fix agent.
