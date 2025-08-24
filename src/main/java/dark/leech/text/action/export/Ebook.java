package dark.leech.text.action.export;

import dark.leech.text.action.Log;
import dark.leech.text.listeners.ProgressListener;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.notification.Alert;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.RegexUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.TypeUtils;
import dark.leech.text.util.ZipUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
                        error(1);
                        return;
                    }
                }
                break;
            case TypeUtils.MOBI:
                if (tool.equals("Calibre")) {
                    tool = SettingUtils.CALIBRE;
                    if (!checkTool(tool)) {
                        error(1);
                        return;
                    }
                } else {
                    tool = SettingUtils.KINDLEGEN;
                    if (!checkTool(tool)) {
                        error(2);
                        return;
                    }
                }
                break;
            case TypeUtils.AZW3:
                tool = SettingUtils.CALIBRE;
                if (!checkTool(tool)) {
                    error(1);
                    return;
                }
                break;
            case TypeUtils.PDF:
                tool = SettingUtils.CALIBRE;
                if (!checkTool(tool)) {
                    error(1);
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
            case TypeUtils.MOBI:
                exportMobi(tool);
                break;
            case TypeUtils.AZW3:
                exportAzw3();
                break;
            case TypeUtils.PDF:
                exportPdf();
                break;
            default:
                break;
        }
    }

    private void error(int stt) {
        if (stt == 1)
            Alert.show(
                    "Đường dẫn Calibre (ebook-convert.exe) không hợp lệ!\n"
                            + "Xem lại thiết lập trong cài đặt!");
        else
            Alert.show(
                    "Đường dẫn Kindlegen (Kindlegen.exe) không hợp lệ!\n"
                            + "Xem lại thiết lập trong cài đặt!");
    }

    private void exportEpub(String tool) {
        if (tool.equals("Mặc định"))
            try {
                exportEpub();
            } catch (Exception e) {
            }
        else {
            String fileName = properties.getName() + " - " + properties.getAuthor() + ".epub";
            fileName = fileName.replaceAll("[:/\\?\\*]", "");
            String cmd =
                    tinyCmd(tool)
                            + " "
                            + tinyCmd(properties.getSavePath() + "/data/content.opf")
                            + " "
                            + tinyCmd(properties.getSavePath() + "/out/" + fileName);
            runCmd(cmd);
        }
    }

    private void exportMobi(String tool) {
        String cmd = "";
        if (tool.equals(SettingUtils.CALIBRE)) {
            tool = SettingUtils.CALIBRE;
            String fileName = properties.getName() + " - " + properties.getAuthor() + ".mobi";
            fileName = fileName.replaceAll("[:/\\?\\*]", "");
            cmd =
                    tinyCmd(tool)
                            + " "
                            + tinyCmd(properties.getSavePath() + "/data/content.opf")
                            + " "
                            + tinyCmd(properties.getSavePath() + "/out/" + fileName)
                            + " --mobi-file-type="
                            + compressLevel
                            + " --no-inline-toc --share-not-sync";
            runCmd(cmd);
        } else {
            tool = SettingUtils.KINDLEGEN;
            String fileName = properties.getName() + " - " + properties.getAuthor() + ".mobi";
            fileName = fileName.replaceAll("[:/\\?\\*]", "");
            cmd =
                    tinyCmd(tool)
                            + " "
                            + tinyCmd(properties.getSavePath() + "/data/content.opf")
                            + " "
                            + compressLevel;
            runCmd(cmd);
            FileUtils.cutFile(
                    properties.getSavePath() + "/data/content.mobi",
                    properties.getSavePath() + "/out/" + fileName);
        }
    }

    private void exportEpub() throws Exception {
        InputStream in = getClass().getResourceAsStream("/dark/leech/res/untitled.epub");
        String fileName = properties.getName() + " - " + properties.getAuthor() + ".epub";
        fileName = fileName.replaceAll("[:/\\?\\*]", "");
        fileName = properties.getSavePath() + "/out/" + fileName;
        FileUtils.byte2file(FileUtils.stream2byte(in), fileName);
        ZipUtils.setDefaultCompressionLevel(Integer.parseInt(compressLevel));
        ZipUtils.addFolders(fileName, properties.getSavePath() + "/data/Text", "Text");
        progressListener.setProgress(75, "(3/3)Tạo Ebook...");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/content.opf");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/toc.ncx");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/stylesheet.css");
        ZipUtils.addFile(fileName, properties.getSavePath() + "/data/cover.jpg");
        if (includeImg) ZipUtils.addFolders(fileName, properties.getSavePath() + "/data/Images");
        progressListener.setProgress(100, "Hoàn tất!");
    }

    private void exportAzw3() {
        tool = SettingUtils.CALIBRE;
        String fileName = properties.getName() + " - " + properties.getAuthor() + ".azw3";
        fileName = fileName.replaceAll("[:/\\?\\*]", "");
        String cmd =
                tinyCmd(tool)
                        + " "
                        + tinyCmd(properties.getSavePath() + "/data/content.opf")
                        + " "
                        + tinyCmd(properties.getSavePath() + "/out/" + fileName)
                        + " --share-not-sync --no-inline-toc";
        runCmd(cmd);
    }

    private void exportPdf() {
        tool = SettingUtils.CALIBRE;
        String fileName = properties.getName() + " - " + properties.getAuthor() + ".pdf";
        fileName = fileName.replaceAll("[:/\\?\\*]", "");
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
        return tool.endsWith(".exe");
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
            e.printStackTrace();
            Log.add(e);
        }
    }

    private String tinyCmd(String cmd) {
        if (cmd.indexOf(" ") != -1) cmd = "\"" + cmd + "\"";
        return FileUtils.validate(cmd);
    }

    public void addProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }
}
