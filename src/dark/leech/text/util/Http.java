package dark.leech.text.util;

import dark.leech.text.action.Log;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Dark on 1/12/2017.
 */
public class Http {
    private Connection connection;
    private Connection.Response response;

    private Http(String url) {
        connection = connect(url);
    }

    public static Connection connect(String url) {
        try {
            String cookies = CookiesUtils.getCookies(url);
            if (cookies == null)
                return Jsoup.connect(url)
                        .userAgent(SettingUtils.USER_AGENT)
                        .ignoreContentType(true)
                        .followRedirects(true)
                        .maxBodySize(0)
                        .timeout(SettingUtils.TIMEOUT);
            else return Jsoup.connect(url)
                    .userAgent(SettingUtils.USER_AGENT)
                    .header("Cookie", cookies)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .timeout(SettingUtils.TIMEOUT);
        } catch (Exception e) {
            Log.add(e);
            return null;
        }
    }


    public static Document get(String url) {
        try {
            return connect(url).get();
        } catch (IOException e) {
            Log.add(e);
            return null;
        }
    }

    public Http cookie(String cookie) {
        connection.header("Cookie", cookie);
        return this;
    }

    public String cookies() {
        return response.header("Set-Cookie");
    }

    public static Http request(String url) {
        return new Http(url);
    }

    public Http data(String name, String value) {
        connection.data(name, value);
        return this;
    }


    public Http data(Map<String, String> data) {
        connection.data(data);
        return this;
    }

    public Http data(String... args) {
        connection.data(args);
        return this;
    }


    public Http header(String name, String value) {
        connection.header(name, value);
        return this;
    }

    private void execute() {
        try {
            response = connection.execute();
        } catch (IOException e) {
        }
    }

    public String string() {
        execute();
        try {
            if (response != null)
                return response.body();
        } catch (Exception e) {
        }
        return null;
    }

    public Document document() {
        execute();
        try {
            if (response != null)
                return response.parse();
        } catch (Exception e) {
        }
        return null;
    }

    public JSONObject json() {
        execute();
        try {
            if (response != null)
                return new JSONObject(response.body());
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] bytes() {
        execute();
        try {
            if (response != null)
                return response.bodyAsBytes();
        } catch (Exception e) {
        }
        return null;
    }
}
