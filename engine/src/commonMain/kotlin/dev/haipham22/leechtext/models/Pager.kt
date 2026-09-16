package dev.haipham22.leechtext.models

/** Một trang pagination (port từ models/Pager.java). */
data class Pager(
    var url: String? = null,
    var name: String? = null,
    var chapter: List<Chapter>? = null,
    var id: String? = null,
    var completed: Boolean = false,
) {
    fun setId(id: Int) {
        this.id = "P$id"
    }
}
