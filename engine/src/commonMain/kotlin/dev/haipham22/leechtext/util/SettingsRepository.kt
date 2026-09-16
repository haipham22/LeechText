package dev.haipham22.leechtext.util

import dev.haipham22.leechtext.models.Settings
import dev.haipham22.leechtext.models.Trash
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.concurrent.Volatile

/**
 * Cài đặt engine immutable (port từ util/SettingUtils.java — bỏ static mutable god-object).
 * `checked` flags giữ để round-trip setting.json đúng format bản gốc.
 */
data class AppSettings(
    val maxConn: Int = 5,
    val reConn: Int = 3,
    val delay: Int = 10,
    val timeout: Int = 90000,
    val userAgent: String = DEFAULT_USER_AGENT,
    val dropcapsEnabled: Boolean = true,
    val dropSyntax: String = "",
    val htmlChecked: Boolean = true,
    val htmlSyntax: String = "",
    val txtChecked: Boolean = true,
    val txtSyntax: String = "",
    val cssChecked: Boolean = true,
    val cssSyntax: String = "",
    val trash: List<Trash> = emptyList(),
    // ponytail: consenti Sentry tạm, opt-in (mặc định KHÔNG gửi) — thay bằng onboarding
    val crashReportEnabled: Boolean = false,
    val pinnedSources: List<String> = emptyList(),
    val readerFontSize: Int = 16,
    val readerFont: String = "",
    val readerBg: String = "light",
    val readerFg: String = "",
    val readerLineHeight: Float = 1.6f,
    val readerAutoScrollSpeed: Int = 3,
    val readerScrollMode: String = "vertical",
    val debugLog: Boolean = false,
    val language: String = "vi",
    val appTheme: String = "system",
    // Path zip plugin mã hóa đã xác nhận install fail (260906) — persist qua phiên
    val encryptedPluginPaths: List<String> = emptyList(),
    // Tên nguồn chết browse rỗng (260906) — persist qua phiên
    val brokenSources: List<String> = emptyList(),
    val workPath: String = "",
    val calibre: String = "",
    val kindlegen: String = "",
    val themeColor: String = "#263238",
) {
    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 26_0 like Mac OS X) AppleWebKit/605.1.15" +
                " (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1"
    }
}

/**
 * Load/save setting.json (port từ util/SettingUtils.java).
 * Format file giữ nguyên bản gốc: ~/.leechtext/tools/setting.json, Gson pretty-print,
 * defaults từ resource /dark/leech/res/setting.json.
 */
object SettingsRepository {

    private val fs: FileSystem get() = FileSystem.SYSTEM

    private fun settingFile(): Path = EnginePaths.dataDir / "tools" / "setting.json"

    /** Cài đặt mặc định từ resource; fallback hardcode nếu resource thiếu. */
    fun defaults(): AppSettings = try {
        fromModel(LeechJsonPretty.decodeFromString<Settings>(readResource("/dark/leech/res/setting.json")), isDefault = true)
    } catch (e: Exception) {
        hardcodedDefaults()
    }

    /** Cache load theo mtime — mỗi call đọc resource + setting.json + parse 2 lần
     *  Gson, gọi nhiều nơi trên main thread (property initializer UI). Stat mtime
     *  rẻ hơn đọc+parse; file đổi ngoài process (test, đổi dataDir) vẫn bắt được. */
    // ponytail: cache key (mtime, size) — mtime đơn thuần va chạm khi 2 write cùng
    // millisecond (test chạy nhanh) → stale cache; thêm size là đủ, inode đắt hơn
    @Volatile private var cache: Pair<Pair<Long, Long>, AppSettings>? = null // (mtime, size) → settings

    /** Defaults + overlay từ ~/.leechtext/tools/setting.json (nếu có, parse được). */
    fun load(): AppSettings {
        val meta = fs.metadataOrNull(settingFile())
        val mtime = meta?.lastModifiedAtMillis ?: -1L
        val size = meta?.size ?: -1L
        val key = mtime to size
        cache?.let { (k, s) -> if (k == key) return s }
        return loadFromDisk().also { cache = key to it }
    }

    private fun loadFromDisk(): AppSettings {
        val base = defaults()
        val json = settingFile().readTextOrNull() ?: return base
        return try {
            fromModel(LeechJsonPretty.decodeFromString<Settings>(json), isDefault = false, base = base)
        } catch (e: Exception) {
            base // fallback default nếu JSON hỏng
        }
    }

    /** Ghi setting.json theo format bản gốc. */
    fun save(settings: AppSettings) {
        val model =
            Settings().apply {
                connection =
                    Settings.ConnectionSettings().apply {
                        numConn = settings.maxConn
                        reConn = settings.reConn
                        timeOut = settings.timeout
                        delay = settings.delay
                        userAgent = settings.userAgent
                    }
                style =
                    Settings.StyleSettings().apply {
                        dropcaps = item(settings.dropcapsEnabled, settings.dropSyntax)
                        html = item(settings.htmlChecked, settings.htmlSyntax)
                        txt = item(settings.txtChecked, settings.txtSyntax)
                        css = item(settings.cssChecked, settings.cssSyntax)
                    }
                other =
                    Settings.OtherSettings().apply {
                        workspace = settings.workPath
                        calibre = settings.calibre
                        kindlegen = settings.kindlegen
                        themeColor = settings.themeColor
                        trash = settings.trash
                        crashReport = settings.crashReportEnabled
                        pinnedSources = settings.pinnedSources
                        readerFontSize = settings.readerFontSize
                        readerFont = settings.readerFont
                        readerBg = settings.readerBg
                        readerFg = settings.readerFg
                        readerLineHeight = settings.readerLineHeight
                        readerAutoScrollSpeed = settings.readerAutoScrollSpeed
                        readerScrollMode = settings.readerScrollMode
                        debugLog = settings.debugLog
                        language = settings.language
                        appTheme = settings.appTheme
                        encryptedPaths = settings.encryptedPluginPaths
                        brokenSources = settings.brokenSources
                    }
            }
        LeechJsonPretty.encodeToString(model).writeTo(settingFile().toString(), log = dev.haipham22.leechtext.log.platformEngineLogger())
        cache = null // invalidate — load() kế đọc lại từ disk
    }

