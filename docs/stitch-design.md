# Design System: LeechText — Paper & Teal

> **Adopted (2026-08-23):** Stitch đã khóa system thành "Teal Archivist" — primary `#006A63`, label font Public Sans, tonal palette M3 đầy đủ. Master screen: `projects/4250987222274687179/screens/3af848727dd64b9a91dc0a0478c97d32` (Thư viện — Tachidesk Light). File này là intent spec; khi khác biệt, theo Teal Archivist trên Stitch.

## 1. Visual Theme & Atmosphere

A bright daylight library interface: warm paper-white surfaces, crisp stone-gray
text, one deep teal accent doing all the pointing. The feel is a reading room
with morning light — book covers sit cleanly on white, download queues tick in
quiet mono type. The information architecture follows Tachiyomi (Mihon): a
uniform cover-grid library, per-source browsing, a manga-detail screen with
dense chapter list, and a downloads drawer — proven reader-app patterns,
restyled in paper and teal. Density is balanced (5/10). Motion is fluid but
weighty (5/10): spring physics, staggered chapter-list reveals, shimmer on
anything actively downloading. Light theme is the default and only theme.

## 2. Color Palette & Roles

- **Paper Canvas** (#F7FAF8) — App background, warm paper white. Never cold pure white as background.
- **Pure Surface** (#FFFFFF) — Cards, panels, app bars, bottom sheets, navigation bars.
- **Soft Fill** (#F1F4F3) — Hover fills, input fields, pressed states, progress-bar tracks, section label chips.
- **Stone Ink** (#181C1C) — Primary text, book titles, headlines. Never pure black.
- **Muted Stone** (#3E4947) — Secondary text, metadata, author names, timestamps.
- **Whisper Border** (rgba 24,28,28 8%) — 1px structural lines, list dividers, card outlines.
- **Deep Teal** (#006A63) — The single accent. Primary buttons (white text on teal), active tab icons and labels, focus rings, download progress fill, unread badges, active toggles. Saturation under 80%, no glow.
- **Teal Tint** (#CCFBF1) — Only for selected-row backgrounds and toggle tracks. Sparingly.
- **Done Moss** (#2F855A) — Semantic only: completed download check, "EPUB exported" state.
- **Alert Clay** (#C25450) — Semantic only: failed chapter, retry hint, destructive confirm.

Only one accent (teal). Green/red are status semantics, never decoration.
No purple, no neon, no gradients on text, no outer glows.

## 3. Typography Rules

- **Display: Space Grotesk** — Screen titles and book titles. Track-tight (letter-spacing -0.02em), weight 600–700, controlled scale. Hierarchy via weight and Stone-Ink-vs-Muted-Stone contrast, never via screaming size.
- **Body: Hanken Grotesk** — Descriptions, chapter titles, buttons, forms. Relaxed line-height 1.5, max 65 characters per line in reading contexts.
- **Mono: JetBrains Mono** — Chapter numbers, percentages, thread counts, URLs, file sizes, timestamps. All measurable numbers are mono.
- **Banned:** Inter, Roboto, system default fonts, all serifs (this is software UI, not editorial).

## 4. Component Stylings

- **Buttons:** Flat fills, no glow. Primary = teal fill with white text; secondary = ghost outline in Whisper Border with Stone Ink text. Pressed state translates -1px with spring return (stiffness 100, damping 20). Minimum 44px tap target.
- **Library grid (Tachiyomi rule):** Uniform cover-only tiles — every cover identical 2:3 ratio, identical 12px radius, no card chrome, no surface fill, no shadows. Title in body font 2 lines max directly under the cover, nothing else. Unread/pending-chapter count = small teal circle badge with white mono number pinned bottom-right ON the cover (e.g. 12). Optional thin 4px teal progress bar along the cover's bottom edge while downloading. Absolute uniformity is the whole point — one stray variant ruins the grid.
- **Chapter rows:** NEVER cards. Border-top Whisper-Border dividers, generous 12px vertical padding, mono chapter number left, title in body font, status glyph right (teal spinner-shimmer while downloading, moss check when done, clay dot when failed). Read chapters dim to Muted Stone; unread stay Stone Ink with a small teal dot.
- **Inputs:** Label above, error text below in Alert Clay. Focus ring in teal, 2px. URL paste field is the hero of the add-book flow — large, mono placeholder showing an example novel URL, Pure Surface fill with Whisper Border.
- **Progress:** Thin 4px bars in teal on Soft Fill track, mono percentage and chapters-done count beside. Active downloads shimmer subtly in an infinite loop.
- **Loaders:** Skeletons matching exact layout — Soft Fill cover rectangles with shimmer for grid, grey text lines for chapter lists. No circular spinners.
- **Empty states:** Composed illustration: one dimmed book cover outline with a teal URL cursor blinking into it — the gesture of pasting the first URL. Never just "No data".
- **Navigation (Tachiyomi IA):** Mobile = 4-tab bottom bar on Pure Surface: Thư viện / Cập nhật / Nguồn / Khác — active item teal icon + label, inactive Muted Stone, no indicator bars. Each tab has its own top app bar: title left, search and filter/sort icons right (icons, never a colored search field). "Thêm truyện" lives inside the Nguồn tab as an embedded URL field, not on the home screen.
- **URL add flow:** Inside Nguồn tab — a mono URL paste field labeled "Thêm từ URL" with teal "Thêm" button, plus the pinned-source list below.
- **Downloads drawer:** Not a tab. A bottom sheet overlay on Pure Surface summoned by a teal download-progress icon in the top bar: active queue rows (cover thumb, title, mono 412/1043 · 39%, thin teal bar), collapsed "Hoàn tất" section with moss check rows, "Tạm dừng tất cả" ghost button.

## 5. Layout Principles

- **Mobile (Tachiyomi structure):** Library tab = top bar + uniform cover grid (3 columns on 390px). Detail screen = Tachiyomi manga layout: cover thumb top-left with title/author/status to its right, teal action row below (Theo dõi, Tải tất cả, Đọc), then full-width chapter list — dense rows, mono chapter number, title, download glyph right, unread dot teal. Nguồn tab = URL field + pinned source rows (source icon, name, language tag in mono).
- **Desktop (Tachidesk-style):** Material navigation rail on the left on Pure Surface (Thư viện / Cập nhật / Nguồn / Cài đặt, icons + labels, active teal), content area max-width 1400px contained — library = 7-column uniform cover grid; downloads = collapsible right panel with the same queue rows as mobile's drawer. Panes separated by 1px Whisper Border, not cards-in-cards.
- Book detail follows Tachiyomi manga screen, never centered-hero: compact header block (cover + meta side by side), teal action row, then the chapter list dominates the screen.
- The generic "3 equal cards in a row" is banned — library grids are cover tiles, everything else uses dividers and whitespace.
- No overlapping elements, no absolute stacking, every element owns its spatial zone. Grid-based spacing (4px base unit: 4/8/12/16/24/32/48).
- Long chapter lists (1000+) virtualize visually: tight rows, no per-row shadows, sticky section letters.

## 6. Motion & Interaction

- Spring physics everywhere (stiffness 100, damping 20). No linear easing.
- Chapter lists and library grids mount with staggered cascade reveals (30–50ms per item).
- Perpetual micro-loops: shimmer on active download rows, blinking teal cursor in the empty URL field, gentle pulse on the queue badge count.
- Animate transform and opacity only. Progress bars grow via scaleX, never width reflow.
- Screen transitions: subtle 12px slide-up with fade, 240ms.

## 7. Responsive & Platform Rules

- One design system across mobile and desktop; components collapse, they do not restyle.
- Below 768px: all multi-column layouts collapse to single column; desktop right queue dock becomes a full-screen Queue tab.
- Touch targets minimum 44px on mobile; desktop rows compress to 36px with hover fills.
- Body text never below 14px. Titles scale via clamp, not fixed jumps.
- No horizontal scroll at any viewport.

## 8. Anti-Patterns (Banned)

- No emojis anywhere in UI.
- No Inter, Roboto, or generic system fonts.
- No serif fonts — this is a tool UI.
- No pure black (#000000) text or backgrounds.
- No purple, no neon accents, no outer glows, no gradient text.
- No dark theme remnants — backgrounds stay paper white.
- No 3-column equal card rows.
- No centered hero sections — detail screens are asymmetric.
- No overlapping elements or stacked absolute content.
- No generic placeholder names — use plausible Vietnamese light-novel titles (e.g. "Lâu Đài Bay Của Pháp Sư", "Kiếm Thánh Trở Lại") and real-feeling site names (truyenyy, wikidich, metruyenchu).
- No fake round numbers (99.99%, 50%); progress uses real chapter counts (128/1043).
- No AI copywriting clichés: "Elevate", "Seamless", "Unleash", "Next-Gen".
- No filler text: "Scroll to explore", swipe hints, bouncing chevrons.
- No broken image links — covers use picsum.photos seeds.
- No circular spinners for loading — skeletons only.

## 9. Settings Screen (Cài đặt)

Settings is a quiet reference screen — the opposite of the library. No cards
stacked on cards; hierarchy comes from section chips, dividers, and trailing
controls.

- **Top bar:** Pure Surface, "Cài đặt" in Space Grotesk 600, left-aligned. Nothing else in the bar.
- **Section labels:** JetBrains Mono 11px UPPERCASE in Muted Stone, wrapped in a Soft Fill chip (6px radius, 8×3px padding): KẾT NỐI, LƯU TRỮ, MẠNG, CẤU TRÚC LƯU, CÔNG CỤ, LỌC RÁC, QUYỀN RIÊNG TƯ.
- **Setting rows:** NEVER cards. Row = title (Hanken 14px Stone Ink) + caption (12px Muted Stone) on the left, control on the right, 12px vertical padding, border-top Whisper Border between groups. Tap target 44px.
- **Numeric settings:** compact mono input 120px wide, JetBrains Mono, right-aligned. On ≥600px viewports numbers pair into two columns; single column below.
- **Paths and URLs:** shown in JetBrains Mono, one line, ellipsized — never wrap.
- **Syntax templates (DropCaps/HTML/TXT/CSS):** a teal switch per row; the mono template editor only reveals when the switch is on (progressive disclosure — the default view is four clean switches).
- **Complex editors (trash rules, tool paths):** the only place a grouped Pure Surface panel with Whisper Border is allowed — they contain internal multi-field editing.
- **Destructive action:** "Khôi phục mặc định" is a ghost outline button with Alert Clay text, isolated at the bottom with 24px breathing room below.
