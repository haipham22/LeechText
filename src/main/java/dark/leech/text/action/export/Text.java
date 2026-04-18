package dark.leech.text.action.export;

import java.util.List;

import dark.leech.text.action.Log;
import dark.leech.text.listeners.ProgressListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.models.Trash;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.HtmlSanitizer;
import dark.leech.text.util.RegexUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.SyntaxUtils;
import dark.leech.text.util.TypeUtils;

/**
 * Exports ebook content to text or HTML format. Supports both split (separate files) and combined
 * (single file) output modes with optional table of contents generation.
 *
 * @author Long
 * @since 9/17/2016
 */
public class Text {
    private static final String RAW_FILE_EXTENSION = ".txt";
    private static final String INTRODUCTION_ID = "gioithieu";
    private static final String INTRODUCTION_NAME = "Giới Thiệu";
    private static final String TABLE_OF_CONTENTS_TITLE = "Mục lục";
    private static final String PROGRESS_MESSAGE_FORMAT = "[2/3]Xuất text...";
    private static final int PROGRESS_MULTIPLIER = 100;

    private final Properties properties;
    private final ExportFormat format;
    private final ExportMode exportMode;
    private final ExportOptions options;
    private final String charset;

    private ProgressListener progressListener;
    private String templateSyntax;

    public Text(Properties properties, int type, boolean makeToc, boolean includeCss, int tach) {
        this.properties = properties;
        this.format = (type == TypeUtils.HTML) ? ExportFormat.HTML : ExportFormat.TEXT;
        this.exportMode = (tach == 1) ? ExportMode.COMBINED : ExportMode.SPLIT;
        this.options = new ExportOptions(makeToc, includeCss);
        this.charset = properties.getCharset();
    }

    public void addProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /** Main entry point to export content. */
    public void export() {
        initializeTemplate();
        String fileExtension = format.getFileExtension();
        exportContent(fileExtension);
    }

    private void initializeTemplate() {
        if (format == ExportFormat.HTML) {
            templateSyntax = SettingUtils.HTML_SYNTAX.replaceAll("[Uu][Tt][Ff]-8", charset);
        } else {
            templateSyntax = SettingUtils.TXT_SYNTAX;
        }
    }

    private void exportContent(String fileExtension) {
        List<Chapter> chapters = properties.getChapList();
        StringBuilder combinedContent = new StringBuilder();

        if (exportMode == ExportMode.COMBINED) {
            combinedContent.append(createCombinedFileHeader());
        }

        if (properties.isAddGt()) {
            exportIntroduction(chapters, fileExtension, combinedContent);
        }

        if (options.makeToc && format == ExportFormat.HTML) {
            exportTableOfContents(chapters, fileExtension, combinedContent);
        }

        exportChapters(chapters, fileExtension, combinedContent);

        if (exportMode == ExportMode.COMBINED) {
            finalizeCombinedFile(combinedContent, fileExtension);
        }
    }

    private String createCombinedFileHeader() {
        if (format != ExportFormat.HTML) {
            return "";
        }

        String header = templateSyntax;
        header =
                header.replaceAll(
                        "<title>.*?</title>", "<title>" + properties.getName() + "</title>");
        header = header.replaceAll("(?s)(.*?<body.*?>).*", "$1");

        if (options.includeCss) {
            header =
                    header.replace(
                            "</head>",
                            "<style>\n" + SettingUtils.CSS_SYNTAX + "\n</style>\n</head>");
        }

        return header;
    }

    private void exportIntroduction(
            List<Chapter> chapters, String fileExtension, StringBuilder combinedContent) {
        Chapter introduction = createIntroductionChapter();

        try {
            String content = processChapterContent(introduction, false);
            saveChapterContent(content, introduction.getId(), fileExtension, combinedContent);
        } catch (Exception e) {
            Log.add("Failed to export introduction: " + e.getMessage());
        }
    }

    private Chapter createIntroductionChapter() {
        Chapter chapter = new Chapter();
        chapter.setChapName(INTRODUCTION_NAME);
        chapter.setId(INTRODUCTION_ID);
        return chapter;
    }

    private void exportTableOfContents(
            List<Chapter> chapters, String fileExtension, StringBuilder combinedContent) {
        String tableOfContents = buildTableOfContentsHtml(chapters);

        if (exportMode == ExportMode.SPLIT) {
            String tocPath = properties.getSavePath() + "/data/Text/mucluc.html";
            FileUtils.string2file(tableOfContents, tocPath, charset);
        } else {
            combinedContent.append(tableOfContents);
        }
    }

