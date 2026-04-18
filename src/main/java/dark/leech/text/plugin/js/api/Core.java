package dark.leech.text.plugin.js.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * Core utilities API for JavaScript plugins using Rhino. Provides chapter creation, URL merging,
 * GZIP decoding, and login/confirm helpers.
 */
public class Core extends JsApiWrapper {

    /**
     * Create Core API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Core(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Core API without context (legacy compatibility). */
    public Core() {
        super();
    }

    /** Create chapter object as Map. Usage: core.newChapter("Chapter 1", "/chapter-1") */
    public Map<String, Object> newChapter(String name, String url) {
        return newChapter(name, url, "");
    }

    public Map<String, Object> newChapter(String name, String url, String host) {
        Map<String, Object> chapter = new HashMap<>();
        chapter.put("name", name != null ? name : "");
        chapter.put("url", mergeUrl(host, url));
        chapter.put("host", host != null ? host : "");
        return chapter;
    }

    /** Merge relative URL with host. Enhanced validation and error handling. */
    public String mergeUrl(String host, String url) {
        String cleanUrl = (url != null) ? url.trim() : "";
        if (cleanUrl.isEmpty()) {
            return null;
        }

        String cleanHost = (host != null) ? host.trim() : "";

        // If URL already has protocol, return as-is
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            return cleanUrl;
        }

        // If no host provided, return URL as-is
        if (cleanHost.isEmpty()) {
            return cleanUrl;
        }

        // Ensure host has protocol
        if (!cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
            cleanHost = "https://" + cleanHost;
        }

        // Merge relative URL with host
        if (!cleanUrl.startsWith("/")) {
            // Remove trailing slash from host if present
            if (cleanHost.endsWith("/")) {
                cleanHost = cleanHost.substring(0, cleanHost.length() - 1);
            }
            return cleanHost + "/" + cleanUrl;
        } else {
            // URL starts with /
            if (!cleanHost.endsWith("/")) {
                cleanHost = cleanHost + "/";
            }
            return cleanHost + cleanUrl.substring(1);
        }
    }

    /** Decode GZIP compressed data. Enhanced error handling and resource cleanup. */
    public String decodeGzip(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        ByteArrayInputStream byteIn = null;
        GZIPInputStream gzIn = null;
        ByteArrayOutputStream byteOut = null;

        try {
            byteIn = new ByteArrayInputStream(data);
            gzIn = new GZIPInputStream(byteIn);
            byteOut = new ByteArrayOutputStream();

            int res;
            byte[] buf = new byte[1024];
            while ((res = gzIn.read(buf, 0, buf.length)) > 0) {
                byteOut.write(buf, 0, res);
            }

            return byteOut.toString(StandardCharsets.UTF_8.name());

        } catch (Exception e) {
            Log.add("GZIP decode failed: " + e.getMessage());
            return "";
        } finally {
            closeQuietly(byteOut);
            closeQuietly(gzIn);
            closeQuietly(byteIn);
        }
    }

    /** Create login URL. Usage: core.createLogin("https://example.com/login") */
    public String createLogin(String url) {
        return "0#" + (url != null ? url : "");
    }

    /** Create confirm URL. Usage: core.createConfirm("https://example.com/confirm") */
    public String createConfirm(String url) {
        return "1#" + (url != null ? url : "");
    }

    /** Encode string to Base64. */
    public String base64Encode(String text) {
        if (text == null) {
            return "";
        }
        try {
            return java.util.Base64.getEncoder()
                    .encodeToString(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.add("Base64 encode failed: " + e.getMessage());
            return "";
        }
    }

    /** Decode Base64 string. */
    public String base64Decode(String encoded) {
        if (encoded == null) {
            return "";
        }
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.add("Base64 decode failed: " + e.getMessage());
            return "";
        }
    }

    /** Create MD5 hash of string. */
    public String md5(String text) {
        if (text == null) {
            return "";
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.add("MD5 calculation failed: " + e.getMessage());
            return "";
        }
    }

    /** Create SHA-256 hash of string. */
    public String sha256(String text) {
        if (text == null) {
            return "";
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.add("SHA-256 calculation failed: " + e.getMessage());
            return "";
        }
    }

    /** Sleep for specified milliseconds. */
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Get current timestamp in milliseconds. */
    public long timestamp() {
        return System.currentTimeMillis();
    }

    /** Get current timestamp in seconds. */
    public long timestampSec() {
        return System.currentTimeMillis() / 1000;
    }

    /** Close a resource quietly (ignore exceptions). */
    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
