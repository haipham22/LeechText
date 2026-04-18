package dark.leech.text.util;

import java.awt.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class providing core application functionality and constants.
 *
 * <p>AppUtils serves as a central repository for application-wide constants, configuration loading,
 * and utility methods. It handles the initialization of syntax rules, display settings, and
 * provides helper methods for common operations like pausing execution and managing application
 * location.
 *
 * <p>The class is designed as a utility class with static methods and constants, making it easily
 * accessible throughout the application without instantiation.
 *
 * @author Long
 * @version 2019.03.30
 * @since 1.0
 * @see SyntaxUtils
 * @see FileUtils
 */
public class AppUtils {

    /** Current version of the application */
    public static final String VERSION = "2019.03.30";

    /** Default time string used for initialization */
    public static final String TIME = "00:00";

    /** System-specific file separator character */
    public static final String SEPARATOR = System.getProperty("file.separator");

    /** Graphics device for screen information */
    private static final GraphicsDevice gd =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    /** Width of the primary display in pixels */
    public static final int width = gd.getDisplayMode().getWidth();

    /** Height of the primary display in pixels */
    public static final int height = gd.getDisplayMode().getHeight();

    /** Current working directory of the application */
    public static String curDir = getAppHomeDir();

    /** Directory for caching application data */
    public static String cacheDir = curDir;

    /**
     * Determines the appropriate home directory for application data.
     *
     * <p>This method detects whether the application is running as a native app (via jpackage) or
     * as a standard JAR, and returns the appropriate directory for storing user data:
     *
     * <ul>
     *   <li><b>Native App:</b> Uses user's home directory ({@code ~/.leechtext})
     *   <li><b>JAR:</b> Uses current working directory (for backward compatibility)
     * </ul>
     *
     * <p>This is critical for jpackaged apps because Program Files and app bundles are read-only.
     *
     * @return The appropriate home directory path for storing application data
     */
    private static String getAppHomeDir() {
        // Detect if running as native app (jpackage sets java.app.name)
        boolean isNativeApp = System.getProperty("java.app.name") != null;

        if (isNativeApp) {
            // Native app: use user home directory
            // This ensures config is writable across all platforms
            String userHome = System.getProperty("user.home");
            return userHome + "/.leechtext";
        } else {
            // JAR: preserve existing behavior (current directory)
            return System.getProperty("user.dir");
        }
    }

    /** Current location of the application window */
    public static Point LOCATION = new Point();

    /** Private constructor to prevent instantiation of utility class. */
    private AppUtils() {}

    /**
     * Loads and initializes application configuration and syntax rules.
     *
     * <p>This method performs the following initialization tasks:
     *
     * <ul>
     *   <li>Normalizes the current directory path
     *   <li>Loads syntax configuration from JSON resources
     *   <li>Initializes chapter and part name patterns
     *   <li>Sets up text optimization and replacement rules
     * </ul>
     *
     * <p>The method loads configuration from the embedded syntax.json resource file and configures
     * the SyntaxUtils class with the loaded patterns and replacement rules.
     *
     * @throws RuntimeException if configuration loading fails (currently silently handled)
     */
    public static void doLoad() {
        try {
            // Normalize current directory path
            if (curDir.endsWith(SEPARATOR)) curDir = curDir.substring(0, curDir.length() - 1);

            // Load syntax configuration from JSON
            JSONObject json =
                    new JSONObject(FileUtils.stream2string("/dark/leech/res/syntax.json"));

            // Extract chapter and part name patterns
            JSONObject find = json.getJSONObject("find");
            SyntaxUtils.CHAP_NAME = find.getString("chap");
            SyntaxUtils.PART_NAME = find.getString("part");

            // Load text optimization rules
            JSONArray optimize = json.getJSONArray("optimize");
            String[] REPLACE_FROM = new String[optimize.length()];
            String[] REPLACE_TO = new String[optimize.length()];

            for (int i = 0; i < optimize.length(); i++) {
                REPLACE_FROM[i] = optimize.getJSONObject(i).getString("replace");
                REPLACE_TO[i] = optimize.getJSONObject(i).getString("to");
            }

            // Configure syntax utilities with loaded rules
            SyntaxUtils.REPLACE_FROM = REPLACE_FROM;
            SyntaxUtils.REPLACE_TO = REPLACE_TO;

        } catch (Exception e) {
            // Silently handle configuration loading errors
        }
    }

    /**
     * Gets the X coordinate of the application's current location.
     *
     * @return The X coordinate of the application window
     */
    public static int getX() {
        return LOCATION.x;
    }

    /**
     * Gets the Y coordinate of the application's current location.
     *
     * @return The Y coordinate of the application window
     */
    public static int getY() {
        return LOCATION.y;
    }

    /**
     * Gets the current location of the application window.
     *
     * @return A Point object representing the current window location
     */
    public static Point getLocation() {
        return LOCATION;
    }

    /**
     * Pauses the current thread execution for the specified duration.
     *
     * <p>This utility method provides a simple way to pause execution without having to handle
     * InterruptedException in calling code. The method silently handles interruption exceptions.
     *
     * @param milliseconds The number of milliseconds to pause
     */
    public static void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // Silently handle thread interruption
        }
    }
}
