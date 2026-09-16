package dev.haipham22.leechtext.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Metadata sách (port từ enities/BookEntity.java). */
@Serializable
data class BookEntity(
    @SerialName("book_id") var id: String = "",
    @SerialName("name") var name: String? = null,
    @SerialName("author") var author: String? = null,
    @SerialName("cover") var cover: String? = null,
    @SerialName("url") var url: String? = null,
    @SerialName("introduce") var introduce: String? = null,
    @SerialName("web_source") var webSource: String? = null,
    @SerialName("detail") var detail: String? = null,
    /** True = Đang ra, false = Hoàn thành — field `ongoing` từ detail.js (vd truyenfull). */
    @SerialName("ongoing") var ongoing: Boolean? = null,
)
