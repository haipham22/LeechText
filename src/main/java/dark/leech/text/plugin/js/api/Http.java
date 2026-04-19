package dark.leech.text.plugin.js.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;
import dark.leech.text.util.CookiesUtils;
import dark.leech.text.util.SettingUtils;

/**
 * HTTP API for JavaScript plugins using Rhino. Provides GET, POST, headers, body, params, and
 * response parsing.
 */
public class Http extends JsApiWrapper {

    private Connection connection;
    private Connection.Response response;
    private String url;
    private boolean syncCookie = true;

    /**
     * Create Http API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Http(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Http API without context (legacy compatibility). */
    public Http() {
        super();
    }

    /** Create new HTTP request builder. Usage: http.request("https://example.com") */
    public Http request(String url) {
        this.url = url;
        this.connection =
                Jsoup.connect(url)
                        .header("User-Agent", SettingUtils.USER_AGENT)
                        //                        .header("Accept",
                        // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        //                        .header("Accept-Language",
                        // "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                        //                        .header("Accept-Encoding", "gzip, deflate, br")
                        //                        .header("Connection", "keep-alive")
                        .followRedirects(true)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(SettingUtils.TIMEOUT)
                        .maxBodySize(0);
        return this;
    }

    /** GET request shorthand. Usage: http.get("https://example.com").string() */
    public Http get(String url) {
        return request(url).method("GET");
    }

    /** POST request shorthand. Usage: http.post("https://example.com").body("data").string() */
    public Http post(String url) {
        return request(url).method("POST");
    }

    /** PUT request shorthand. Usage: http.put("https://example.com").body("data").string() */
    public Http put(String url) {
        return request(url).method("PUT");
    }

    /** DELETE request shorthand. Usage: http.delete("https://example.com").string() */
    public Http delete(String url) {
        return request(url).method("DELETE");
    }

    /** PATCH request shorthand. Usage: http.patch("https://example.com").body("{}").string() */
    public Http patch(String url) {
        return request(url).method("PATCH");
    }

    /** HEAD request shorthand. Usage: http.head("https://example.com").statusCode() */
    public Http head(String url) {
        return request(url).method("HEAD");
    }

    /** Set request method. */
    public Http method(String method) {
        try {
            this.connection.method(Connection.Method.valueOf(method.toUpperCase()));
        } catch (IllegalArgumentException e) {
            Log.add("Invalid HTTP method: " + method);
        }
        return this;
    }

