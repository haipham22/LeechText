package dev.haipham22.leechtext.models

/** Kết quả nội dung một chương (port từ models/Post.java). */
data class Post(
    var partName: String? = "",
    var chapName: String? = "",
    var error: Boolean = false,
    var empty: Boolean = false,
    var imageChapter: Boolean = false,
    var text: String? = "",
)
