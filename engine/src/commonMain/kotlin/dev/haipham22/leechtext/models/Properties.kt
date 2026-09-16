package dev.haipham22.leechtext.models

/**
 * properties.json của mỗi book (port từ models/Properties.java).
 * Không phải java.util.Properties — model per-book.
 */
data class Properties(
    var name: String? = null,
    var author: String? = null,
    var url: String? = null,
    var cover: String? = null,
    var chapList: List<Chapter>? = null,
    var pageList: List<Pager>? = null,
    var forum: Boolean = false,
    var size: Int = 0,
    var savePath: String? = null,
    var introduce: String? = null,
    var addGt: Boolean = false,
    var charset: String? = "UTF-8",
    var urlList: Array<String>? = null,
    /** True = Đang ra, false = Hoàn thành — từ field `ongoing` của detail.js. */
    var ongoing: Boolean? = null,
) {
    /** Resolve cover tương đối theo URL trang. */
    fun setCover(
        cover: String?,
        page: String?,
    ) {
        if (cover == null) return
        this.cover = if (cover.startsWith("http")) cover else page + cover
    }

    override fun equals(other: Any?): Boolean = other is Properties && name == other.name && author == other.author &&
        url == other.url && cover == other.cover && chapList == other.chapList &&
        pageList == other.pageList && forum == other.forum && size == other.size &&
        savePath == other.savePath && introduce == other.introduce &&
        addGt == other.addGt && charset == other.charset &&
        urlList.contentEquals(other.urlList)

    override fun hashCode(): Int = urlList?.contentHashCode() ?: 0
}
