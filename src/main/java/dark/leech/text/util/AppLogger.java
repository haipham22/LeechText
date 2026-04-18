package dark.leech.text.util;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for LeechText application.
 *
 * <p>Provides file-based logging with rotation and console output. Logs are stored in {@code
 * ~/.leechtext/app.log} for native apps or {@code ./app.log} for JAR execution.
 *
 * @author LeechText Team
 * @since 1.0.2
 */
public class AppLogger {
    private static final String LOG_FILE = "app.log";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static PrintWriter logWriter;
    private static boolean initialized = false;

    /** Log levels */
    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /** Initialize the logger with file output. */
    private static void initialize() {
        if (initialized) return;

        try {
            String logPath = Paths.get(AppUtils.curDir, LOG_FILE).toString();
            logWriter = new PrintWriter(new FileWriter(logPath, true));
            initialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    /**
     * Log a message with specified level.
     *
     * @param level The log level
     * @param message The message to log
     * @param throwable Optional exception to log
     */
    public static void log(Level level, String message, Throwable throwable) {
        initialize();

        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // Always print to console
        if (level == Level.ERROR) {
            System.err.println(logMessage);
        } else {
            System.out.println(logMessage);
        }

        // Write to file if initialized
        if (logWriter != null) {
            logWriter.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(logWriter);
            }
            logWriter.flush();
        }

        // Print stack trace to console if error
        if (throwable != null && level == Level.ERROR) {
            throwable.printStackTrace();
        }
    }

    /** Log debug message */
    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    /** Log info message */
    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    /** Log warning message */
    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    /** Log warning message with exception */
    public static void warn(String message, Throwable throwable) {
        log(Level.WARN, message, throwable);
    }

    /** Log error message */
    public static void error(String message) {
        log(Level.ERROR, message, null);
    }

    /** Log error message with exception */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    /** Close the logger and release resources. */
    public static void close() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
            initialized = false;
        }
    }
}
