package dev.haipham22.leechtext.ui.model

import androidx.compose.runtime.Composable
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.export_error_empty
import dev.haipham22.leechtext.resources.sources_error_encrypted
import dev.haipham22.leechtext.resources.sources_error_plugin
import dev.haipham22.leechtext.resources.sources_error_registry
import dev.haipham22.leechtext.resources.sources_error_security
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Chuỗi UI sinh ngoài composition (state/VM: message, status queue...) — giữ
 * resource + args, resolve ở chỗ render. Raw = text động (lỗi engine, tên file).
 * Tone để UI chọn màu (ExportBookDialog đỏ/xanh) thay vì dò prefix chuỗi.
 */
sealed interface UiText {
    /** Tone hiển thị — chỉ message dùng; mặc định NEUTRAL. */
    enum class Tone { NEUTRAL, ERROR, SUCCESS }

    data class Res(
        val key: StringResource,
        val args: List<Any> = emptyList(),
        val tone: Tone = Tone.NEUTRAL,
    ) : UiText

    data class Raw(
        val text: String,
        val tone: Tone = Tone.NEUTRAL,
    ) : UiText
}

/** Tone hiển thị (đỏ/xanh) — dùng cho message coloring. */
fun UiText.tone(): UiText.Tone = when (this) {
    is UiText.Res -> tone
    is UiText.Raw -> tone
}

/** Resolve UiText trong composition — đổi locale là recompose lại chuỗi mới. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res ->
        if (args.isEmpty()) {
            stringResource(key)
        } else {
            // ponytail: spread copy mảng 1-4 phần tử mỗi lần format — ổn
            @Suppress("SpreadOperator")
            stringResource(key, *args.toTypedArray())
        }

    is UiText.Raw -> text
}

/**
 * Exception mang UiText — lỗi user-facing throw từ state/pipeline (ngoài
 * composition), UI bắt rồi hiển thị qua UiText.asString() thay vì message
 * tiếng Việt hardcode. Engine (module :engine) KHÔNG dùng class này.
 */
class UiException(
    val ui: UiText,
) : IllegalArgumentException(ui.toString())

/** UiText từ exception: UiException mang key sẵn, exception engine có code → map
 * stringResource theo locale, còn lại Raw message kỹ thuật (i18n lỗi engine).
 * Chỉ map exception ở commonMain của engine — class sharedJvmMain (RepositoryException,
 * PluginFormatException...) rơi về Raw để không vỡ target iOS. */
fun uiTextOf(e: Exception): UiText = when {
    e is UiException -> e.ui

    // ExportException.code == "empty" — detail = size bytes
    e is dev.haipham22.leechtext.action.export.ExportException && e.code == "empty" ->
        UiText.Res(
            Res.string.export_error_empty,
            listOf(e.detail),
            UiText.Tone.ERROR,
        )

    e is dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException && e.code == "encrypted" ->
        UiText.Res(Res.string.sources_error_encrypted, tone = UiText.Tone.ERROR)

    // Plugin/registry/security — typed exception commonMain của engine
    e is dev.haipham22.leechtext.plugin.vbook.exception.VBookPluginException ->
        UiText.Res(Res.string.sources_error_plugin, tone = UiText.Tone.ERROR)

    e is dev.haipham22.leechtext.plugin.vbook.exception.VBookRegistryException ->
        UiText.Res(Res.string.sources_error_registry, tone = UiText.Tone.ERROR)

    e is dev.haipham22.leechtext.plugin.security.SecurityValidationException ->
        UiText.Res(Res.string.sources_error_security, tone = UiText.Tone.ERROR)

    else -> UiText.Raw(e.message ?: e::class.simpleName ?: "error", UiText.Tone.ERROR)
}
