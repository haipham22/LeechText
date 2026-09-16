package dev.haipham22.leechtext.util

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SSL/TLS utilities cho site cấu hình SSL lạ (port từ util/SSLUtils.java).
 *
 * WARNING: trust-all chỉ dùng cho scraper này theo hành vi bản gốc — nhiều site truyện
 * dùng cert tự ký/expired. Không dùng pattern này cho app chứa dữ liệu nhạy cảm.
 */
object SSLUtils {
    private fun trustAllManager(): X509TrustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

        override fun checkClientTrusted(
            certs: Array<X509Certificate>,
            authType: String,
        ) {
            // trust-all theo hành vi bản gốc — xem WARNING đầu object
        }

        override fun checkServerTrusted(
            certs: Array<X509Certificate>,
            authType: String,
        ) {
            // trust-all theo hành vi bản gốc — xem WARNING đầu object
        }
    }

    fun createLenientSSLContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(trustAllManager())
        return SSLContext.getInstance("TLSv1.2").apply { init(null, trustAllCerts, SecureRandom()) }
    }

    fun createLenientHostnameVerifier(): HostnameVerifier = HostnameVerifier { _, _ -> true }

    fun createLenientTrustManager(): Array<TrustManager> = arrayOf<TrustManager>(trustAllManager())
}
