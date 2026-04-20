package dark.leech.text.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import dark.leech.text.action.Log;

/** Created by Dark on 1/12/2017. */
public class Http {
    private Request.Builder requestBuilder;
    private Response response;
    private final String url;
    private RequestBody requestBody;
    private String responseBody; // Cache response body for multiple reads

    // OkHttp3 client with lenient SSL configuration (matches vBooks Android)
    private static final OkHttpClient OK_HTTP_CLIENT =
            new OkHttpClient.Builder()
                    .connectTimeout(SettingUtils.TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(SettingUtils.TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(SettingUtils.TIMEOUT, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .sslSocketFactory(
                            SSLUtils.createLenientSSLContext().getSocketFactory(),
                            (javax.net.ssl.X509TrustManager)
                                    SSLUtils.createLenientTrustManager()[0])
                    .hostnameVerifier(SSLUtils.createLenientHostnameVerifier())
                    .build();

    private Http(String url) {
        this.url = url;
        this.requestBuilder = new Request.Builder().url(url);
    }

    public static HttpConnection connect(String url) {
        return new HttpConnection(url);
    }

    public static Document get(String url) {
        try {
            String html = connect(url).string();
            return Jsoup.parse(html, url);
        } catch (Exception e) {
            Log.add(e);
            return null;
        }
    }

    public static Http request(String url) {
        return new Http(url);
    }

    public Http cookie(String cookie) {
        requestBuilder.header("Cookie", cookie);
        return this;
    }

    public String cookies() {
        try {
            if (response == null) {
                execute();
            }
            return response.header("Set-Cookie");
        } catch (Exception e) {
            return null;
        }
    }

    public Http data(String name, String value) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (requestBody instanceof FormBody) {
            // Preserve existing form data
            // Note: OkHttp3 FormBody is immutable, so we need to rebuild
        }
        formBuilder.add(name, value);
        requestBody = formBuilder.build();
        return this;
    }

    public Http data(Map<String, String> data) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        requestBody = formBuilder.build();
        return this;
    }

    public Http data(String... args) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (int i = 0; i < args.length - 1; i += 2) {
            formBuilder.add(args[i], args[i + 1]);
        }
        requestBody = formBuilder.build();
        return this;
    }

    public Http header(String name, String value) {
        requestBuilder.header(name, value);
        return this;
    }

    private void execute() {
        try {
            if (response == null) {
                Request request;
                if (requestBody != null) {
                    request = requestBuilder.post(requestBody).build();
                } else {
                    request = requestBuilder.get().build();
                }
                response = OK_HTTP_CLIENT.newCall(request).execute();

                // Cache response body for multiple reads
                responseBody = response.body().string();

                // Sync response cookies
                String setCookie = response.header("Set-Cookie");
                if (setCookie != null) {
                    CookiesUtils.put(url, setCookie);
                }
            }
        } catch (IOException e) {
            Log.add(e);
        }
    }

    public String string() {
        execute();
        return responseBody;
    }

    public Document document() {
        execute();
        try {
            if (responseBody != null) {
                return Jsoup.parse(responseBody, url);
            }
        } catch (Exception e) {
            Log.add(e);
        }
        return null;
    }

    public JSONObject json() {
        execute();
        try {
            if (responseBody != null) return new JSONObject(responseBody);
        } catch (Exception e) {
            Log.add(e);
        }
        return null;
    }

    public byte[] bytes() {
        execute();
        try {
            if (responseBody != null) return responseBody.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.add(e);
        }
        return null;
    }

    /** Wrapper class to maintain backward compatibility with existing API. */
    public static class HttpConnection {
        private final String url;
        private final Request.Builder requestBuilder;
        private Response response;

        HttpConnection(String url) {
            this.url = url;
            this.requestBuilder =
                    new Request.Builder()
                            .url(url)
                            .header("User-Agent", SettingUtils.USER_AGENT)
                            .get();

            // Add cookies if available
            String cookies = CookiesUtils.getCookies(url);
            if (cookies != null) {
                requestBuilder.header("Cookie", cookies);
            }
        }

        public HttpConnection execute() {
            try {
                if (response == null) {
                    Request request = requestBuilder.build();
                    response = OK_HTTP_CLIENT.newCall(request).execute();

                    // Sync response cookies
                    String setCookie = response.header("Set-Cookie");
                    if (setCookie != null) {
                        CookiesUtils.put(url, setCookie);
                    }
                }
                return this;
            } catch (IOException e) {
                Log.add(e);
                return this;
            }
        }

        public byte[] bodyAsBytes() {
            try {
                if (response == null) {
                    execute();
                }
                return response != null ? response.body().bytes() : null;
            } catch (IOException e) {
                Log.add(e);
                return null;
            }
        }

        public String string() {
            try {
                if (response == null) {
                    execute();
                }
                return response != null ? response.body().string() : null;
            } catch (IOException e) {
                Log.add(e);
                return null;
            }
        }
    }
}
