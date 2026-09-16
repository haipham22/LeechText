package dev.haipham22.leechtext.plugin.js.api

/**
 * UserAgent presets cho JS plugins (port từ UserAgent.java — pure constants, P5.2c common).
 */
@Suppress("UtilityClassWithPublicConstructor") // vBook binding bind INSTANCE (JsApiSetup)
class UserAgent {
    companion object {
        // Current versions as of 2025 (giữ nguyên giá trị bản gốc)
        private const val ANDROID_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"

        private const val IOS_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"

        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Safari/537.36"

        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"

        fun android(): String = ANDROID_UA

        fun iOS(): String = IOS_UA

        fun desktop(): String = DESKTOP_UA

        fun mobile(): String = MOBILE_UA
    }
}
