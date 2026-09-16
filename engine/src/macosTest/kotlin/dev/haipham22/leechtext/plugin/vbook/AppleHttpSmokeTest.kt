package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.js.api.Http
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke ktor-Darwin HTTP trên Apple (P5.2c seam) — CHẠY CẦN MẠNG, skip khi offline
 * (CI không có rule này nên chỉ chạy local qua -PappleNetSmoke=true).
 */
class AppleHttpSmokeTest {
    @Test
    fun fetchVbookRegistry() {
        val body = Http(platformEngineLogger()).request("https://raw.githubusercontent.com/Darkrai9x/vbook-extensions/master/repository.json").string()
        assertTrue(body.contains("extensions") || body.contains("data"), "body rỗng/lỗi: ${body.take(120)}")
    }
}
