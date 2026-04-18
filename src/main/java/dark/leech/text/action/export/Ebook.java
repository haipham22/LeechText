package dark.leech.text.action.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import dark.leech.text.action.Log;
import dark.leech.text.listeners.ProgressListener;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.notification.Alert;
import dark.leech.text.util.*;

public class Ebook {
    private final Properties properties;
    private boolean autoSplit;
    private String compressLevel;
    private String tool;
    private boolean includeImg;
    private int type;
    private ProgressListener progressListener;

    public Ebook(Properties properties) {
        this.properties = properties;
    }

    public void setData(
            int type, String tool, String compressLevel, boolean autoSplit, boolean includeImg) {
        this.type = type;
        this.tool = tool;
        this.compressLevel = compressLevel;
        this.autoSplit = autoSplit;
        this.includeImg = includeImg;
    }

    public void export() {
        switch (type) {
            case TypeUtils.EPUB:
                if (tool.equals("Calibre")) {
                    tool = SettingUtils.CALIBRE;
                    if (!checkTool(tool)) {
                        error();
                        return;
                    }
                }
                break;
            case TypeUtils.PDF:
                tool = SettingUtils.CALIBRE;
                if (!checkTool(tool)) {
                    error();
                    return;
                }
                break;
            default:
                break;
        }
        FileUtils.string2file(
                SettingUtils.CSS_SYNTAX, properties.getSavePath() + "/data/stylesheet.css");
        progressListener.setProgress(0, "[1/3]Xuất Text...");
        Text text = new Text(properties, TypeUtils.HTML, false, false, 0);
        text.addProgressListener(progressListener);
        text.export();
        progressListener.setProgress(10, "[2/3]Tạo mục lục...");
        ToC toC = new ToC(properties);
        toC.setAutoSplit(autoSplit);
        toC.setIncludeImg(includeImg);
        toC.mkToC();
        progressListener.setProgress(14, "[3/3]Tạo Ebook...");
        switch (type) {
            case TypeUtils.EPUB:
                exportEpub(tool);
                break;
            case TypeUtils.PDF:
                exportPdf();
                break;
            default:
                break;
        }
    }

    private void error() {
        Alert.show(
                "Đường dẫn Calibre (ebook-convert.exe) không hợp lệ!\n"
                        + "Xem lại thiết lập trong cài đặt!");
    }

    private void exportEpub(String tool) {
        if (tool.equals("Mặc định")) {
            try {
                exportEpub();
            } catch (Exception e) {
                Log.add(e);
            }
        } else {
            String fileName = properties.getName() + " - " + properties.getAuthor() + ".epub";
            fileName = fileName.replaceAll("[:/?*]", "");
            String cmd =
                    tinyCmd(tool)
                            + " "
                            + tinyCmd(properties.getSavePath() + "/data/content.opf")
                            + " "
                            + tinyCmd(properties.getSavePath() + "/out/" + fileName);
            runCmd(cmd);
        }
    }

    private void exportEpub() {
        InputStream in = getClass().getResourceAsStream("/dark/leech/res/untitled.epub");
        String fileName = properties.getName() + " - " + properties.getAuthor() + ".epub";
        fileName = fileName.replaceAll("[:/?*]", "");
        fileName = properties.getSavePath() + "/out/" + fileName;
        FileUtils.byte2file(FileUtils.stream2byte(in), fileName);
        ZipUtils.setDefaultCompressionLevel(Integer.parseInt(compressLevel));
        ZipUtils.addFolders(fileName, properties.getSavePath() + "/data/Text", "");
        progressListener.setProgress(75, "(3/3)Tạo Ebook...");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/content.opf");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/toc.ncx");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/stylesheet.css");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/cover.jpg");
        if (includeImg)
            ZipUtils.addFolders(fileName, properties.getSavePath() + "/data/Images", "");
        progressListener.setProgress(100, "Hoàn tất!");
    }

    private void exportPdf() {
        tool = SettingUtils.CALIBRE;
        String fileName = properties.getName() + " - " + properties.getAuthor() + ".pdf";
        fileName = fileName.replaceAll("[:/?*]", "");
        String cmd =
                tinyCmd(tool)
                        + " "
                        + tinyCmd(properties.getSavePath() + "/data/content.opf")
                        + " "
                        + tinyCmd(properties.getSavePath() + "/out/" + fileName)
                        + " --paper-size="
                        + compressLevel;
        runCmd(cmd);
    }

    public void createToc() {
        ToC tableOfContent = new ToC(properties);
        tableOfContent.setAutoSplit(autoSplit);
        tableOfContent.setIncludeImg(includeImg);
        tableOfContent.mkToC();
    }

    private boolean checkTool(String tool) {
        boolean b = Tool(tool);
        if (b) createToc();
        return b;
    }

    private boolean Tool(String tool) {
        if (tool == null) return false;
        if (tool.length() < 2) return false;
        File file = new File(tool);
        if (!file.exists()) return false;
        if (file.isDirectory()) return false;
        // Cross-platform: check executable on Unix/macOS, .exe on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return tool.endsWith(".exe");
        } else {
            return file.canExecute();
        }
    }

    private void runCmd(String cmd) {
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmd);
            InputStream s = p.getInputStream();

            BufferedReader in;
            in = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8));
            String temp;
            int percent = 0;
            while ((temp = in.readLine()) != null) {
                String pe = RegexUtils.find(temp, "(^\\d+)%", 1);
                if (pe != null) percent = Integer.parseInt(pe);
                progressListener.setProgress(percent, "[3/3]" + temp.replaceAll("[\n\r]", " "));
            }
            progressListener.setProgress(100, "Hoàn tất!");

        } catch (Exception e) {
            Log.add(e);
        }
    }

    private String tinyCmd(String cmd) {
        if (cmd.contains(" ")) cmd = "\"" + cmd + "\"";
        return FileUtils.validate(cmd);
    }

    public void addProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }
}