    private fun item(
        checked: Boolean,
        value: String,
    ) = Settings.StyleItem().apply {
        this.checked = checked
        this.value = value
    }

    /** Syntax áp cho style item: được check (hoặc là default) thì lấy value, không thì giữ current.
     * value RỖNG không được đè template — setting.json cũ ghi `{checked:true, value:""}` từng
     * xóa trắng htmlSyntax → export EPUB ra chương 0 byte (bug Android 260906). */
    private fun effectiveSyntax(
        checked: Boolean,
        value: String,
        current: String,
        isDefault: Boolean,
    ) = if ((checked || isDefault) && value.isNotBlank()) value else current

    /**
     * Map model → AppSettings với semantics bản gốc: isDefault=true luôn áp syntax value;
     * load user (isDefault=false) chỉ áp syntax khi checked, không thì giữ giá trị base.
     */
    private fun fromModel(
        model: Settings?,
        isDefault: Boolean,
        base: AppSettings? = null,
    ): AppSettings {
        if (model == null) return base ?: hardcodedDefaults()
        var s = base ?: AppSettings()
        model.connection?.let {
            s =
                s.copy(
                    maxConn = it.numConn,
                    reConn = it.reConn,
                    delay = it.delay,
                    timeout = it.timeOut,
                    userAgent = it.userAgent,
                )
        }
        model.style?.let { style ->
            style.css?.let {
                s =
                    s.copy(
                        cssChecked = it.checked,
                        cssSyntax = effectiveSyntax(it.checked, it.value, s.cssSyntax, isDefault),
                    )
            }
            style.html?.let {
                s =
                    s.copy(htmlChecked = it.checked, htmlSyntax = effectiveSyntax(it.checked, it.value, s.htmlSyntax, isDefault))
            }
            style.txt?.let {
                s =
                    s.copy(
                        txtChecked = it.checked,
                        txtSyntax = effectiveSyntax(it.checked, it.value, s.txtSyntax, isDefault),
                    )
            }
            style.dropcaps?.let {
                s =
                    s.copy(dropcapsEnabled = it.checked, dropSyntax = effectiveSyntax(it.checked, it.value, s.dropSyntax, isDefault))
            }
        }
        model.other?.let { other ->
            s =
                s.copy(
                    workPath = other.workspace
                        .takeIf { wsp -> wsp.isNotEmpty() && okio.FileSystem.SYSTEM.exists(wsp.toPath()) }
                        ?: EnginePaths.dataDir.toString(), // workspace chỉ phải trỏ tới folder còn tồn tại (container iOS đổi mỗi cài lại)
                    calibre = other.calibre,
                    kindlegen = other.kindlegen,
                    trash = other.trash ?: emptyList(),
                    themeColor = other.themeColor,
                    crashReportEnabled = other.crashReport,
                    pinnedSources = other.pinnedSources ?: emptyList(),
                    readerFontSize = other.readerFontSize,
                    readerFont = other.readerFont,
                    readerBg = other.readerBg,
                    readerFg = other.readerFg,
                    readerLineHeight = other.readerLineHeight,
                    readerAutoScrollSpeed = other.readerAutoScrollSpeed,
                    readerScrollMode = other.readerScrollMode,
                    debugLog = other.debugLog,
                    language = other.language,
                    appTheme = other.appTheme,
                    encryptedPluginPaths = other.encryptedPaths ?: emptyList(),
                    brokenSources = other.brokenSources ?: emptyList(),
                )
        }
        return s
    }

    private fun hardcodedDefaults(): AppSettings {
        // ponytail: gộp path các OS làm 1 list (không phân nhánh os.name như bản gốc) —
        // first-match theo thứ tự, Windows path đứng đầu
        val calibre =
            listOf(
                "C:\\Program Files\\Calibre2\\ebook-convert.exe",
                "C:\\Program Files\\Calibre\\ebook-convert.exe",
                "C:\\Calibre\\ebook-convert.exe",
                "/opt/homebrew/bin/ebook-convert",
                "/usr/local/bin/ebook-convert",
                "/Applications/calibre.app/Contents/MacOS/ebook-convert",
                "/usr/bin/ebook-convert",
                "/usr/local/bin/ebook-convert",
                "/opt/calibre/ebook-convert",
            ).firstOrNull { fs.exists(it.toPath()) } ?: ""
        val kindlegen =
            listOf(
                "C:\\Program Files\\Amazon\\KindleGen\\kindleGen.exe",
                "C:\\KindleGen\\kindleGen.exe",
                "/usr/local/bin/kindlegen",
                "/Applications/KindleGen/kindlegen",
                "/usr/bin/kindlegen",
                "/usr/local/bin/kindlegen",
            ).firstOrNull { fs.exists(it.toPath()) } ?: ""
        return AppSettings(
            workPath = EnginePaths.dataDir.toString(),
            calibre = calibre,
            kindlegen = kindlegen,
        )
    }
}