    private String buildTableOfContentsHtml(List<Chapter> chapters) {
        StringBuilder tocBuilder = new StringBuilder();
        tocBuilder.append("\n<h4>" + TABLE_OF_CONTENTS_TITLE + "</h4>\n");

        if (exportMode == ExportMode.SPLIT) {
            tocBuilder.append(createSplitTableOfContents(chapters));
            return wrapInHtmlDocument(tocBuilder.toString());
        } else {
            tocBuilder.append(createCombinedTableOfContents(chapters));
            return tocBuilder.toString();
        }
    }

    private String createSplitTableOfContents(List<Chapter> chapters) {
        StringBuilder toc = new StringBuilder();

        if (properties.isAddGt()) {
            toc.append("<div class=\"lv2\"><a href=\"../Text/")
                    .append(INTRODUCTION_ID)
                    .append(".html\">")
                    .append(INTRODUCTION_NAME)
                    .append("</a></div>\n");
        }

        for (Chapter chapter : chapters) {
            toc.append("<div class=\"lv2\"><a href=\"../Text/")
                    .append(chapter.getId())
                    .append(".html\">")
                    .append(formatChapterLinkText(chapter))
                    .append("</a></div>\n");
        }

        return toc.toString();
    }

    private String createCombinedTableOfContents(List<Chapter> chapters) {
        StringBuilder toc = new StringBuilder();

        if (properties.isAddGt()) {
            toc.append("<div class=\"lv2\"><a href=\"#")
                    .append(INTRODUCTION_ID)
                    .append("\">")
                    .append(INTRODUCTION_NAME)
                    .append("</a></div>\n");
        }

        for (Chapter chapter : chapters) {
            toc.append("<div class=\"lv2\"><a href=\"#")
                    .append(chapter.getId())
                    .append("\">")
                    .append(formatChapterLinkText(chapter))
                    .append("</a></div>\n");
        }

        return toc.toString();
    }

    private String formatChapterLinkText(Chapter chapter) {
        String partName = chapter.getPartName();
        String chapterName = chapter.getChapName();

        if (partName == null || partName.isEmpty()) {
            return chapterName;
        }

        return partName + " - " + chapterName;
    }

    private String wrapInHtmlDocument(String bodyContent) {
        String document = templateSyntax;
        document =
                document.replaceAll(
                        "<title>.*?</title>", "<title>" + properties.getName() + "</title>");
        document = document.replaceAll("(?s)(.*?<body.*?>).*", "$1");
        return document + bodyContent + "</body>\n</html>";
    }

    private void exportChapters(
            List<Chapter> chapters, String fileExtension, StringBuilder combinedContent) {
        int completedCount = 0;
        int totalChapters = properties.getSize();

        for (Chapter chapter : chapters) {
            if (!chapter.isCompleted()) {
                continue;
            }

            try {
                String content = processChapterContent(chapter, true);
                saveChapterContent(content, chapter.getId(), fileExtension, combinedContent);
                completedCount++;

                reportProgress(completedCount, totalChapters);
            } catch (Exception e) {
                Log.add(
                        "Failed to export chapter "
                                + chapter.getChapName()
                                + ": "
                                + e.getMessage());
            }
        }
    }

    private void reportProgress(int completed, int total) {
        if (progressListener != null) {
            int percentage = completed * PROGRESS_MULTIPLIER / total;
            progressListener.setProgress(percentage, PROGRESS_MESSAGE_FORMAT);
        }
    }

    private void saveChapterContent(
            String content, String chapterId, String fileExtension, StringBuilder combinedContent) {

        if (exportMode == ExportMode.SPLIT) {
            String filePath = properties.getSavePath() + "/data/Text/" + chapterId + fileExtension;
            FileUtils.string2file(content, filePath, charset);
        } else {
            String processedContent = extractBodyContent(content);
            if (format == ExportFormat.TEXT) {
                processedContent += "\n\n";
            }
            combinedContent.append(processedContent);
        }
    }

    private String extractBodyContent(String htmlContent) {
        if (format == ExportFormat.HTML) {
            return htmlContent.replaceAll("(?s).*?<body.*?>(.*?)</body>.*", "$1");
        }
        return htmlContent;
    }

    private void finalizeCombinedFile(StringBuilder combinedContent, String fileExtension) {
        if (format == ExportFormat.HTML) {
            combinedContent.append("</body>\n</html>");
        }

        String outputPath = properties.getSavePath() + "/out/text" + fileExtension;
        FileUtils.string2file(combinedContent.toString(), outputPath, charset);
    }

    /**
     * Processes chapter content by loading raw text, applying transformations, and formatting.
     *
     * @param chapter Chapter to process
     * @param preserveParagraphs Whether to apply paragraph formatting
     * @return Formatted chapter content
     * @throws Exception if chapter file cannot be read
     */
    private String processChapterContent(Chapter chapter, boolean preserveParagraphs)
            throws Exception {
        String rawContent = loadRawChapterContent(chapter.getId());
        String cleanedContent = cleanTextContent(rawContent);
        String formattedContent = formatChapterContent(chapter, cleanedContent, preserveParagraphs);

        if (format == ExportFormat.HTML) {
            formattedContent = HtmlSanitizer.sanitize(formattedContent);
        }

        return formattedContent;
    }

