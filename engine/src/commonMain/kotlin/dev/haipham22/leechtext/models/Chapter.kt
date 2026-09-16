package dev.haipham22.leechtext.models

/** Chương đang tải (port từ models/Chapter.java). */
data class Chapter(
    var url: String? = "",
    var partName: String? = "",
    var chapName: String? = "",
    var completed: Boolean = false,
    var error: Boolean = false,
    var empty: Boolean = false,
    var imageChapter: Boolean = false,
    var id: String? = null,
    var purchase: Boolean = false,
) {
    fun setId(id: Int) {
        this.id = "C$id"
    }
}
