package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.models.Trash
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.TypeUtils
import dev.haipham22.leechtext.util.normalizeVietnamese
import dev.haipham22.leechtext.util.readTextOrNull
import dev.haipham22.leechtext.util.regexFind
import dev.haipham22.leechtext.util.sanitizeHtml
import dev.haipham22.leechtext.util.stripParagraphTags
import dev.haipham22.leechtext.util.writeTo
import okio.IOException
import okio.Path.Companion.toPath

/** Hậu tố link chương trong TOC HTML (format bản gốc). */
private const val CHAPTER_LINK_SUFFIX = "</a></div>\n"

/**
 * Xuất nội dung sang text/HTML (port từ action/export/Text.java).
 * Split (mỗi chương 1 file) hoặc combined (1 file duy nhất).
 */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
class Text(
    private val log: EngineLogger,
    private val properties: Properties,
    type: Int,
    private val makeToc: Boolean,
    private val includeCss: Boolean,
    tach: Int,
    private val settings: AppSettings,
    private val progressListener: ProgressListener? = null,
) {
    private enum class ExportFormat(
        val fileExtension: String,
    ) {
        HTML(".html"),
        TEXT(".txt"),
    }

    private enum class ExportMode { SPLIT, COMBINED }

    private val format: ExportFormat = if (type == TypeUtils.HTML) ExportFormat.HTML else ExportFormat.TEXT
    private val exportMode: ExportMode = if (tach == 1) ExportMode.COMBINED else ExportMode.SPLIT
    private val charset: String = properties.charset ?: "UTF-8"
    private var templateSyntax: String = ""

    /** Entry point. */
    fun export() {
        initializeTemplate()
        exportContent(format.fileExtension)
    }

    private fun initializeTemplate() {
        templateSyntax =
            if (format == ExportFormat.HTML) {
                settings.htmlSyntax.replace("[Uu][Tt][Ff]-8".toRegex(), charset)
            } else {
                settings.txtSyntax
            }
    }

    private fun exportContent(fileExtension: String) {
        val chapters = properties.chapList ?: emptyList()
        val combinedContent = StringBuilder()

        if (exportMode == ExportMode.COMBINED) {
            combinedContent.append(createCombinedFileHeader())
        }

        if (properties.addGt) {
            exportIntroduction(fileExtension, combinedContent)
        }

        if (makeToc && format == ExportFormat.HTML) {
            exportTableOfContents(chapters, combinedContent)
        }

        exportChapters(chapters, fileExtension, combinedContent)

        if (exportMode == ExportMode.COMBINED) {
            finalizeCombinedFile(combinedContent, fileExtension)
        }
    }

    private fun createCombinedFileHeader(): String {
        if (format != ExportFormat.HTML) return ""

        var header = templateSyntax
        header = header.replace("<title>.*?</title>".toRegex(), "<title>" + properties.name + "</title>")
        header = header.replace("(?s)(.*?<body.*?>).*".toRegex(), "$1")

        if (includeCss) {
            header =
                header.replace(
                    "</head>",
                    "<style>\n" + settings.cssSyntax + "\n</style>\n</head>",
                )
        }
        return header
    }

    private fun exportIntroduction(
        fileExtension: String,
        combinedContent: StringBuilder,
    ) {
        val introduction =
            Chapter().apply {
                chapName = INTRODUCTION_NAME
                id = INTRODUCTION_ID
            }
        try {
            val content = processChapterContent(introduction, preserveParagraphs = false)
            saveChapterContent(content, introduction.id ?: "", fileExtension, combinedContent)
        } catch (e: Exception) {
            log.add("Failed to export introduction: " + e.message)
        }
    }

    private fun exportTableOfContents(
        chapters: List<Chapter>,
        combinedContent: StringBuilder,
    ) {
        val tableOfContents = buildTableOfContentsHtml(chapters)
        if (exportMode == ExportMode.SPLIT) {
            tableOfContents.writeTo(properties.savePath + "/data/Text/mucluc.html", charset, log)
        } else {
            combinedContent.append(tableOfContents)
        }
    }

    private fun buildTableOfContentsHtml(chapters: List<Chapter>): String {
        val tocBuilder = StringBuilder()
        tocBuilder.append("\n<h4>" + TABLE_OF_CONTENTS_TITLE + "</h4>\n")
        return if (exportMode == ExportMode.SPLIT) {
            tocBuilder.append(createSplitTableOfContents(chapters))
            wrapInHtmlDocument(tocBuilder.toString())
        } else {
            tocBuilder.append(createCombinedTableOfContents(chapters))
            tocBuilder.toString()
        }
    }

    private fun createSplitTableOfContents(chapters: List<Chapter>): String = buildString {
        if (properties.addGt) {
            append("<div class=\"lv2\"><a href=\"../Text/")
                .append(INTRODUCTION_ID)
                .append(".html\">")
                .append(INTRODUCTION_NAME)
                .append(CHAPTER_LINK_SUFFIX)
        }
        for (chapter in chapters) {
            append("<div class=\"lv2\"><a href=\"../Text/")
                .append(chapter.id)
                .append(".html\">")
                .append(formatChapterLinkText(chapter))
                .append(CHAPTER_LINK_SUFFIX)
        }
    }

    private fun createCombinedTableOfContents(chapters: List<Chapter>): String = buildString {
        if (properties.addGt) {
            append("<div class=\"lv2\"><a href=\"#")
                .append(INTRODUCTION_ID)
                .append("\">")
                .append(INTRODUCTION_NAME)
                .append(CHAPTER_LINK_SUFFIX)
        }
        for (chapter in chapters) {
            append("<div class=\"lv2\"><a href=\"#")
                .append(chapter.id)
                .append("\">")
                .append(formatChapterLinkText(chapter))
                .append(CHAPTER_LINK_SUFFIX)
        }
    }

    private fun formatChapterLinkText(chapter: Chapter): String {
        val partName = chapter.partName ?: ""
        val chapterName = chapter.chapName ?: ""
        return if (partName.isEmpty()) chapterName else "$partName - $chapterName"
    }

    private fun wrapInHtmlDocument(bodyContent: String): String {
        var document = templateSyntax
        document = document.replace("<title>.*?</title>".toRegex(), "<title>" + properties.name + "</title>")
        document = document.replace("(?s)(.*?<body.*?>).*".toRegex(), "$1")
        return document + bodyContent + "</body>\n</html>"
    }

    private fun exportChapters(
        chapters: List<Chapter>,
        fileExtension: String,
        combinedContent: StringBuilder,
    ) {
        var completedCount = 0
        val totalChapters = properties.size

        for (chapter in chapters) {
            if (!chapter.completed) continue
            try {
                val content = processChapterContent(chapter, preserveParagraphs = true)
                saveChapterContent(content, chapter.id ?: "", fileExtension, combinedContent)
                completedCount++
                reportProgress(completedCount, totalChapters)
            } catch (e: Exception) {
                log.add("Failed to export chapter " + chapter.chapName + ": " + e.message)
            }
        }
    }

    private fun reportProgress(
        completed: Int,
        total: Int,
    ) {
        if (progressListener != null && total > 0) {
            progressListener.setProgress(completed * PROGRESS_MULTIPLIER / total, PROGRESS_MESSAGE_FORMAT)
        }
    }

    private fun saveChapterContent(
        content: String,
        chapterId: String,
        fileExtension: String,
        combinedContent: StringBuilder,
    ) {
        if (exportMode == ExportMode.SPLIT) {
            content.writeTo(properties.savePath + "/data/Text/" + chapterId + fileExtension, charset, log)
        } else {
            var processedContent = extractBodyContent(content)
            if (format == ExportFormat.TEXT) processedContent += "\n\n"
            combinedContent.append(processedContent)
        }
    }

    private fun extractBodyContent(htmlContent: String): String = if (format == ExportFormat.HTML) {
        htmlContent.replace("(?s).*?<body.*?>(.*?)</body>.*".toRegex(), "$1")
    } else {
        htmlContent
    }

    private fun finalizeCombinedFile(
        combinedContent: StringBuilder,
        fileExtension: String,
    ) {
        if (format == ExportFormat.HTML) {
            combinedContent.append("</body>\n</html>")
        }
        combinedContent.toString().writeTo(properties.savePath + "/out/text" + fileExtension, charset, log)
    }

    /**
     * Load raw text → áp regex Trash + normalizeVietnamese → format theo template.
     *
     * @param preserveParagraphs có wrap `<p>` theo template [5] hay không
     */
    private fun processChapterContent(
        chapter: Chapter,
        preserveParagraphs: Boolean,
    ): String {
        val rawContent = loadRawChapterContent(chapter.id ?: "")
        // Junk `<p>` anti-scrape lồng lệch (truyenfull 260905) — strip trước khi
        // wrap `<p>` chuẩn, nếu không XHTML EPUB mismatch (sanitizer chỉ fix inline)
        // TXT: raw giữ HTML cho EPUB — phải đổi tag khối thành xuống dòng + strip tag
        // rác, không thì file text lẫn "<br>" literal suốt chương (dogfood 260906)
        val cleanedContent =
            if (format == ExportFormat.TEXT) {
                htmlToPlainText(cleanTextContent(rawContent))
            } else {
                stripParagraphTags(cleanTextContent(rawContent))
            }
        var formattedContent = formatChapterContent(chapter, cleanedContent, preserveParagraphs)
        if (format == ExportFormat.HTML) {
            formattedContent = sanitizeHtml(formattedContent)!!
        }
        return formattedContent
    }

    private fun loadRawChapterContent(chapterId: String): String = ((properties.savePath ?: "").toPath() / "raw/$chapterId$RAW_FILE_EXTENSION")
        .readTextOrNull(charset)
        ?: throw IOException("raw/$chapterId$RAW_FILE_EXTENSION")

    private fun cleanTextContent(content: String): String {
        var result = content
        for (trashRule: Trash in settings.trash) {
            if (trashRule.replace) {
                result = result.replace(unescapeTrashPattern(trashRule.src).toRegex(), unescapeTrashPattern(trashRule.to))
            }
        }
        return normalizeVietnamese(result)
    }

    /** HTML raw → text thuần cho TXT: `<br>`/block tag → xuống dòng, strip tag còn lại, decode entity. */
    private fun htmlToPlainText(content: String): String = stripParagraphTags(content)
        .replace(Regex("<br\\b[^>]*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?(div|p)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("\\n{3,}"), "\n\n")

    private fun unescapeTrashPattern(pattern: String?): String = pattern?.replace("\\n", "\n")?.replace("\\r", "\r")?.replace("\\t", "\t") ?: ""

    private fun formatChapterContent(
        chapter: Chapter,
        content: String,
        preserveParagraphs: Boolean,
    ): String {
        var template = templateSyntax

        // Thay placeholder bằng giá trị thực
        template = replaceTemplatePlaceholder(template, properties.name, "\\[1\\]", "NAME")
        template = replaceTemplatePlaceholder(template, properties.author, "\\[2\\]", "AUTHOR")
        template = replaceTemplatePlaceholder(template, chapter.partName, "\\[3\\]", "NAME_PART")
        template = replaceTemplatePlaceholder(template, chapter.chapName, "\\[4\\]", "NAME_CHAP")
        template = template.replace("[ID]", chapter.id ?: "")

        // Format đoạn văn
        val formattedContent = formatParagraphs(content, preserveParagraphs)
        val paragraphPlaceholder = regexFind(template, "(\\[5\\].*\\[PARAGRAPH\\].*\\[5\\])", 1)
        return if (paragraphPlaceholder != null) {
            template.replace(paragraphPlaceholder, formattedContent)
        } else {
            template
        }
    }

    private fun replaceTemplatePlaceholder(
        template: String,
        value: String?,
        placeholderTag: String,
        placeholderKey: String,
    ): String {
        if (value == null || value.length < 2) {
            return template.replace("\\s*$placeholderTag.*?$placeholderTag".toRegex(), "")
        }
        val pattern = placeholderTag + "(.*?" + "\\[" + placeholderKey + "\\]" + ".*?)" + placeholderTag
        val replacement = template.replace(pattern.toRegex(), "$1")
        return replacement.replace("[" + placeholderKey + "]", value)
    }

    private fun formatParagraphs(
        contentIn: String,
        preserveParagraphs: Boolean,
    ): String {
        if (contentIn.isEmpty()) return contentIn
        var content = applyDropCapFormatting(contentIn)
        if (preserveParagraphs) {
            content = wrapInParagraphTags(content, extractParagraphTemplate())
        }
        return content
    }

    private fun extractParagraphTemplate(): String {
        val firstTag = regexFind(templateSyntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 1) ?: ""
        val lastTag = regexFind(templateSyntax, "\\[5\\](.*)\\[PARAGRAPH\\](.*)\\[5\\]", 2) ?: ""
        return firstTag + "\n" + lastTag
    }

    private fun wrapInParagraphTags(
        content: String,
        paragraphTemplate: String,
    ): String {
        val parts = paragraphTemplate.split("\n")
        if (parts.size != 2) return content

        val firstTag = parts[0]
        val lastTag = parts[1]
        return firstTag + content.replace("\n", lastTag + "\n" + firstTag) + lastTag
    }

    private fun applyDropCapFormatting(content: String): String {
        if (content.isEmpty()) return content
        if (!settings.dropcapsEnabled || format != ExportFormat.HTML) return content

        val firstChar = content[0]
        if (firstChar == firstChar.lowercaseChar()) return content

        return settings.dropSyntax.replace("[DROP]", firstChar.toString()) + content.substring(1)
    }

    companion object {
        private const val RAW_FILE_EXTENSION = ".txt"
        private const val INTRODUCTION_ID = "gioithieu"
        private const val INTRODUCTION_NAME = "Giới Thiệu"
        private const val TABLE_OF_CONTENTS_TITLE = "Mục lục"
        private const val PROGRESS_MESSAGE_FORMAT = "[2/3]Exporting text..."
        private const val PROGRESS_MULTIPLIER = 100
    }
}
