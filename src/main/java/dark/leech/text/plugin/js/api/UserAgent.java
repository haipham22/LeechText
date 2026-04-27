package dark.leech.text.plugin.js.api;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * UserAgent API for JavaScript plugins. Provides preset user agent strings for different platforms.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * http.get(url)
 *     .headers({"User-Agent": UserAgent.android()})
 *     .string();
 * }</pre>
 */
public final class UserAgent extends JsApiWrapper {

    // Current versions as of 2025
    private static final String ANDROID_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Mobile Safari/537.36";

    private static final String IOS_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Safari/537.36";

    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/131.0.0.0 Mobile Safari/537.36";

    public UserAgent(Context context, Scriptable scope) {
        super(context, scope);
    }

    /**
     * Get Android Chrome user agent string.
     *
     * @return Android user agent
     */
    public static String android() {
        return ANDROID_UA;
    }

    /**
     * Get iOS Safari user agent string.
     *
     * @return iOS user agent
     */
    public static String iOS() {
        return IOS_UA;
    }

    /**
     * Get Desktop Chrome user agent string.
     *
     * @return Desktop user agent
     */
    public static String desktop() {
        return DESKTOP_UA;
    }

    /**
     * Get generic mobile user agent string.
     *
     * @return Mobile user agent
     */
    public static String mobile() {
        return MOBILE_UA;
    }
}
