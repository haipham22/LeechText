package dark.leech.text.plugin.js.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;
import dark.leech.text.util.SettingUtils;

/**
 * HTML API for JavaScript plugins using Rhino. Provides HTML parsing, selection, URL encoding, and
 * element manipulation. Simple Java class - public methods automatically exposed to JavaScript.
 */
public class Html extends JsApiWrapper {

    private static final org.jsoup.nodes.Document EMPTY_DOC =
            org.jsoup.Jsoup.parse("<html><body></body></html>");

    /**
     * Create Html API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Html(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Html API without context (legacy compatibility). */
    public Html() {
        super();
    }

    /**
     * Parse HTML string into JSDocument wrapper. Usage: html.parse("<html>...</html>") Returns
     * empty document wrapper for null/empty input (vBook compatibility).
     */
    public JSDocument parse(String html) {
        if (html == null || html.isEmpty()) {
            return new JSDocument(EMPTY_DOC);
        }
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
        return new JSDocument(doc);
    }

    /**
     * Parse HTML from URL. Usage: html.parseUrl("https://example.com") Returns empty document
     * wrapper on failure (vBook compatibility).
     */
    public JSDocument parseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return new JSDocument(EMPTY_DOC);
        }
        try {
            org.jsoup.nodes.Document doc =
                    org.jsoup.Jsoup.connect(url)
                            .userAgent(SettingUtils.USER_AGENT)
                            .timeout(SettingUtils.TIMEOUT)
                            .get();
            return new JSDocument(doc);
        } catch (Exception e) {
            Log.add("Failed to parse HTML from URL: " + e.getMessage());
            return new JSDocument(EMPTY_DOC);
        }
    }

    /** URL encode a string. Usage: html.urlEncode("hello world") => "hello+world" */
    public String urlEncode(String url) {
        return urlEncode(url, StandardCharsets.UTF_8.name());
    }

    public String urlEncode(String url, String charset) {
        try {
            return URLEncoder.encode(url, charset);
        } catch (Exception e) {
            Log.add("URL encoding failed: " + e.getMessage());
            return url;
        }
    }

    /** URL decode a string. */
    public String urlDecode(String url) {
        return urlDecode(url, StandardCharsets.UTF_8.name());
    }

    public String urlDecode(String url, String charset) {
        try {
            return java.net.URLDecoder.decode(url, charset);
        } catch (Exception e) {
            Log.add("URL decoding failed: " + e.getMessage());
            return url;
        }
    }

    /**
     * Clean HTML by removing specified tags. Usage: html.clean("<html>...</html>", ["script",
     * ".ads"])
     */
    public static String clean(String html, Object[] tags) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);

        if (tags != null) {
            for (Object tag : tags) {
                if (tag != null) {
                    String tagStr = org.mozilla.javascript.Context.toString(tag);
                    if (tagStr != null && !tagStr.isEmpty()) {
                        doc.select(tagStr).remove();
                    }
                }
            }
        }
        return doc.body().html();
    }

    /** Create JSElement wrapper from JSoup Element. */
    public static JSElement wrapElement(Element element) {
        return element != null ? new JSElement(element) : null;
    }

    /** Create JSElements wrapper from JSoup Elements. */
    public static JSElements wrapElements(Elements elements) {
        return new JSElements(elements);
    }

    /** Parse and select in one call. Usage: html.select("<html>...</html>", "div.class") */
    public JSElements select(String html, String selector) {
        JSDocument doc = parse(html);
        return doc.select(selector);
    }
}
