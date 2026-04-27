package dark.leech.text.action.export;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.RegexUtils;
import dark.leech.text.util.SettingUtils;

/**
 * Generates Table of Contents (ToC) for ebook exports. Creates NCX, OPF, and HTML table of contents
 * files with optional part splitting.
 *
 * @author Long
 * @since 9/17/2016
 */
public class ToC {
    private static final String CHAPTER_PATTERN = "Ch..ng\\s*(\\d+)";
    private static final String VOLUME_PATTERN = "(Q|Quy.n\\s*)(\\d+)";
    private static final int DEFAULT_PART_SIZE = 100;
    private static final int CHAPTER_SEARCH_RANGE = 10;
    private static final int CHAPTER_TOLERANCE = 5;
    private static final int AUTO_SPLIT_THRESHOLD = 300;
    private static final int FIRST_VOLUME_CHAPTER = 2;

    private final Properties properties;
    private final List<Chapter> chapList;
    private final StringBuilder tocBuilder;
    private final StringBuilder contentBuilder;
    private final StringBuilder htmlToCBuilder;
    private final List<String> partList;
    private final List<String> partNameList;
    private final String charset;

    private int navPointId;
    private boolean autoSplit;
    private boolean includeImg;

    public ToC(Properties properties) {
        this.properties = properties;
        this.chapList = properties.getChapList();
        this.charset = properties.getCharset();
        this.tocBuilder = new StringBuilder();
        this.contentBuilder = new StringBuilder();
        this.htmlToCBuilder = new StringBuilder();
        this.partList = new ArrayList<>();
        this.partNameList = new ArrayList<>();
    }

    public void setIncludeImg(boolean includeImg) {
        this.includeImg = includeImg;
    }

    public void setAutoSplit(boolean autoSplit) {
        this.autoSplit = autoSplit;
    }

    /** Main entry point to generate table of contents files. */
    public void mkToC() {
        navPointId = 1;

        buildHtmlToCHeader();

        if (properties.isAddGt()) {
            addIntroductionSection();
        }

        addToCSection();

        if (!autoSplit) {
            generateSinglePart();
        } else {
            List<Integer> partIndexes = splitPart();
            if (partIndexes.size() < 2) {
                generateSinglePart();
            } else {
                generateMultiPartContent(partIndexes);
            }
        }

        saveData();
    }

    private void buildHtmlToCHeader() {
        htmlToCBuilder.append("\n<h4>Mục lục</h4>\n");
    }

    private void addIntroductionSection() {
        htmlToCBuilder.append(
                "<div class=\"lv2\"><a href=\"../Text/gioithieu.html\">Giới Thiệu</a></div>\n");

        tocBuilder.append(
                createNavPoint("gioithieu", "Giới Thiệu", "Text/gioithieu.html", navPointId++));
        tocBuilder.append("</navPoint>\n");

        contentBuilder.append(
                "\t<item id=\"gioithieu\" href=\"Text/gioithieu.html\""
                        + " media-type=\"application/xhtml+xml\"/>\n");
    }

    private void addToCSection() {
        contentBuilder.append(
                "\t<item id=\"mucluc\" href=\"Text/mucluc.html\""
                        + " media-type=\"application/xhtml+xml\"/>\n");
    }

    private void generateSinglePart() {
        makePart(0, chapList.size(), "\t");
    }

    private void generateMultiPartContent(List<Integer> partIndexes) {
        makePart(0, partIndexes.get(0), "    ");

        for (int i = 0; i < partIndexes.size(); i++) {
            String partName = determinePartName(i, partIndexes);
            addVolumeSection(i, partName, partIndexes);
        }
    }

