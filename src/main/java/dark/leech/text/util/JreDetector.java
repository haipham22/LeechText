package dark.leech.text.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for detecting and downloading Java Runtime Environment (JRE).
 *
 * <p>This class provides functionality to:
 *
 * <ul>
 *   <li>Detect if Java 17+ is installed on the system
 *   <li>Download portable JRE from Adoptium if needed
 *   <li>Cache downloaded JRE for future use
 * </ul>
 *
 * @author LeechText Team
 * @since 2026.04.18
 */
public class JreDetector {

    private static final String MIN_JAVA_VERSION = "17";
    private static final String JRE_CACHE_DIR = AppUtils.curDir + "/jre";
    private static final String ADOPTIUM_API_URL =
            "https://api.adoptium.net/v3/assets/latest/%s/hotspot?architecture=%s&image_type=jre&jvm_impl=hotspot&vendor=adoptium";

    /**
     * Checks if Java 17+ is available on the system.
     *
     * @return true if Java 17+ is found, false otherwise
     */
    public static boolean isJavaAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"java", "-version"});
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("version")) {
                    String version = extractVersion(line);
                    if (compareVersions(version, MIN_JAVA_VERSION) >= 0) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Java not found or error checking version
        }
        return false;
    }

    /**
     * Gets the path to the cached JRE or downloads it if not available.
     *
     * @return Path to JRE directory, or null if download fails
     */
    public static String getOrDownloadJre() {
        // Check if JRE is already cached
        File cachedJre = new File(JRE_CACHE_DIR);
        if (cachedJre.exists() && cachedJre.isDirectory()) {
            File javaBin = new File(cachedJre, getJavaBinaryPath());
            if (javaBin.exists()) {
                return JRE_CACHE_DIR;
            }
        }

        // Download JRE if not cached
        return downloadJre();
    }

    /**
     * Downloads portable JRE from Adoptium.
     *
     * @return Path to downloaded JRE, or null if download fails
     */
    private static String downloadJre() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            String platform;
            String architecture;

            if (os.contains("win")) {
                platform = "windows";
            } else if (os.contains("mac")) {
                platform = "mac";
            } else if (os.contains("nix") || os.contains("nux")) {
                platform = "linux";
            } else {
                return null; // Unsupported platform
            }

            if (arch.contains("64") || arch.equals("x86_64") || arch.equals("amd64")) {
                architecture = "x64";
            } else if (arch.contains("aarch64") || arch.equals("arm64")) {
                architecture = "aarch64";
            } else {
                return null; // Unsupported architecture
            }

            // Query Adoptium API for download URL
            String apiUrl = String.format(ADOPTIUM_API_URL, platform, architecture);
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                return null;
            }

            // Parse JSON response to get download URL
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extract download URL from JSON
            String downloadUrl = extractDownloadUrl(response.toString());
            if (downloadUrl == null) {
                return null;
            }

            // Download JRE archive
            File tempFile = File.createTempFile("jre", ".zip");
            downloadFile(downloadUrl, tempFile);

            // Extract JRE to cache directory
            File cacheDir = new File(JRE_CACHE_DIR);
            cacheDir.mkdirs();
            extractArchive(tempFile, cacheDir);

            tempFile.delete();

            return JRE_CACHE_DIR;

        } catch (Exception e) {
            return null; // Download failed
        }
    }

    /** Extracts Java version string from version output. */
    private static String extractVersion(String versionLine) {
        // Examples: "version \"17.0.1\"", "openjdk version \"17.0.1\""
        String[] parts = versionLine.split("\"");
        if (parts.length >= 2) {
            return parts[1].split("\\.")[0]; // Get major version
        }
        return "0";
    }

    /**
     * Compares two version strings.
     *
     * @return positive if v1 > v2, 0 if equal, negative if v1 < v2
     */
    private static int compareVersions(String v1, String v2) {
        try {
            int i1 = Integer.parseInt(v1.replaceAll("[^0-9]", ""));
            int i2 = Integer.parseInt(v2.replaceAll("[^0-9]", ""));
            return Integer.compare(i1, i2);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Gets the path to java binary for the current platform. */
    private static String getJavaBinaryPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "bin/java.exe";
        } else {
            return "bin/java";
        }
    }

    /** Extracts download URL from Adoptium API JSON response. */
    private static String extractDownloadUrl(String jsonResponse) {
        // Parse JSON to find package download URL
        // Simplified implementation - in production, use proper JSON parser
        int urlIndex = jsonResponse.indexOf("\"binary\":{\"package\":{\"link\":\"");
        if (urlIndex == -1) {
            return null;
        }
        int start = urlIndex + "\"binary\":{\"package\":{\"link\":\"".length();
        int end = jsonResponse.indexOf("\"", start);
        return jsonResponse.substring(start, end).replace("\\", "");
    }

    /** Downloads a file from URL to local file. */
    private static void downloadFile(String urlString, File destFile) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (var in = conn.getInputStream()) {
            Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Extracts archive to destination directory. */
    private static void extractArchive(File archive, File destDir) throws Exception {
        // Use external tools for extraction
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            pb =
                    new ProcessBuilder(
                            "tar",
                            "-xf",
                            archive.getAbsolutePath(),
                            "-C",
                            destDir.getAbsolutePath());
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            pb =
                    new ProcessBuilder(
                            "tar",
                            "-xf",
                            archive.getAbsolutePath(),
                            "-C",
                            destDir.getAbsolutePath());
        } else {
            throw new Exception("Unsupported platform for extraction");
        }

        Process process = pb.start();
        process.waitFor();
    }
}