    private String loadRawChapterContent(String chapterId) throws Exception {
        String rawFilePath = properties.getSavePath() + "/raw/" + chapterId + RAW_FILE_EXTENSION;
        return FileUtils.file2string(rawFilePath, charset);
    }

    private String cleanTextContent(String content) {
        for (Trash trashRule : SettingUtils.TRASH) {
            if (trashRule.isReplace()) {
                String searchPattern = unescapeTrashPattern(trashRule.getSrc());
                String replacement = unescapeTrashPattern(trashRule.getTo());
                content = content.replaceAll(searchPattern, replacement);
            }
        }
        return SyntaxUtils.covertString(content);
    }

    private String unescapeTrashPattern(String pattern) {
        if (pattern == null) {
            return "";
        }
        return pattern.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    private String formatChapterContent(
            Chapter chapter, String content, boolean preserveParagraphs) {

        String template = templateSyntax;

        // Replace placeholders with actual values
        template = replaceTemplatePlaceholder(template, properties.getName(), "\\[1\\]", "NAME");
        template =
                replaceTemplatePlaceholder(template, properties.getAuthor(), "\\[2\\]", "AUTHOR");
        template =
                replaceTemplatePlaceholder(template, chapter.getPartName(), "\\[3\\]", "NAME_PART");
        template =
                replaceTemplatePlaceholder(template, chapter.getChapName(), "\\[4\\]", "NAME_CHAP");
        template = template.replace("[ID]", chapter.getId());

        // Format paragraphs
        String formattedContent = formatParagraphs(content, preserveParagraphs);
        String paragraphPlaceholder =
                RegexUtils.find(template, "(\\[5\\].*\\[PARAGRAPH\\].*\\[5\\])", 1);

        return template.replace(paragraphPlaceholder, formattedContent);
    }

    private String replaceTemplatePlaceholder(
            String template, String value, String placeholderTag, String placeholderKey) {

        if (value == null || value.length() < 2) {
            return template.replaceAll("\\s*" + placeholderTag + ".*?" + placeholderTag, "");
        }

        String pattern =
                placeholderTag + "(.*?" + "\\[" + placeholderKey + "\\]" + ".*?)" + placeholderTag;
        String replacement = template.replaceAll(pattern, "$1");
        return replacement.replace("[" + placeholderKey + "]", value);
    }

    private String formatParagraphs(String content, boolean preserveParagraphs) {
        if (content.isEmpty()) {
            return content;
        }

        content = applyDropCapFormatting(content);

        if (preserveParagraphs) {
            String paragraphTemplate = extractParagraphTemplate();
            content = wrapInParagraphTags(content, paragraphTemplate);
        }

        return content;
    }

    private String extractParagraphTemplate() {
        String firstTag =
                RegexUtils.find(templateSyntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 1);
        String lastTag =
                RegexUtils.find(templateSyntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 2);
        return firstTag + "\n" + lastTag;
    }

    private String wrapInParagraphTags(String content, String paragraphTemplate) {
        String[] parts = paragraphTemplate.split("\n");
        if (parts.length != 2) {
            return content;
        }

        String firstTag = parts[0];
        String lastTag = parts[1];

        content = content.replace("\n", lastTag + "\n" + firstTag);
        return firstTag + content + lastTag;
    }

    private String applyDropCapFormatting(String content) {
        if (content.isEmpty()) {
            return content;
        }

        if (!SettingUtils.IS_DROP_SELECTED || format != ExportFormat.HTML) {
            return content;
        }

        char firstChar = content.charAt(0);
        if (firstChar == Character.toLowerCase(firstChar)) {
            return content;
        }

        String dropCapSyntax = SettingUtils.DROP_SYNTAX;
        return dropCapSyntax.replace("[DROP]", String.valueOf(firstChar)) + content.substring(1);
    }

    /** Export format enumeration. */
    private enum ExportFormat {
        HTML(".html"),
        TEXT(".txt");

        private final String fileExtension;

        ExportFormat(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

    /** Export mode enumeration. */
    private enum ExportMode {
        SPLIT, // Separate files for each chapter
        COMBINED // Single combined file
    }

    /** Export options configuration. */
    private static class ExportOptions {
        final boolean makeToc;
        final boolean includeCss;

        ExportOptions(boolean makeToc, boolean includeCss) {
            this.makeToc = makeToc;
            this.includeCss = includeCss;
        }
    }
}
