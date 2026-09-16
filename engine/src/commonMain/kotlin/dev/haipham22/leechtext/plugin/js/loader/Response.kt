package dev.haipham22.leechtext.plugin.js.loader

import dev.haipham22.leechtext.plugin.js.api.JsValueBridge

/**
 * Response API vBook (port từ loader/Response.java; P5.2c common qua JsValueBridge).
 * success/error tạo engine object {code, data, data2} — companion đọc giá trị normalized
 * (Map) hoặc engine-native (getProp seam).
 */
class Response {
    fun success(data: Any?): Any = JsValueBridge.objectOf(listOf("code" to 0, "data" to data))

    fun success(
        data: Any?,
        data2: Any?,
    ): Any = JsValueBridge.objectOf(listOf("code" to 0, "data" to data, "data2" to data2))

    fun error(data: Any?): Any = JsValueBridge.objectOf(listOf("code" to 1, "data2" to data))

    fun error(
        code: Int,
        data: Any?,
    ): Any = JsValueBridge.objectOf(listOf("code" to code, "data2" to data))

    fun error(
        code: Int,
        data: Any?,
        message: String?,
    ): Any = JsValueBridge.objectOf(listOf("code" to code, "data2" to data, "message" to message))

    companion object {
        /** Result có phải Response success (code == 0) — null/not-Response tính là raw data. */
        fun isSuccess(result: Any?): Boolean {
            if (!JsValueBridge.hasProp(result, "code")) return true
            val codeProp = JsValueBridge.getProp(result, "code")
            if (codeProp !is Number) return true
            return codeProp.toInt() == 0
        }

        fun getData(result: Any?): Any? {
            if (result == null) return null
            if (!JsValueBridge.hasProp(result, "code")) {
                // Dual data format legacy {data, data2} không code
                return if (JsValueBridge.hasProp(result, "data") && JsValueBridge.hasProp(result, "data2")) {
                    JsValueBridge.getProp(result, "data")
                } else {
                    result
                }
            }
            val codeProp = JsValueBridge.getProp(result, "code")
            return when {
                codeProp !is Number -> result
                codeProp.toInt() != 0 -> null
                else -> JsValueBridge.getProp(result, "data")
            }
        }

        fun getErrorMessage(result: Any?): String? {
            if (!JsValueBridge.hasProp(result, "code")) return null
            val codeProp = JsValueBridge.getProp(result, "code")
            if (codeProp !is Number) return null
            if (codeProp.toInt() == 0) return null
            val errorData = JsValueBridge.getProp(result, "data2")
            return if (errorData != null) JSResponse.getString(errorData) ?: "Unknown error" else "Unknown error"
        }

        fun getData2(result: Any?): Any? {
            if (!JsValueBridge.hasProp(result, "code")) {
                // Dual data format legacy {data, data2} không code
                return if (JsValueBridge.hasProp(result, "data") && JsValueBridge.hasProp(result, "data2")) {
                    JsValueBridge.getProp(result, "data2")
                } else {
                    null
                }
            }
            val codeProp = JsValueBridge.getProp(result, "code")
            return when {
                codeProp !is Number -> null
                codeProp.toInt() != 0 -> null
                else -> JsValueBridge.getProp(result, "data2")
            }
        }
    }
}
