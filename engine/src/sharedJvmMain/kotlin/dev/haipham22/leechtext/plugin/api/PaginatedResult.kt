package dev.haipham22.leechtext.plugin.api

/**
 * Result container phân trang cho search/listing (port từ api/PaginatedResult.java).
 */
data class PaginatedResult<T>(
    val items: List<T> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalPages: Int = 0,
    val hasMore: Boolean = false,
) {
    /** Page tiếp theo, -1 nếu hết. */
    fun getNextPage(): Int = if (hasMore) page + 1 else -1

    /** Page trước, -1 nếu đang ở page đầu. */
    fun getPreviousPage(): Int = if (page > 0) page - 1 else -1

    companion object {
        /** Tạo từ items + total count. */
        @JvmStatic
        fun <T> of(
            items: List<T>,
            totalCount: Int,
            page: Int,
            pageSize: Int,
        ): PaginatedResult<T> {
            val totalPages =
                if (pageSize > 0) {
                    kotlin.math.ceil(totalCount.toDouble() / pageSize).toInt()
                } else {
                    0
                }
            return PaginatedResult(
                items = items,
                totalCount = totalCount,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                hasMore = page < totalPages - 1,
            )
        }

        /** Result rỗng. */
        @JvmStatic
        fun <T> empty(): PaginatedResult<T> = PaginatedResult(pageSize = 0, totalPages = 0, hasMore = false)
    }
}
