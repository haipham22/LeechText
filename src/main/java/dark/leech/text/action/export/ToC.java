package dark.leech.text.action.export;

import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.RegexUtils;
import dark.leech.text.util.SettingUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Long on 9/17/2016.
 */
public class ToC {
    private int id;
    private Properties properties;
    private List<Chapter> chapList;
    private boolean autoSplit;
    private StringBuilder toc;
    private StringBuilder content;
    private StringBuilder muclucHtml;
    private List<String> partList;
    private List<String> namePartList;
    private boolean includeImg;
    private String charset;


    public ToC(Properties properties) {
        this.properties = properties;
        this.chapList = properties.getChapList();
        toc = new StringBuilder();
        content = new StringBuilder();
        muclucHtml = new StringBuilder();
        partList = new ArrayList<String>();
        namePartList = new ArrayList<String>();
        this.charset = properties.getCharset();
    }

    public void setIncludeImg(boolean includeImg) {
        this.includeImg = includeImg;
    }

    public void setAutoSplit(boolean autoSplit) {
        this.autoSplit = autoSplit;
    }


    public ArrayList<Integer> splitPart() {
        ArrayList<Integer> part = new ArrayList<Integer>();
        int index, c1;
        //
        for (index = 0; index < chapList.size(); index++) {
            if (chapList.get(index).getPartName().length() != 0) {
                part.add(index);
                break;
            }
            if (RegexUtils.find(chapList.get(index).getChapName(), "(Ch..ng\\s*\\d+)", 1).length() != 0) {
                part.add(index);
                break;
            }
        }
        c1 = index + 1;
        //Lấy quyển chia sẳn
        //Lấy các quyển tiếp
        int nextPart = 2;
        for (index = c1; index < chapList.size(); index++) {
            if (toInt(RegexUtils.find(chapList.get(index).getChapName(), "(Q|Quy.n\\s*)(\\d+)", 2)) == nextPart) {
                part.add(index);
                nextPart++;
            }
        }
        if (part.size() > 1) return part;
        //Quyển không chia sẳn

        //Chia tự động theo c1
        for (index = c1; index < chapList.size(); index++)
            if (toInt(RegexUtils.find(chapList.get(index).getChapName(), "Ch..ng\\s*(\\d+)", 1)) == 1) {
                if (toInt(RegexUtils.find(chapList.get(index - 1).getChapName(), "Ch..ng\\s*(\\d+)", 1)) != 1)
                    part.add(index);
            }

        if (part.size() > 1) return part;
        //Chia 100c/Quyển
        if (chapList.size() < 300) return part;
        int partNum = 100;
        for (index = c1; index < chapList.size(); index++) {
            int i = Math.abs(partNum - toInt(RegexUtils.find(chapList.get(index).getChapName(), "Ch..ng\\s*(\\d+)", 1)));
            if (i >= 0 && i <= 5) {
                int vt = findAround(index, 10, partNum);
                if (vt == -1)
                    continue;
                else index = vt;
                part.add(index);
                partNum += 100;
            }
        }
        return part;
    }

    //Tìm kiếm chương xung quanh
    private int findAround(int point, int range, int value) {
        for (int i = 0; i < range; i++)
            if (i + point < chapList.size())
                if (value < toInt(RegexUtils.find(chapList.get(i + point).getChapName(), "Ch..ng\\s*(\\d+)", 1))) {
                    return i + point;
                }
        return -1;
    }

    private int toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    public void mkToC() {
        id = 1;
        chapList = properties.getChapList();
        muclucHtml.append("\n<h4>Mục lục</h4>\n");
        if (properties.isAddGt()) {
            muclucHtml.append("<div class=\"lv2\"><a href=\"../Text/gioithieu.html\">Giới Thiệu</a></div>\n");
            toc.append("    <navPoint id=\"gioithieu\" playorder=\"" + Integer.toString(id)
                    + "\">\n" + "      <navLabel>\n" + "        <text>Giới Thiệu</text>\n"
                    + "      </navLabel>\n" + "      <content src=\"Text/gioithieu.html\"/>\n</navPoint>\n"
            );
            content.append("\t" + "<item id=\"gioithieu\" href=\"Text/gioithieu.html\" media-type=\"application/xhtml+xml\"/>\n");
            id++;
        }
        content.append("\t" + "<item id=\"mucluc\" href=\"Text/mucluc.html\" media-type=\"application/xhtml+xml\"/>\n");
        //Không chia quyển
        if (!autoSplit) {
            makePart(0, chapList.size(), "\t");
            saveData();
            return;
        }
        ArrayList<Integer> part = splitPart();
        if (part.size() < 2) {
            makePart(0, chapList.size(), "\t");
            saveData();
            return;
        }
        //Chia quyển
        makePart(0, part.get(0), "    ");
        for (int i = 0; i < part.size(); i++) {
            String namePart = chapList.get(part.get(i)).getPartName();
            if (namePart.length() == 0)
                namePart = "Chương " + Integer.toString(i * 100 + 1) + "→" + ((i == part.size() - 1) ? "Hết" : String.valueOf((i + 1) * 100));
            String s = "    <navPoint id=\"nav" + Integer.toString(id) + "\" playorder=\"" + Integer.toString(id)
                    + "\">\n" + "      <navLabel>\n" + "        <text>" + namePart + "</text>\n"
                    + "      </navLabel>\n" + "      <content src=\"Text/Q" + Integer.toString(i + 1) + ".html\"/>\n";
            toc.append(s);
            content.append("\t" + "<item id=\"Q" + Integer.toString(i + 1) + "\" href=\"Text/Q" + Integer.toString(i + 1) + ".html\" media-type=\"application/xhtml+xml\"/>\n");
            namePartList.add(namePart);
            id++;
            if (i == part.size() - 1)
                makePart(part.get(i), chapList.size(), "      ");
            else
                makePart(part.get(i), part.get(i + 1), "      ");
            toc.append("    </navPoint>\n");
        }
        saveData();
    }

