package dark.leech.text.plugin.js.api;

import java.time.Duration;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

import dark.leech.text.action.Log;

/**
 * Browser automation API for JavaScript plugins. Provides headless browser capabilities for
 * scraping dynamic content.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * const browser = Engine.newBrowser();
 * browser.launch("https://example.com");
 * const title = browser.callJs("return document.title;");
 * browser.close();
 * }</pre>
 */
public class Browser extends JsApiWrapper {

    private WebDriver driver;
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /** Create Browser instance without context (factory method). */
    public Browser() {
        super();
    }

    /**
     * Create Browser instance with execution context.
     *
     * @param context The Rhino context
     * @param scope The Rhino scope
     */
    public Browser(Context context, Scriptable scope) {
        super(context, scope);
    }

    /**
     * Launch browser and navigate to URL. Returns this Browser instance for chaining.
     *
     * @param url The URL to navigate to
     * @return This Browser instance
     */
    public Browser launch(String url) {
        if (url == null || url.isEmpty()) {
            Log.add("[Browser.launch()] URL is null or empty");
            return this;
        }

        try {
            Log.add("[Browser.launch()] Launching browser with URL: " + url);

            // Setup ChromeDriver
            WebDriverManager.chromedriver().setup();

            // Configure Chrome for headless mode
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--user-agent=" + dark.leech.text.util.SettingUtils.USER_AGENT);
            options.setImplicitWaitTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
            options.setPageLoadTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
            options.setScriptTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            // Create driver
            driver = new ChromeDriver(options);

            // Navigate to URL
            driver.get(url);

            Log.add(
                    "[Browser.launch()] Browser launched successfully, page title: "
                            + driver.getTitle());

        } catch (Exception e) {
            Log.add("[Browser.launch()] Failed to launch browser: " + e.getMessage());
            driver = null;
        }

        return this;
    }

    /**
     * Set custom user agent for browser.
     *
     * @param userAgent The user agent string
     * @return This Browser instance
     */
    public Browser setUserAgent(String userAgent) {
        if (driver == null) {
            Log.add("[Browser.setUserAgent()] Browser not launched");
            return this;
        }

        try {
            Log.add("[Browser.setUserAgent()] Setting user agent: " + userAgent);

            // Execute JavaScript to set user agent (note: this may not work in headless mode)
            ((JavascriptExecutor) driver)
                    .executeScript(
                            "Object.defineProperty(navigator, 'userAgent', {get: function() {"
                                    + " return '"
                                    + userAgent
                                    + "'; }});");

        } catch (Exception e) {
            Log.add("[Browser.setUserAgent()] Failed to set user agent: " + e.getMessage());
        }

        return this;
    }

    /**
     * Execute JavaScript in browser context and return result.
     *
     * @param script The JavaScript code to execute
     * @return Execution result or null on error
     */
    public Object callJs(String script) {
        if (driver == null) {
            Log.add("[Browser.callJs()] Browser not launched");
            return null;
        }

        if (script == null || script.isEmpty()) {
            Log.add("[Browser.callJs()] Script is null or empty");
            return null;
        }

        try {
            Log.add(
                    "[Browser.callJs()] Executing script: "
                            + (script.length() > 100 ? script.substring(0, 100) + "..." : script));

            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
            Object result = jsExecutor.executeScript(script);

            // Convert result to Java type
            return toJava(result);

        } catch (Exception e) {
            Log.add("[Browser.callJs()] Failed to execute script: " + e.getMessage());
            return null;
        }
    }

    /**
     * Wait for specified number of milliseconds.
     *
     * @param millis Milliseconds to wait
     * @return This Browser instance
     */
    public Browser sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * Get current page HTML as JSDocument.
     *
     * @return JSDocument of current page or empty document on error
     */
    public JSDocument html() {
        if (driver == null) {
            Log.add("[Browser.html()] Browser not launched");
            return new JSDocument(org.jsoup.Jsoup.parse(""));
        }

        try {
            String html = driver.getPageSource();
            return new JSDocument(org.jsoup.Jsoup.parse(html));

        } catch (Exception e) {
            Log.add("[Browser.html()] Failed to get HTML: " + e.getMessage());
            return new JSDocument(org.jsoup.Jsoup.parse(""));
        }
    }

    /**
     * Get current page title.
     *
     * @return Page title or empty string on error
     */
    public String title() {
        if (driver == null) {
            Log.add("[Browser.title()] Browser not launched");
            return "";
        }

        try {
            return driver.getTitle();
        } catch (Exception e) {
            Log.add("[Browser.title()] Failed to get title: " + e.getMessage());
            return "";
        }
    }

    /**
     * Get current URL.
     *
     * @return Current URL or empty string on error
     */
    public String url() {
        if (driver == null) {
            Log.add("[Browser.url()] Browser not launched");
            return "";
        }

        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            Log.add("[Browser.url()] Failed to get URL: " + e.getMessage());
            return "";
        }
    }

    /** Close browser and cleanup resources. */
    public void close() {
        if (driver != null) {
            try {
                Log.add("[Browser.close()] Closing browser");
                driver.quit();
            } catch (Exception e) {
                Log.add("[Browser.close()] Failed to close browser: " + e.getMessage());
            } finally {
                driver = null;
            }
        }
    }

    /**
     * Check if browser is still active.
     *
     * @return true if browser is active, false otherwise
     */
    public boolean isActive() {
        if (driver == null) {
            return false;
        }

        try {
            driver.getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
