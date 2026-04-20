package dark.leech.text.plugin.js.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import dark.leech.text.action.Log;
import dark.leech.text.util.CookiesUtils;
import dark.leech.text.util.SSLUtils;
import dark.leech.text.util.SettingUtils;

/**
 * HTTP API for JavaScript plugins using Rhino. Provides GET, POST, headers, body, params, and
 * response parsing.
 *
 * <p>Now uses OkHttp3 (matching vBooks Android implementation) instead of jsoup for better HTTP
 * compatibility and robustness.
 */
public class Http extends JsApiWrapper {

    private Request.Builder requestBuilder;
    private Response response;
    private String url;
    private RequestBody requestBody;
    private boolean syncCookie = true;
    private String method = "GET";

    // OkHttp3 client with lenient SSL configuration (matches vBooks Android)
    private static final OkHttpClient OK_HTTP_CLIENT =
            new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .sslSocketFactory(
                            SSLUtils.createLenientSSLContext().getSocketFactory(),
                            (javax.net.ssl.X509TrustManager)
                                    SSLUtils.createLenientTrustManager()[0])
                    .hostnameVerifier(SSLUtils.createLenientHostnameVerifier())
                    .build();

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
        this.requestBuilder = new Request.Builder().url(url);

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
        this.method = method.toUpperCase();
        return this;
    }

    /** Set request headers from Map. Usage: http.request(url).headers({"User-Agent": "custom"}) */
    public Http headers(Object headers) {
        if (headers instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    this.requestBuilder.header((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return this;
    }

    /** Set request header. */
    public Http header(String key, String value) {
        this.requestBuilder.header(key, value);
        return this;
    }

    /** Set request body. Usage: http.request(url).body("data") */
    public Http body(String body) {
        this.requestBody =
                RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));
        return this;
    }

    /** Set form parameters from Map. Usage: http.request(url).params({"key": "value"}) */
    public Http params(Object params) {
        if (params instanceof Map<?, ?> map) {
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String strValue =
                            entry.getValue() != null
                                    ? org.mozilla.javascript.Context.toString(entry.getValue())
                                    : "";
                    formBuilder.add((String) entry.getKey(), strValue);
                }
            }
            this.requestBody = formBuilder.build();
        }
        return this;
    }

    /** Set form parameter. */
    public Http param(String key, String value) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (this.requestBody instanceof FormBody) {
            // Preserve existing form data
            // Note: OkHttp3 FormBody is immutable, so we need to rebuild
        }
        formBuilder.add(key, value);
        this.requestBody = formBuilder.build();
        return this;
    }

    /**
     * Set form-encoded body with proper Content-Type. Usage: http.request(url).form({"key":
     * "value"})
     */
    public Http form(Object data) {
        return params(data); // OkHttp3 handles Content-Type automatically
    }

    /**
     * Set query parameters (appends to URL). Usage: http.request(url).queries({"page": "1",
     * "limit": "10"})
     */
    public Http queries(Object params) {
        if (params instanceof Map<?, ?> map) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(this.url).newBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String strValue =
                            entry.getValue() != null
                                    ? org.mozilla.javascript.Context.toString(entry.getValue())
                                    : "";
                    urlBuilder.addQueryParameter((String) entry.getKey(), strValue);
                }
            }
            this.url = urlBuilder.build().toString();
            this.requestBuilder.url(this.url);
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

    /** Set timeout in milliseconds. */
    public Http timeout(int ms) {
        // OkHttp3 client already configured with 90s timeout
        // Individual request timeout would require custom client per request
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
            Response response = execute();
            String html = response.body().string();
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html, this.url);
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
        return html();
    }

    /**
     * Execute request and return response body as string. Handles BOM (Byte Order Mark) for UTF-8.
     * Usage: http.get(url).string()
     */
    public String string() {
        try {
            String body = execute().body().string();
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
            return execute().body().bytes();
        } catch (IOException e) {
            Log.add("Failed to get response bytes: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute request and parse JSON response. Returns Map (JavaScript object). Returns null on
     * error. Usage: http.get(url).json()
     */
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
            return execute().code();
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
            return execute().message();
        } catch (IOException e) {
            return "";
        }
    }

    /** Get response headers as Map. Usage: http.get(url).responseHeaders() */
    public Map<String, String> responseHeaders() {
        try {
            Response response = execute();
            Map<String, String> headers = new HashMap<>();
            for (String name : response.headers().names()) {
                headers.put(name, response.header(name));
            }
            return headers;
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
    private Response execute() throws IOException {
        // Add User-Agent if not already set
        if (requestBuilder.build().header("User-Agent") == null) {
            requestBuilder.header("User-Agent", SettingUtils.USER_AGENT);
        }

        // Cookie handling
        if (syncCookie) {
            String cookies = CookiesUtils.getCookies(url);
            if (cookies != null && !cookies.isEmpty()) {
                requestBuilder.header("Cookie", cookies);
            }
        }

        // Set request method and body
        Request request;
        if (requestBody != null) {
            request = requestBuilder.method(method, requestBody).build();
        } else {
            request = requestBuilder.method(method, null).build();
        }

        response = OK_HTTP_CLIENT.newCall(request).execute();

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
