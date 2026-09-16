package dev.haipham22.leechtext.util

import kotlinx.serialization.json.Json

/**
 * Json config thay Gson (P4, design 2A — Gson cần reflection, không chạy wasm).
 * Hành vi khớp Gson để giữ format file legacy (setting.json, repository.json, .plugin):
 * - ignoreUnknownKeys: Gson bỏ qua field lạ
 * - isLenient: Gson coerce "5" (string) → Int (num_conn/time_out legacy là string)
 * - encodeDefaults: Gson ghi mọi field non-null kể cả giá trị default
 * - explicitNulls=false: Gson bỏ qua null
 */
val LeechJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

/** Bản pretty (thay GsonBuilder().setPrettyPrinting()) — setting.json, plugin.json. */
val LeechJsonPretty: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = true
}
