package dev.haipham22.leechtext.plugin.js.loader

/**
 * Loại loader vBook plugin — mỗi loại một scope riêng (port từ LoaderType.java).
 */
enum class LoaderType(
    val type: String,
    val scriptName: String,
) {
    LIST("list", "tocGetter"),
    DETAIL("detail", "detailGetter"),
    TEXT("text", "chapGetter"),
    PAGE("page", "pageGetter"),
}