    private void makePart(int start, int end, String tab) {
        StringBuilder pa = new StringBuilder();
        for (int i = start; i < end; i++) {
            String s = tab + "<navPoint id=\"nav" + Integer.toString(id) + "\" playorder=\"" + Integer.toString(id)
                    + "\">\n" + tab + "  <navLabel>\n" + tab + "    <text>" + chapList.get(i).getChapName() + "</text>\n"
                    + tab + "  </navLabel>\n" + tab + "  <content src=\"Text/" + chapList.get(i).getId() + ".html\"/>\n"
                    + tab + "</navPoint>\n";
            pa.append("<div class=\"lv2\"><a href=\"../Text/" + chapList.get(i).getId() + ".html\">" + chapList.get(i).getChapName() + "</a></div>\n");
            content.append("\t" + "<item id=\"C" + Integer.toString(i) + "\" href=\"Text/" + chapList.get(i).getId() + ".html\" media-type=\"application/xhtml+xml\"/>\n");
            toc.append(s);
            id++;
        }
        partList.add(pa.toString());
    }

    private void saveData() {
        //toc.ncx
        String ncx = FileUtils.stream2string("/dark/leech/res/toc.ncx");
        ncx = ncx.replace("[NAME]", properties.getName())
                .replace("[AUTHOR]", properties.getAuthor())
                .replace("[NAVPOINT]", new String(toc));
        FileUtils.string2file(ncx, properties.getSavePath() + "/data/toc.ncx", charset);
        //content.opf
        String opf = FileUtils.stream2string("/dark/leech/res/content.opf");
        opf = opf.replace("[NAME]", properties.getName())
                .replace("[AUTHOR]", properties.getAuthor());
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        opf = opf.replace("[DATE]", df.format(todaysDate));
        String manifest = new String(content);
        opf = opf.replace("[MANIFEST]", manifest)
                .replace("[IMAGE]", addImg());
        manifest = manifest.replaceAll("<item id=(.*?)\\s*href=.*?/>", "<itemref idref=$1/>");
        opf = opf.replace("[NCX]", manifest);
        FileUtils.string2file(opf, properties.getSavePath() + "/data/content.opf", charset);
        //mucluc.html
        String head = SettingUtils.HTML_SYNTAX;
        head = head.replaceAll("(?s)(.*?<body.*?>).*", "$1");
        if (!autoSplit) {
            muclucHtml.append(partList.get(0));
        } else {
            if (partList.get(0) != null)
                muclucHtml.append(partList.get(0));
            for (int i = 1; i < partList.size(); i++) {
                muclucHtml.append("<div class=\"lv2\"><a href=\"../Text/Q" + Integer.toString(i) + ".html\">" + namePartList.get(i - 1) + "</a></div>\n");
                String Q = head;
                Q = Q.replaceAll("<title>.*?</title>", "<title>" + namePartList.get(i - 1) + "</title>");
                Q += "\n<h4>" + namePartList.get(i - 1) + "</h4>\n";
                Q += partList.get(i);
                Q += "</body>\n</html>";
                FileUtils.string2file(Q, properties.getSavePath() + "/data/Text/Q" + Integer.toString(i) + ".html", charset);
            }
        }
        String mucluc = head.replaceAll("<title>.*?</title>", "<title>Mục lục</title>")
                + muclucHtml.toString()
                + "</body>\n</html>";
        FileUtils.string2file(mucluc, properties.getSavePath() + "/data/Text/mucluc.html", charset);
    }

    private String addImg() {
        if (!includeImg) return "\n";
        StringBuilder img = new StringBuilder();
        File file = new File(properties.getSavePath() + "/data/Images");
        String[] files = file.list();
        for (String fn : files) {
            if (fn.endsWith(".png"))
                img.append("<item id=\"" + fn + "\" href=\"Images/" + fn + "\" media-type=\"image/png\"/>\n");
            if (fn.endsWith(".jpg") || fn.endsWith(".jpeg"))
                img.append("<item id=\"" + fn + "\" href=\"Images/" + fn + "\" media-type=\"image/jpeg\"/>\n");
            if (fn.endsWith(".gif"))
                img.append("<item id=\"" + fn + "\" href=\"Images/" + fn + "\" media-type=\"image/gif\"/>\n");
        }
        return new String(img);
    }
}