    private String determinePartName(int partIndex, List<Integer> partIndexes) {
        String name = chapList.get(partIndexes.get(partIndex)).getPartName();
        if (name.isEmpty()) {
            int startChapter = partIndex * DEFAULT_PART_SIZE + 1;
            int endChapter =
                    (partIndex == partIndexes.size() - 1)
                            ? -1 // "Hết" (End)
                            : (partIndex + 1) * DEFAULT_PART_SIZE;
            name = formatPartName(startChapter, endChapter);
        }
        return name;
    }

    private String formatPartName(int start, int end) {
        return "Chương " + start + "→" + (end == -1 ? "Hết" : String.valueOf(end));
    }

    private void addVolumeSection(int volumeIndex, String volumeName, List<Integer> partIndexes) {
        String volumeId = "Q" + (volumeIndex + 1);

        tocBuilder.append(
                createNavPoint(
                        "nav" + navPointId,
                        volumeName,
                        "Text/" + volumeId + ".html",
                        navPointId++,
                        "    "));

        contentBuilder.append(
                String.format(
                        "\t<item id=\"%s\" href=\"Text/%s.html\""
                                + " media-type=\"application/xhtml+xml\"/>%n",
                        volumeId, volumeId));

        partNameList.add(volumeName);

        int startIndex = partIndexes.get(volumeIndex);
        int endIndex =
                (volumeIndex == partIndexes.size() - 1)
                        ? chapList.size()
                        : partIndexes.get(volumeIndex + 1);

        makePart(startIndex, endIndex, "      ");
        tocBuilder.append("    </navPoint>\n");
    }

    /**
     * Analyzes chapter list and determines optimal split points for volumes. Uses multiple
     * strategies: pre-marked parts, volume markers, chapter 1 detection, or fixed-size fallback for
     * large works.
     *
     * @return List of chapter indexes where volumes should split
     */
    public List<Integer> splitPart() {
        List<Integer> splitPoints = new ArrayList<>();

        // Strategy 1: Find first valid chapter or pre-marked part
        int startIndex = findFirstSplitPoint();
        if (startIndex < chapList.size()) {
            splitPoints.add(startIndex);
        }

        // Strategy 2: Look for pre-marked volume indicators
        findVolumeIndicators(startIndex + 1, splitPoints);
        if (splitPoints.size() > 1) {
            return splitPoints;
        }

        // Strategy 3: Detect chapter 1 restarts
        findChapterRestarts(startIndex + 1, splitPoints);
        if (splitPoints.size() > 1) {
            return splitPoints;
        }

        // Strategy 4: Fallback to fixed-size parts for large works
        if (chapList.size() >= AUTO_SPLIT_THRESHOLD) {
            createFixedSizeSplits(startIndex + 1, splitPoints);
        }

        return splitPoints;
    }

    private int findFirstSplitPoint() {
        for (int i = 0; i < chapList.size(); i++) {
            Chapter chapter = chapList.get(i);

            if (!chapter.getPartName().isEmpty()) {
                return i;
            }

            String chapterNum = RegexUtils.find(chapter.getChapName(), CHAPTER_PATTERN, 1);
            if (!chapterNum.isEmpty()) {
                return i;
            }
        }
        return 0;
    }

    private void findVolumeIndicators(int startIndex, List<Integer> splitPoints) {
        int expectedVolume = FIRST_VOLUME_CHAPTER;

        for (int i = startIndex; i < chapList.size(); i++) {
            String volumeNum = RegexUtils.find(chapList.get(i).getChapName(), VOLUME_PATTERN, 2);
            if (parseInt(volumeNum) == expectedVolume) {
                splitPoints.add(i);
                expectedVolume++;
            }
        }
    }

    private void findChapterRestarts(int startIndex, List<Integer> splitPoints) {
        for (int i = startIndex; i < chapList.size(); i++) {
            String currentChapter =
                    RegexUtils.find(chapList.get(i).getChapName(), CHAPTER_PATTERN, 1);

            if (parseInt(currentChapter) == 1) {
                String previousChapter =
                        RegexUtils.find(chapList.get(i - 1).getChapName(), CHAPTER_PATTERN, 1);

                if (parseInt(previousChapter) != 1) {
                    splitPoints.add(i);
                }
            }
        }
    }

