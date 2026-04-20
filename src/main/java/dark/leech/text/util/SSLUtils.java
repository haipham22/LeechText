package dark.leech.text.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import dark.leech.text.action.Log;

/**
 * SSL/TLS utilities for fixing connection issues with problematic websites.
 *
 * <p>Provides workarounds for SSL handshake errors, certificate validation issues, and TLS version
 * mismatches when connecting to websites with non-standard SSL configurations.
 */
public class SSLUtils {

    /**
     * Create a lenient SSL context that trusts all certificates.
     *
     * <p><strong>WARNING:</strong> This should only be used for development or testing purposes.
     * Trusting all certificates is a security risk in production.
     *
     * <p>This can be used as a last resort when connecting to websites with:
     *
     * <ul>
     *   <li>Self-signed certificates
     *   <li>Expired certificates
     *   <li>Certificate chain issues
     * </ul>
     *
     * @return SSLContext that trusts all certificates
     * @throws RuntimeException if SSL context initialization fails
     */
    public static SSLContext createLenientSSLContext() {
        try {
            TrustManager[] trustAllCerts =
                    new TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }

                            public void checkClientTrusted(
                                    X509Certificate[] certs, String authType) {
                                // Trust all client certificates
                            }

                            public void checkServerTrusted(
                                    X509Certificate[] certs, String authType) {
                                // Trust all server certificates
                            }
                        }
                    };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create lenient SSL context", e);
        }
    }

    /**
     * Create a lenient HostnameVerifier that accepts all hostnames.
     *
     * <p><strong>WARNING:</strong> This should only be used for development or testing purposes.
     * Disabling hostname verification is a security risk in production.
     *
     * @return HostnameVerifier that accepts all hostnames
     */
    public static HostnameVerifier createLenientHostnameVerifier() {
        return (hostname, session) -> true;
    }

    /**
     * Create TrustManager array that trusts all certificates.
     *
     * <p><strong>WARNING:</strong> This should only be used for development or testing purposes.
     *
     * @return TrustManager array with single X509TrustManager
     */
    public static TrustManager[] createLenientTrustManager() {
        return new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all client certificates
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all server certificates
                }
            }
        };
    }

    /**
     * Configure jsoup Connection with lenient SSL settings.
     *
     * <p><strong>WARNING:</strong> This should only be used for development or testing purposes.
     *
     * @param connection The jsoup Connection to configure
     */
    public static void configureConnectionWithLenientSSL(org.jsoup.Connection connection) {
        try {
            SSLContext sslContext = createLenientSSLContext();

            // Use jsoup's native SSL context configuration API (preferred over deprecated
            // sslSocketFactory)
            connection.sslContext(sslContext);

        } catch (Exception e) {
            // If configuration fails, just log and continue
            Log.add("Failed to configure lenient SSL: " + e.getMessage());
        }
    }
}
