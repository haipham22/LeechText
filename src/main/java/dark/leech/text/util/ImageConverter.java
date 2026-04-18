package dark.leech.text.util;

import java.io.*;

/**
 * Utility class for converting images between formats. Handles WebP to JPEG conversion for EPUB
 * compatibility.
 *
 * <p>Uses external tools (ffmpeg/ImageMagick) for conversion when available, with fallback to save
 * as-is when tools are not available.
 */
public class ImageConverter {

    private ImageConverter() {}

    /**
     * Detect if image data is WebP format by checking magic bytes. WebP files start with
     * "RIFF"...."WEBP"
     */
    public static boolean isWebP(byte[] imageData) {
        if (imageData == null || imageData.length < 12) {
            return false;
        }
        // Check RIFF header (bytes 0-3)
        if (imageData[0] != 'R'
                || imageData[1] != 'I'
                || imageData[2] != 'F'
                || imageData[3] != 'F') {
            return false;
        }
        // Check WEBP marker (bytes 8-11)
        if (imageData[8] != 'W'
                || imageData[9] != 'E'
                || imageData[10] != 'B'
                || imageData[11] != 'P') {
            return false;
        }
        return true;
    }

    /**
     * Download image from URL and convert to JPEG if it's WebP format. Saves to specified path with
     * .jpg extension.
     */
    public static void downloadAndConvertToJpeg(String url, String savePath) {
        try {
            // First, download to a temporary file
            String tempPath = savePath + ".tmp";
            FileUtils.url2file(url, tempPath);

            // Check if it's WebP
            byte[] imageData = FileUtils.file2byte(tempPath);
            if (isWebP(imageData)) {
                // Convert WebP to JPEG
                convertWebPToJpeg(tempPath, savePath);
                // Delete temp file
                new File(tempPath).delete();
            } else {
                // Not WebP, just rename
                new File(tempPath).renameTo(new File(savePath));
            }
        } catch (Exception e) {
            dark.leech.text.action.Log.add(e);
        }
    }

    /**
     * Convert WebP file to JPEG using external tools. Tries ffmpeg, then ImageMagick convert, then
     * falls back to renaming.
     */
    private static void convertWebPToJpeg(String webpPath, String jpegPath) {
        boolean success = false;

        // Try ffmpeg first (most reliable)
        success = tryConvertWithFfmpeg(webpPath, jpegPath);

        // Try ImageMagick if ffmpeg failed
        if (!success) {
            success = tryConvertWithImageMagick(webpPath, jpegPath);
        }

        // Fallback: just rename the file (not ideal but prevents data loss)
        if (!success) {
            dark.leech.text.action.Log.add(
                    "Warning: Could not convert WebP to JPEG (ffmpeg/ImageMagick not available)."
                            + " Saving WebP with .jpg extension - EPUB readers may not display it"
                            + " correctly.");
            FileUtils.copyFile(webpPath, jpegPath);
        }
    }

    /** Try converting WebP to JPEG using ffmpeg. */
    private static boolean tryConvertWithFfmpeg(String webpPath, String jpegPath) {
        try {
            // Check if ffmpeg is available
            String os = System.getProperty("os.name").toLowerCase();
            String ffmpegCmd = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";

            ProcessBuilder pb =
                    new ProcessBuilder(
                            ffmpegCmd,
                            "-i",
                            webpPath,
                            "-qscale:v",
                            "2", // High quality JPEG
                            "-y", // Overwrite output file
                            jpegPath);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Wait for completion
            int exitCode = process.waitFor();
            return exitCode == 0 && new File(jpegPath).exists();

        } catch (Exception e) {
            // ffmpeg not available or failed
            return false;
        }
    }

    /** Try converting WebP to JPEG using ImageMagick convert. */
    private static boolean tryConvertWithImageMagick(String webpPath, String jpegPath) {
        try {
            // Check if convert is available
            String os = System.getProperty("os.name").toLowerCase();
            String convertCmd = os.contains("win") ? "convert.exe" : "convert";

            ProcessBuilder pb =
                    new ProcessBuilder(
                            convertCmd,
                            webpPath,
                            "-quality",
                            "95", // High quality JPEG
                            jpegPath);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Wait for completion
            int exitCode = process.waitFor();
            return exitCode == 0 && new File(jpegPath).exists();

        } catch (Exception e) {
            // ImageMagick not available or failed
            return false;
        }
    }

    /** Convert an existing WebP file to JPEG format. */
    public static boolean convertWebPFileToJpeg(String webpPath, String jpegPath) {
        byte[] webpData = FileUtils.file2byte(webpPath);
        if (webpData == null) {
            return false;
        }

        if (!isWebP(webpData)) {
            // Not WebP, just copy
            FileUtils.copyFile(webpPath, jpegPath);
            return true;
        }

        convertWebPToJpeg(webpPath, jpegPath);
        return true;
    }
}