    private void createFixedSizeSplits(int startIndex, List<Integer> splitPoints) {
        int targetChapter = DEFAULT_PART_SIZE;

        for (int i = startIndex; i < chapList.size(); i++) {
            String chapterNum = RegexUtils.find(chapList.get(i).getChapName(), CHAPTER_PATTERN, 1);
            int currentChapter = parseInt(chapterNum);

            int difference = Math.abs(targetChapter - currentChapter);
            if (difference >= 0 && difference <= CHAPTER_TOLERANCE) {
                int adjustedIndex = findNearestChapterMatch(i, CHAPTER_SEARCH_RANGE, targetChapter);
                if (adjustedIndex != -1) {
                    i = adjustedIndex;
                    splitPoints.add(i);
                    targetChapter += DEFAULT_PART_SIZE;
                }
            }
        }
    }

    /**
     * Searches for chapter number near target within range.
     *
     * @param startPoint Index to start searching from
     * @param range Number of chapters to search
     * @param targetChapter Target chapter number
     * @return Index of best match, or -1 if not found
     */
    private int findNearestChapterMatch(int startPoint, int range, int targetChapter) {
        for (int offset = 0; offset < range; offset++) {
            int index = startPoint + offset;
            if (index >= chapList.size()) {
                break;
            }

            String chapterNum =
                    RegexUtils.find(chapList.get(index).getChapName(), CHAPTER_PATTERN, 1);

            if (parseInt(chapterNum) > targetChapter) {
                return index;
            }
        }
        return -1;
    }

    private void makePart(int startIndex, int endIndex, String indent) {
        StringBuilder partHtml = new StringBuilder();

        for (int i = startIndex; i < endIndex; i++) {
            Chapter chapter = chapList.get(i);
            String chapterId = chapter.getId();
            String chapterName = chapter.getChapName();

            tocBuilder.append(
                    createNavPoint(
                            "nav" + navPointId,
                            chapterName,
                            "Text/" + chapterId + ".html",
                            navPointId++,
                            indent));
            tocBuilder.append(indent).append("</navPoint>\n");
            partHtml.append(createChapterLink(chapterId, chapterName));
            contentBuilder.append(
                    String.format(
                            "\t<item id=\"C%d\" href=\"Text/%s.html\""
                                    + " media-type=\"application/xhtml+xml\"/>%n",
                            i, chapterId));
        }

        partList.add(partHtml.toString());
    }

    private String createNavPoint(String id, String label, String src, int playOrder) {
        return createNavPoint(id, label, src, playOrder, "\t");
    }

    private String createNavPoint(
            String id, String label, String src, int playOrder, String indent) {
        return String.format(
                "%s<navPoint id=\"%s\" playorder=\"%d\">%n"
                        + "%s  <navLabel>%n"
                        + "%s    <text>%s</text>%n"
                        + "%s  </navLabel>%n"
                        + "%s  <content src=\"%s\"/>%n",
                indent, id, playOrder, indent, indent, escapeXml(label), indent, indent, src);
    }

    private String createChapterLink(String chapterId, String chapterName) {
        return String.format(
                "<div class=\"lv2\"><a href=\"../Text/%s.html\">%s</a></div>%n",
                chapterId, escapeHtml(chapterName));
    }

    private void saveData() {
        saveNcxFile();
        saveOpfFile();
        saveHtmlFiles();
    }

    private void saveNcxFile() {
        String ncxTemplate = FileUtils.stream2string("/dark/leech/res/toc.ncx");
        String ncxContent =
                ncxTemplate
                        .replace("[NAME]", properties.getName())
                        .replace("[AUTHOR]", properties.getAuthor())
                        .replace("[NAVPOINT]", tocBuilder.toString());

        FileUtils.string2file(ncxContent, properties.getSavePath() + "/data/toc.ncx", charset);
    }