    /** Set request headers from Map. Usage: http.request(url).headers({"User-Agent": "custom"}) */
    @SuppressWarnings("unchecked")
    public Http headers(Object headers) {
        if (headers instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) headers;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    connection.header((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return this;
    }

    /** Set request header. */
    public Http header(String key, String value) {
        connection.header(key, value);
        return this;
    }

    /** Set request body. Usage: http.request(url).body("data") */
    public Http body(String body) {
        connection.requestBody(body);
        return this;
    }

    /** Set form parameters from Map. Usage: http.request(url).params({"key": "value"}) */
    @SuppressWarnings("unchecked")
    public Http params(Object params) {
        if (params instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) params;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String strValue =
                            entry.getValue() != null
                                    ? org.mozilla.javascript.Context.toString(entry.getValue())
                                    : "";
                    connection.data((String) entry.getKey(), strValue);
                }
            }
        }
        return this;
    }

    /** Set form parameter. */
    public Http param(String key, String value) {
        connection.data(key, value);
        return this;
    }

    /**
     * Set form-encoded body with proper Content-Type. Usage: http.request(url).form({"key":
     * "value"})
     */
    @SuppressWarnings("unchecked")
    public Http form(Object data) {
        if (data instanceof Map) {
            // Set Content-Type header
            connection.header("Content-Type", "application/x-www-form-urlencoded");

            // Build form-encoded string
            StringBuilder formBody = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String strValue =
                            entry.getValue() != null
                                    ? org.mozilla.javascript.Context.toString(entry.getValue())
                                    : "";

                    if (formBody.length() > 0) {
                        formBody.append("&");
                    }
                    formBody.append(urlEncode((String) entry.getKey()))
                            .append("=")
                            .append(urlEncode(strValue));
                }
            }
            connection.requestBody(formBody.toString());
        }
        return this;
    }

    /**
     * Set query parameters (appends to URL). Usage: http.request(url).queries({"page": "1",
     * "limit": "10"})
     */
    @SuppressWarnings("unchecked")
    public Http queries(Object params) {
        if (params instanceof Map) {
            StringBuilder query = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) params;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String strValue =
                            entry.getValue() != null
                                    ? org.mozilla.javascript.Context.toString(entry.getValue())
                                    : "";

                    if (query.length() > 0) {
                        query.append("&");
                    }
                    query.append(urlEncode((String) entry.getKey()))
                            .append("=")
                            .append(urlEncode(strValue));
                }
            }

            // Append to URL
            if (query.length() > 0) {
                String separator = url.contains("?") ? "&" : "?";
                this.url = url + separator + query.toString();
                connection.url(this.url);
            }
        }
        return this;
    }

    /**
     * Alias for params() for better API consistency. Usage: http.request(url).data({"key":
     * "value"})
     */
    public Http data(Object params) {
        return params(params);
    }

    /** URL encode a string value. */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            Log.add("URL encoding failed: " + e.getMessage());
            return value;
        }
    }

    /** Set timeout in milliseconds. */
    public Http timeout(int ms) {
        connection.timeout(ms);
        return this;
    }

    /** Enable or disable cookie synchronization. */
    public Http syncCookie(boolean sync) {
        this.syncCookie = sync;
        return this;
    }

    /**
     * Execute request and return parsed HTML document. Returns JSDocument for vBook plugin
     * compatibility. Usage: http.get(url).html()
     */
    public JSDocument html() {
        try {
            Connection.Response response = execute();
            org.jsoup.nodes.Document doc = response.parse();
            // Defensive: if parse returns null, use empty document
            if (doc == null) {
                Log.add("Response.parse() returned null, using empty document");
                return new JSDocument(org.jsoup.Jsoup.parse(""));
            }
            return new JSDocument(doc);
        } catch (IOException e) {
            Log.add("Failed to get HTML document: " + e.getMessage());
            return new JSDocument(org.jsoup.Jsoup.parse(""));
        }
    }

    /**
     * Execute request and return parsed HTML document. Returns a JSDocument wrapper. Returns empty
     * document on error. Usage: http.get(url).document()
     */
    public JSDocument document() {
        try {
            org.jsoup.nodes.Document doc = execute().parse();
            return new JSDocument(doc);
        } catch (IOException e) {
            Log.add("Failed to parse HTML document: " + e.getMessage());
            return new JSDocument(org.jsoup.Jsoup.parse(""));
        }
    }

    /**
     * Execute request and return response body as string. Handles BOM (Byte Order Mark) for UTF-8.
     * Usage: http.get(url).string()
     */
    public String string() {
        try {
            String body = execute().body();
            // Remove BOM if present
            if (body.startsWith("\uFEFF")) {
                body = body.substring(1);
            }
            return body;
        } catch (IOException e) {
            Log.add("Failed to get response body: " + e.getMessage());
            return "";
        }
    }

    /** Execute request and return response as bytes. Usage: http.get(url).bytes() */
    public byte[] bytes() {
        try {
            return execute().bodyAsBytes();
        } catch (IOException e) {
            Log.add("Failed to get response bytes: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute request and parse JSON response. Returns Map (JavaScript object). Returns null on
     * error. Usage: http.get(url).json()
     */
    @SuppressWarnings("unchecked")
    public Object json() {
        try {
            String body = string();
            if (body == null || body.isEmpty()) {
                return null;
            }
            Json jsonApi = new Json();
            return jsonApi.parse(body);
        } catch (Exception e) {
            Log.add("Failed to parse JSON: " + e.getMessage());
            return null;
        }
    }

    /** Get response status code. Usage: http.get(url).statusCode() */
    public int statusCode() {
        try {
            return execute().statusCode();
        } catch (IOException e) {
            return -1;
        }
    }

    /** Check if response was successful (status code 2xx or 3xx). */
    public boolean ok() {
        int code = statusCode();
        return code >= 200 && code < 400;
    }

    /** Get response status message. */
    public String statusMessage() {
        try {
            return execute().statusMessage();
        } catch (IOException e) {
            return "";
        }
    }

    /** Get response headers as Map. Usage: http.get(url).responseHeaders() */
    public Map<String, String> responseHeaders() {
        try {
            return execute().headers();
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    /** Get specific response header. */
    public String header(String name) {
        try {
            return execute().header(name);
        } catch (IOException e) {
            return null;
        }
    }

    /** Get content type from response headers. */
    public String contentType() {
        return header("Content-Type");
    }

    /** Execute the HTTP request with cookie handling. */
    private Connection.Response execute() throws IOException {
        // Cookie handling
        if (syncCookie) {
            String cookies = CookiesUtils.getCookies(url);
            if (cookies != null && !cookies.isEmpty()) {
                connection.header("Cookie", cookies);
            }
        }

        response = connection.execute();

        // Sync response cookies
        if (syncCookie) {
            String setCookie = response.header("Set-Cookie");
            if (setCookie != null && !setCookie.isEmpty()) {
                CookiesUtils.put(url, setCookie);
            }
        }

        return response;
    }
}
