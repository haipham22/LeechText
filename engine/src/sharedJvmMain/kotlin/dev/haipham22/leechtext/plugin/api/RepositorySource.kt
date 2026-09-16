package dev.haipham22.leechtext.plugin.api

import dev.haipham22.leechtext.entities.PluginEntity
import java.io.InputStream
import java.util.concurrent.CompletableFuture

/**
 * Abstraction cho plugin repository sources (port từ api/RepositorySource.java) — hỗ trợ
 * nhiều loại repo (vBook, LeechText, custom).
 */
interface RepositorySource<T : PluginMetadata> {
    /** Unique id — format "{type}-{id}" vd "vbook-default". */
    val id: String

    /** Tên hiển thị trong UI. */
    val name: String

    /** Loại repository — vd "vbook", "leechtext", "github", "custom". */
    val type: String

    /** Source có đang enabled không — disabled không search/download. */
    var isEnabled: Boolean

    /**
     * Search plugin theo query, kết quả phân trang.
     *
     * @param query query string
     * @param page page number (0-indexed)
     * @param pageSize results per page (khuyến nghị 20-50)
     */
    @Throws(RepositoryException::class)
    fun search(
        query: String?,
        page: Int,
        pageSize: Int,
    ): PaginatedResult<T>

    /** List mọi plugin trong repository (phân trang). */
    @Throws(RepositoryException::class)
    fun listAll(
        page: Int,
        pageSize: Int,
    ): PaginatedResult<T>

    /** Download plugin theo id — trả raw PluginEntity cần format conversion. */
    @Throws(RepositoryException::class)
    fun download(pluginId: String): PluginEntity

    /** Source có handle được URL này không — dùng cho auto-discovery. */
    fun canHandle(url: String?): Boolean

    /** Tìm + install plugin cho URL — async, trả null nếu không match. */
    fun findAndInstall(url: String?): CompletableFuture<PluginEntity?>

    /** Health status — dùng cho monitoring/circuit breaking. */
    val health: RepositoryHealth

    /** Metadata repository — version, update check URL, etc. */
    val metadata: RepositoryMetadata
}

/**
 * Abstraction cho plugin file format (port từ api/PluginFormat.java) — mỗi repository có thể
 * dùng format khác nhau.
 */
interface PluginFormat {
    /** Format id — vd "vbook-json", "leechtext-json", "zip". */
    val formatType: String

    /**
     * Parse plugin từ input stream, validate format khi parse.
     */
    @Throws(PluginFormatException::class)
    fun parse(input: InputStream): PluginEntity

    /** Validate plugin trước khi install — required fields, script syntax, security. */
    fun validate(plugin: PluginEntity): ValidationResult

    /** Convert sang standard LeechText format nếu cần (field mapping). */
    fun normalize(plugin: PluginEntity): PluginEntity

    /**
     * Extract metadata không full parse — dùng cho search results/listings.
     */
    @Throws(PluginFormatException::class)
    fun extractMetadata(input: InputStream): PluginMetadata
}