    private void saveOpfFile() {
        String opfTemplate = FileUtils.stream2string("/dark/leech/res/content.opf");
        String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        String opfContent =
                opfTemplate
                        .replace("[NAME]", properties.getName())
                        .replace("[AUTHOR]", properties.getAuthor())
                        .replace("[DATE]", currentDate);

        String manifest = contentBuilder.toString();
        String spine = manifest.replaceAll("<item id=(.*?)\\s*href=.*?/>", "<itemref idref=$1/>");

        opfContent =
                opfContent
                        .replace("[MANIFEST]", manifest)
                        .replace("[IMAGE]", getImageManifest())
                        .replace("[NCX]", spine);

        FileUtils.string2file(opfContent, properties.getSavePath() + "/data/content.opf", charset);
    }

    private void saveHtmlFiles() {
        String htmlHeader = extractHtmlHeader();

        if (!autoSplit) {
            saveSingleToCFile(htmlHeader);
        } else {
            saveMultiPartToCFiles(htmlHeader);
        }
    }

    private String extractHtmlHeader() {
        String html = SettingUtils.HTML_SYNTAX;
        return html.replaceAll("(?s)(.*?<body.*?>).*", "$1");
    }

    private void saveSingleToCFile(String header) {
        String toCContent =
                header.replaceAll("<title>.*?</title>", "<title>Mục lục</title>")
                        + htmlToCBuilder.toString()
                        + partList.get(0)
                        + "</body>\n</html>";

        FileUtils.string2file(
                toCContent, properties.getSavePath() + "/data/Text/mucluc.html", charset);
    }

    private void saveMultiPartToCFiles(String header) {
        // Save main ToC
        String mainToC =
                header.replaceAll("<title>.*?</title>", "<title>Mục lục</title>")
                        + htmlToCBuilder.toString();

        if (!partList.isEmpty()) {
            mainToC += partList.get(0);
        }

        mainToC += "</body>\n</html>";
        FileUtils.string2file(
                mainToC, properties.getSavePath() + "/data/Text/mucluc.html", charset);

        // Save volume files
        for (int i = 1; i < partList.size(); i++) {
            String volumeContent = createVolumeHtmlFile(header, i);
            String volumePath = properties.getSavePath() + "/data/Text/Q" + i + ".html";
            FileUtils.string2file(volumeContent, volumePath, charset);
        }
    }

    private String createVolumeHtmlFile(String header, int volumeIndex) {
        String volumeTitle = partNameList.get(volumeIndex - 1);

        // Add volume link to main ToC
        htmlToCBuilder.append(
                String.format(
                        "<div class=\"lv2\"><a href=\"../Text/Q%d.html\">%s</a></div>%n",
                        volumeIndex, volumeTitle));

        return header.replaceAll("<title>.*?</title>", "<title>" + volumeTitle + "</title>")
                + "\n<h4>"
                + volumeTitle
                + "</h4>\n"
                + partList.get(volumeIndex)
                + "</body>\n</html>";
    }

    private String getImageManifest() {
        if (!includeImg) {
            return "\n";
        }

        StringBuilder manifest = new StringBuilder();
        File imagesDir = new File(properties.getSavePath() + "/data/Images");

        if (!imagesDir.exists() || !imagesDir.isDirectory()) {
            return "\n";
        }

        String[] imageFiles = imagesDir.list();
        if (imageFiles == null) {
            return "\n";
        }

        for (String filename : imageFiles) {
            String mimeType = determineImageMimeType(filename);
            if (mimeType != null) {
                manifest.append(
                        String.format(
                                "<item id=\"%s\" href=\"Images/%s\" media-type=\"%s\"/>%n",
                                filename, filename, mimeType));
            }
        }

        return manifest.toString();
    }

    private String determineImageMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return null;
    }

    private int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
