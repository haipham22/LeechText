package dev.haipham22.leechtext.plugin.js.api

/**
 * Seam giá trị JS↔Kotlin cho các engine script (P5.2c).
 *
 * Rhino actual (sharedJvmMain) tạo/thao tác NativeObject/NativeArray/Function — giữ
 * nguyên contract Rhino 1.7.15 (JS property access trên object trả về). QuickJS actual
 * (appleMain) dùng Map/List + JsObject của quickjs-kt.
 *
 * Loaders/common code nhận giá trị ĐÃ normalize (Map/List/primitive/null) qua
 * JsSandbox.callFunction — các hàm ở đây chỉ cần cho giá trị TẠO ra (đẩy vào JS) hoặc
 * giá trị engine-native tràn qua từ JS (callback, Json.parse...).
 */
@Suppress("TooManyFunctions") // API surface cho plugin JS — số hàm mirror đúng API vBook, không tách
expect object JsValueBridge {
    /** Tạo engine array (JVM: NativeArray; apple: List). */
    fun arrayOf(items: List<Any?>): Any

    /** Tạo engine object (JVM: NativeObject; apple: Map). */
    fun objectOf(pairs: List<Pair<String, Any?>>): Any

    /** Parse JSON → engine-native value (JVM: NativeJSON.parse; apple: kotlinx → Map/List). */
    fun parseJson(json: String): Any?

    /** JSON-stringify giá trị engine (match format Json.stringify). */
    fun stringify(value: Any?): String

    /** Property access trên engine object (NativeObject/Map). */
    fun getProp(
        obj: Any?,
        key: String,
    ): Any?

    fun hasProp(
        obj: Any?,
        key: String,
    ): Boolean

    fun setProp(
        obj: Any?,
        key: String,
        value: Any?,
    )

    /** Size của engine array/list (NativeArray/Array/List). */
    fun sizeOf(value: Any?): Int

    /** Phần tử tại index của engine array/list. */
    fun itemAt(
        value: Any?,
        index: Int,
    ): Any?

    /** Giá trị JS null/undefined (JVM: null | Undefined; apple: null). */
    fun isNullValue(value: Any?): Boolean

    /** JS toString semantics (JVM: Context.toString; apple: Kotlin string). */
    fun toJsString(value: Any?): String?

    /** Giá trị có phải JS function không (callback JSList/JSElements). */
    fun isFunction(value: Any?): Boolean

    /** Gọi JS function — null nếu không phải function/lỗi. */
    fun call(
        fn: Any?,
        args: List<Any?>,
    ): Any?
}
