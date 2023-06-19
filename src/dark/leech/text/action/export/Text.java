package dark.leech.text.action.export;

import dark.leech.text.action.Log;
import dark.leech.text.listeners.ProgressListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.models.Trash;
import dark.leech.text.util.*;

import java.util.List;

/**
 * Created by Long on 9/17/2016.
 */
public class Text {
    private Properties properties;
    private int type;
    private boolean makeToc;
    private boolean includeCss;
    private int tach;
    private ProgressListener progressListener;
    private String syntax;
    private String charset;
    private List<Chapter> chapList;


    public Text(Properties properties, int type, boolean makeToc, boolean includeCss, int tach) {
        this.properties = properties;
        this.type = type;
        this.makeToc = makeToc;
        this.includeCss = includeCss;
        this.tach = tach;
        this.charset = properties.getCharset();

    }

    public void export() {
        if (type == TypeUtils.HTML) {
            syntax = SettingUtils.HTML_SYNTAX.replaceAll("[Uu][Tt][Ff]-8", charset);
            exportTach(".html");
        } else {
            syntax = SettingUtils.TXT_SYNTAX;
            exportTach(".txt");
        }


    }

    public void exportTach(String duoi) {
        chapList = properties.getChapList();
        StringBuffer gop = new StringBuffer();
        //Tạo file gộp tach == 1

        if (tach == 1) {
            if (type == TypeUtils.HTML) {
                String head = syntax;
                head = head.replaceAll("<title>.*?</title>", "<title>" + properties.getName() + "</title>");
                head = head.replaceAll("(?s)(.*?<body.*?>).*", "$1");
                if (includeCss)
                    head = head.replace("</head>", "<style>\n" + SettingUtils.CSS_SYNTAX + "\n</style>\n</head>");
                gop.append(head);
            }
        }
        if (properties.isAddGt()) {
            Chapter gt = new Chapter();
            gt.setChapName("Giới Thiệu");
            gt.setId("gioithieu");
            String text = "";
            try {
                text = chapterReplace(gt, false);
            } catch (Exception e) {
                Log.add(e);
            }
            if (tach == 0) {
                String savePath = properties.getSavePath() + "/data/Text/" + gt.getId() + duoi;
                FileUtils.string2file(text, savePath, charset);
            } else {
                if (type == TypeUtils.HTML)
                    text = text.replaceAll("(?s).*?<body.*?>(.*?)</body>.*", "$1");
                else text += "\n\n";
                gop.append(text);
            }
        }
        //Tạo mục lục
        if (makeToc && type == TypeUtils.HTML) {
            String toc = "\n<h4>Mục lục</h4>\n";
            if (tach == 0) {
                if (properties.isAddGt())
                    toc += "<div class=\"lv2\"><a href=\"../Text/gioithieu.html\">Giới Thiệu</a></div>\n";
                for (Chapter ch : chapList)
                    toc += "<div class=\"lv2\"><a href=\"../Text/"
                            + ch.getId() + ".html\">"
                            + (ch.getPartName().length() == 0 ? "" : ch.getPartName()
                            + " - ")
                            + ch.getChapName()
                            + "</a></div>\n";
                String head = syntax;
                head = head.replaceAll("<title>.*?</title>", "<title>" + properties.getName() + "</title>");
                head = head.replaceAll("(?s)(.*?<body.*?>).*", "$1");
                toc = head + toc + "</body>\n</html>";
                FileUtils.string2file(toc, properties.getSavePath() + "/data/Text/mucluc.html", charset);

            } else {
                if (properties.isAddGt())
                    toc += "<div class=\"lv2\"><a href=\"#gioithieu\">Giới Thiệu</a></div>\n";
                for (Chapter ch : chapList) {
                    toc += "<div class=\"lv2\"><a href=\"#" + ch.getId() + "\">" + (ch.getPartName().length() == 0 ? "" : ch.getPartName() + " - ") + ch.getChapName() + "</a></div>\n";
                }
                gop.append(toc);
            }
        }
        //Thay thế text
        int value = 0;

        for (Chapter ch : chapList) {
            if (ch.isCompleted()) {
                String text = "";
                try {
                    text = chapterReplace(ch);
                } catch (Exception e) {
                }
                if (tach == 0) {
                    String savePath = properties.getSavePath() + "/data/Text/" + ch.getId() + duoi;
                    FileUtils.string2file(text, savePath, charset);
                } else {
                    if (type == TypeUtils.HTML)
                        text = text.replaceAll("(?s).*?<body.*?>(.*?)</body>.*", "$1");
                    else text += "\n\n";
                    gop.append(text);
                }
                value++;
                if (progressListener != null)
                    progressListener.setProgress(value * 100 / properties.getSize(), "[2/3]Xuất text...");
            }
        }
        if (tach == 1) {
            if (type == TypeUtils.HTML)
                gop.append("</body>\n</html>");
            FileUtils.string2file(gop.toString(), properties.getSavePath() + "/out/text" + duoi, charset);
        }

    }

    private String clearText(String text) {
        for (Trash tr : SettingUtils.TRASH)
            if (tr.isReplace()) {
                String src = tr.getSrc()
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
                String to = tr.getTo()
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");
                text = text.replaceAll(src, to);
            }
        return SyntaxUtils.covertString(text);
    }

    private String chapterReplace(Chapter chapter) throws Exception {
        return chapterReplace(chapter, true);
    }

    private String chapterReplace(Chapter chapter, boolean pp) throws Exception {
        String nd = FileUtils.file2string(properties.getSavePath() + "/raw/" + chapter.getId() + ".txt", charset);
        nd = clearText(nd);
        String text = syntax;
        //
        text = replaceString(text, properties.getName(), "\\[1\\]", "NAME");
        text = replaceString(text, properties.getAuthor(), "\\[2\\]", "AUTHOR");
        text = replaceString(text, chapter.getPartName(), "\\[3\\]", "NAME_PART");
        text = replaceString(text, chapter.getChapName(), "\\[4\\]", "NAME_CHAP");
        text = text.replace("[ID]", chapter.getId());
        //replace paragraph
        String firstTag = RegexUtils.find(syntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 1);
        String lastTag = RegexUtils.find(syntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 2);
        nd = replaceDrop(nd);
        if (pp) {
            nd = nd.replace("\n", lastTag + "\n" + firstTag);
            nd = firstTag + nd + lastTag;
        }
        text = text.replace(RegexUtils.find(text, "(\\[5\\].*\\[PARAGRAPH\\].*\\[5\\])", 1), nd);

        return text;
    }

    private String replaceString(String src, String replace, String tag, String key) {
        if (replace == null || replace.length() < 2)
            return src.replaceAll("\\s*" + tag + ".*?" + tag, "");
        src = src.replaceAll(tag + "(.*?" + "\\[" + key + "\\]" + ".*?)" + tag, "$1")
                .replace("[" + key + "]", replace);
        return src;
    }

    private String replaceDrop(String text) {
        if (text.length() == 0) return text;
        char drop = text.charAt(0);
        String newt;
        if (SettingUtils.IS_DROP_SELECTED && type == TypeUtils.HTML)
            if (drop != Character.toLowerCase(drop)) {
                newt = (SettingUtils.DROP_SYNTAX).replace("[DROP]", String.valueOf(drop));
                text = newt + text.substring(1);
            }
        return text;
    }

    public void addProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

}
