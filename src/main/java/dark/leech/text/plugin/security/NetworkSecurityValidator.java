package dark.leech.text.plugin.security;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Security validator for network operations. Enforces HTTPS, content-type validation, and size
 * limits.
 */
public class NetworkSecurityValidator {

    private static final long MAX_DOWNLOAD_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
    private static final int MAX_REDIRECTS = 3;

    // Allowed content types for plugin downloads
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "application/zip", "application/x-zip-compressed", "application/octet-stream"
    };

    /**
     * Validates a URL for plugin download.
     *
     * @param urlString URL to validate
     * @return Validated URL object
     * @throws SecurityValidationException if URL is invalid
     */
    public static URL validateUrl(String urlString) throws SecurityValidationException {
        if (urlString == null || urlString.isEmpty()) {
            throw new SecurityValidationException("URL cannot be empty");
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new SecurityValidationException("Invalid URL: " + urlString, e);
        }

        // Enforce HTTPS for remote URLs (except localhost and file://)
        String protocol = url.getProtocol();
        if (!protocol.equals("https") && !protocol.equals("file") && !isLocalhost(url)) {
            throw new SecurityValidationException(
                    "Only HTTPS URLs are allowed for security (found: "
                            + protocol
                            + "): "
                            + urlString);
        }

        return url;
    }

    /**
     * Checks if a URL points to localhost.
     *
     * @param url URL to check
     * @return true if localhost
     */
    private static boolean isLocalhost(URL url) {
        String host = url.getHost();
        return host != null
                && (host.equals("localhost")
                        || host.equals("127.0.0.1")
                        || host.equals("[::1]")
                        || host.startsWith("127."));
    }

    /**
     * Validates response headers for plugin downloads.
     *
     * @param contentType Content-Type header value
     * @param contentLength Content-Length header value (-1 if unknown)
     * @throws SecurityValidationException if validation fails
     */
    public static void validateResponse(String contentType, long contentLength)
            throws SecurityValidationException {

        // Check content type
        if (contentType != null && !contentType.isEmpty()) {
            boolean typeAllowed = false;
            String lowerContentType = contentType.toLowerCase();

            for (String allowedType : ALLOWED_CONTENT_TYPES) {
                if (lowerContentType.contains(allowedType.toLowerCase())) {
                    typeAllowed = true;
                    break;
                }
            }

            if (!typeAllowed) {
                throw new SecurityValidationException(
                        "Invalid content type for plugin download: " + contentType);
            }
        }

        // Check content length
        if (contentLength > MAX_DOWNLOAD_SIZE_BYTES) {
            throw new SecurityValidationException(
                    String.format(
                            "Download too large: %d MB (max: %d MB)",
                            contentLength / (1024 * 1024),
                            MAX_DOWNLOAD_SIZE_BYTES / (1024 * 1024)));
        }
    }

    /**
     * Gets maximum allowed download size.
     *
     * @return Maximum size in bytes
     */
    public static long getMaxDownloadSize() {
        return MAX_DOWNLOAD_SIZE_BYTES;
    }

    /**
     * Gets maximum allowed redirects.
     *
     * @return Maximum redirect count
     */
    public static int getMaxRedirects() {
        return MAX_REDIRECTS;
    }
}
