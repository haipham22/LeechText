package dark.leech.text.ui.main;

import java.awt.*;

import javax.swing.*;

import dark.leech.text.ui.Animation;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;

/**
 * Main application class for LeechText.
 *
 * <p>LeechText is a text extraction and ebook creation application that allows users to download
 * content from various sources and convert them into different formats. This class serves as the
 * entry point and manages the main application lifecycle.
 *
 * <p>The application initializes the UI components, loads settings and utilities, and manages the
 * main user interface frame. It runs the main UI in a separate thread to ensure proper
 * initialization and smooth startup experience.
 *
 * @author LeechText Development Team
 * @version 1.0
 * @since 1.0
 */
public class App {

    /** The main user interface frame of the application */
    private static MainUI mainFrame;

    /** Counter for tracking how many times the application has been opened */
    private static int openCount;

    /** Counter for tracking how many times the application has been closed */
    private static int closeCount;

    /**
     * Main entry point for the LeechText application.
     *
     * <p>This method initializes the application by:
     *
     * <ul>
     *   <li>Setting up the Swing look and feel
     *   <li>Loading application utilities and settings
     *   <li>Initializing file utilities
     *   <li>Creating and displaying the main user interface
     *   <li>Applying fade-in animation for smooth startup
     * </ul>
     *
     * <p>The initialization is performed in a separate thread to prevent blocking the main thread
     * and ensure responsive UI behavior.
     *
     * @param args Command line arguments (currently not used)
     */
    public static void main(String[] args) {

        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Set the look and feel to Metal for consistent cross-platform
                                    // appearance
                                    UIManager.setLookAndFeel(
                                            "javax.swing.plaf.metal.MetalLookAndFeel");
                                } catch (Exception ex) {
                                    // Silently handle look and feel exceptions
                                }

                                // Initialize core application components
                                AppUtils.doLoad();
                                FileUtils.init();
                                SettingUtils.doLoad();

                                // Create and display main UI
                                mainFrame = new MainUI();
                                Animation.fadeIn(mainFrame);
                                mainFrame.setVisible(true);

                                // Store application location for future reference
                                AppUtils.LOCATION =
                                        new Point(
                                                mainFrame.getLocation().x,
                                                mainFrame.getLocation().y + 20);
                            }
                        })
                .start();
    }

    /**
     * Gets the main user interface instance.
     *
     * <p>This method provides access to the main UI frame, allowing other components to interact
     * with the primary application window.
     *
     * @return The main user interface instance, or null if not yet initialized
     */
    public static MainUI getMain() {
        return mainFrame;
    }
}
