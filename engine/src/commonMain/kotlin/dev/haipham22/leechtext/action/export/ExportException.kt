package dev.haipham22.leechtext.action.export

/**
 * Export fail với code machine-readable — UI map code → stringResource theo locale
 * (engine không phụ thuộc Compose resources; message English chỉ cho log/dev).
 */
class ExportException(
    message: String,
    /** vd "empty" — UI tra bảng string, null/unknown → fallback message thô. */
    val code: String?,
    /** Data kèm theo (vd size bytes) — placeholder %1$s trong string UI. */
    val detail: String = "",
) : RuntimeException(message)
