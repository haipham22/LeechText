package dev.haipham22.leechtext.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** setting.json (port từ models/Settings.java). SerializedName là format compat — không đổi. */
@Serializable
data class Settings(
    @SerialName("connection") var connection: ConnectionSettings? = null,
    @SerialName("style") var style: StyleSettings? = null,
    @SerialName("other") var other: OtherSettings? = null,
) {
    @Serializable
    data class ConnectionSettings(
        @SerialName("num_conn") var numConn: Int = 5,
        @SerialName("re_conn") var reConn: Int = 3,
        @SerialName("delay") var delay: Int = 10,
        @SerialName("time_out") var timeOut: Int = 30000,
        @SerialName("user_agent") var userAgent: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                " Chrome/54.0.2840.71 Safari/537.36",
    )

    @Serializable
    data class StyleSettings(
        @SerialName("dropcaps") var dropcaps: StyleItem? = null,
        @SerialName("html") var html: StyleItem? = null,
        @SerialName("txt") var txt: StyleItem? = null,
        @SerialName("css") var css: StyleItem? = null,
    )

    @Serializable
    data class StyleItem(
        @SerialName("checked") var checked: Boolean = false,
        @SerialName("value") var value: String = "",
    )

    @Serializable
    data class OtherSettings(
        @SerialName("workspace") var workspace: String = "",
        @SerialName("calibre") var calibre: String = "",
        @SerialName("kindlegen") var kindlegen: String = "",
        @SerialName("theme_color") var themeColor: String = "#263238",
        @SerialName("trash") var trash: List<Trash>? = null,
        // ponytail: consenti Sentry tạm, opt-in (mặc định KHÔNG gửi) — thay bằng onboarding
        @SerialName("crash_report") var crashReport: Boolean = false,
        @SerialName("pinned_sources") var pinnedSources: List<String>? = null,
        @SerialName("reader_font_size") var readerFontSize: Int = 16,
        /** Font đọc: "" (mặc định) | serif | sans | mono — generic FontFamily, không file font. */
        @SerialName("reader_font") var readerFont: String = "",
        /** Nền đọc: light | sepia | dark — áp reader body, header theo nền. */
        @SerialName("reader_bg") var readerBg: String = "light",
        @SerialName("reader_fg") var readerFg: String = "",
        @SerialName("reader_line_height") var readerLineHeight: Float = 1.6f,
        /** Tốc độ auto-scroll reader 1..10. */
        @SerialName("reader_auto_scroll_speed") var readerAutoScrollSpeed: Int = 3,
        /** Chế độ đọc: vertical (cuộn dọc) | paged (lật trang ngang). */
        @SerialName("reader_scroll_mode") var readerScrollMode: String = "vertical",
        @SerialName("debug_log") var debugLog: Boolean = false,
        @SerialName("language") var language: String = "vi",
        /** Theme app: system | light | dark — override Dark Mode hệ thống. */
        @SerialName("app_theme") var appTheme: String = "system",
        /** Path zip plugin mã hóa đã confirm install fail — ẩn khỏi NÂNG CẤP (260906). */
        @SerialName("encrypted_paths") var encryptedPaths: List<String>? = null,
        /** Tên nguồn chết (browse rỗng) — ẩn khỏi tab Nguồn, hiện section Thử lại (260906). */
        @SerialName("broken_sources") var brokenSources: List<String>? = null,
    )
}
