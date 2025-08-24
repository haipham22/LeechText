package dark.leech.text.util;

import dark.leech.text.action.Log;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

/**
 * Created by Long on 10/1/2016.
 * Updated to use zip4j library
 */
public class ZipUtils {
    private static CompressionLevel defaultCompressionLevel = CompressionLevel.NORMAL;
    
    private ZipUtils() {
    }

    public static void addFile(String zip, String file) {
        addFile(new File(FileUtils.validate(zip)), new File(FileUtils.validate(file)));
    }

    public static void addFile(File zip, File file) {
        addFile(zip, file, "");
    }

    public static void addFile(File zip, File file, String path) {
        if (!file.exists()) return;
        try {
            ZipFile zipFile = new ZipFile(zip);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(defaultCompressionLevel);
            
            if (path.length() > 0) {
                parameters.setRootFolderNameInZip(path);
            }
            
            zipFile.addFile(file, parameters);
        } catch (Exception e) {
            Log.add("Error adding file to zip: " + e.toString());
        }
    }

    public static void addFolders(String zip, String dir) {
        addFolders(zip, dir, "");
    }

    public static void addFolders(String zip, String dir, String path) {
        addFolders(new File(FileUtils.validate(zip)), new File(FileUtils.validate(dir)), path);
    }

    public static void addFolders(File zip, File dir) {
        addFolders(zip, dir, "");
    }

    public static void addFolders(File zip, File dir, String path) {
        try {
            ZipFile zipFile = new ZipFile(zip);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(defaultCompressionLevel);
            
            if (path.length() > 0) {
                parameters.setRootFolderNameInZip(path);
            }
            
            zipFile.addFolder(dir, parameters);
        } catch (Exception e) {
            Log.add("Error adding folder to zip: " + e.toString());
        }
    }

    public static byte[] readInZipAsByte(File zipfile, String filepath) {
        try {
            ZipFile zipFile = new ZipFile(zipfile);
            return zipFile.getInputStream(zipFile.getFileHeader(filepath)).readAllBytes();
        } catch (Exception e) {
            Log.add("Error reading from zip: " + e.toString());
            return new byte[0];
        }
    }

    public static String readInZipAsString(File zipfile, String filepath) {
        try {
            return new String(readInZipAsByte(zipfile, filepath), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.add(e.toString());
            return "";
        }
    }
    
    public static void setDefaultCompressionLevel(int level) {
        switch (level) {
            case 0:
                defaultCompressionLevel = CompressionLevel.NO_COMPRESSION;
                break;
            case 1:
                defaultCompressionLevel = CompressionLevel.FASTEST;
                break;
            case 6:
                defaultCompressionLevel = CompressionLevel.NORMAL;
                break;
            case 9:
                defaultCompressionLevel = CompressionLevel.MAXIMUM;
                break;
            default:
                defaultCompressionLevel = CompressionLevel.NORMAL;
        }
    }
}

