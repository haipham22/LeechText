package dev.haipham22.leechtext.action.export

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.models.Chapter
import dev.haipham22.leechtext.models.Properties
import dev.haipham22.leechtext.util.AppSettings
import dev.haipham22.leechtext.util.formatDateDDMMYYYY
import dev.haipham22.leechtext.util.regexFind
import dev.haipham22.leechtext.util.writeTo
import okio.FileSystem
import okio.Path.Companion.toPath

/** Regex thay <title> và footer HTML mục lục (format bản gốc). */
private const val TOC_TITLE_REGEX = "<title>.*?</title>"
private const val TOC_HTML_FOOTER = "</body>\n</html>"

/** Đuôi item manifest OPF cho trang xhtml (lặp mỗi item). */
private const val OPF_ITEM_XHTML_SUFFIX = " media-type=\"application/xhtml+xml\"/>\n"

/** Template ncx/opf inline (P5.2c — bỏ readResource classpath cho KMP; giữ nguyên
 * nội dung file dark/leech/res/{toc.ncx,content.opf}). */
private val TOC_NCX_TEMPLATE =
    """
    <?xml version="1.0" encoding="utf-8" ?>
    <!DOCTYPE ncx PUBLIC "-//NISO//DTD ncx 2005-1//EN"
     "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd">
    <ncx version="2005-1" xmlns="http://www.daisy.org/z3986/2005/ncx/">
      <head>
        <meta content="urn:uuid:e5cb99bd-0e41-4213-b61e-1b93b7240d30" name="dtb:uid"/>
        <meta content="0" name="dtb:depth"/>
        <meta content="0" name="dtb:totalPageCount"/>
        <meta content="0" name="dtb:maxPageNumber"/>
      </head>
      <docTitle>
        <text>[NAME] - [AUTHOR]</text>
      </docTitle>
      <navMap>
        <navPoint id="mucluc" playorder="0">
          <navLabel>
            <text>Mục lục</text>
          </navLabel>
          <content src="Text/mucluc.html"/>
        </navPoint>
    [NAVPOINT]  </navMap>
    </ncx>
    """.trimIndent()

private val CONTENT_OPF_TEMPLATE =
    """
    <?xml version="1.0" encoding="utf-8" standalone="yes"?>
    <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
      <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
        <dc:title>[NAME]</dc:title>
        <dc:creator opf:role="aut">[AUTHOR]</dc:creator>
        <dc:identifier id="BookId" opf:scheme="UUID">urn:uuid:e5cb99bd-0e41-4213-b61e-1b93b7240d30</dc:identifier>
        <dc:date>[DATE]</dc:date>
        <dc:language>vi</dc:language>
        <meta name="cover" content="cover"/>
        <meta content="2.0" name="Ebook Builder"/>
      </metadata>
      <manifest>
      [MANIFEST]    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
        <item id="stylesheet.css" href="stylesheet.css" media-type="text/css"/>
      [COVER]  [IMAGE]  </manifest>
      <spine toc="ncx">
      [NCX]</spine>
      <guide>
      </guide>
    </package>
    """.trimIndent()

/**
 * Sinh mục lục NCX/OPF/HTML cho export (port từ action/export/ToC.java).
 */
@Suppress("LongParameterList") // tham số log DI + params nghiệp vụ — bundle làm code khó đọc
class ToC(
    private val log: EngineLogger,
    private val properties: Properties,
    private val autoSplit: Boolean = false,
    private val includeImg: Boolean = false,
    private val settings: AppSettings,
) {
    private val chapList: List<Chapter> = properties.chapList ?: emptyList()
    private val charset: String = properties.charset ?: "UTF-8"
    private val tocBuilder = StringBuilder()
    private val contentBuilder = StringBuilder()
    private val htmlToCBuilder = StringBuilder()
    private val partList = ArrayList<String>()
    private val partNameList = ArrayList<String>()
    private var navPointId = 0

    /** Entry point sinh các file mục lục. */
    fun mkToC() {
        navPointId = 1

        htmlToCBuilder.append("\n<h4>Mục lục</h4>\n")

        if (properties.addGt) {
            addIntroductionSection()
        }

        // item mucluc trong manifest
        contentBuilder.append(
            "\t<item id=\"mucluc\" href=\"Text/mucluc.html\"" +
                OPF_ITEM_XHTML_SUFFIX,
        )

        if (!autoSplit) {
            makePart(0, chapList.size, "\t")
        } else {
            val partIndexes = splitPart()
            if (partIndexes.size < 2) {
                makePart(0, chapList.size, "\t")
            } else {
                generateMultiPartContent(partIndexes)
            }
        }

        saveData()
    }

    private fun addIntroductionSection() {
        htmlToCBuilder.append(
            "<div class=\"lv2\"><a href=\"../Text/gioithieu.html\">Giới Thiệu</a></div>\n",
        )
        tocBuilder.append(
            createNavPoint("gioithieu", "Giới Thiệu", "Text/gioithieu.html", navPointId++),
        )
        tocBuilder.append("</navPoint>\n")
        contentBuilder.append(
            "\t<item id=\"gioithieu\" href=\"Text/gioithieu.html\"" +
                OPF_ITEM_XHTML_SUFFIX,
        )
    }

    private fun generateMultiPartContent(partIndexes: List<Int>) {
        makePart(0, partIndexes[0], "    ")

        for (i in partIndexes.indices) {
            val partName = determinePartName(i, partIndexes)
            addVolumeSection(i, partName, partIndexes)
        }
    }

    private fun determinePartName(
        partIndex: Int,
        partIndexes: List<Int>,
    ): String {
        var name = chapList[partIndexes[partIndex]].partName ?: ""
        if (name.isEmpty()) {
            val startChapter = partIndex * DEFAULT_PART_SIZE + 1
            val endChapter =
                if (partIndex == partIndexes.size - 1) {
                    -1 // "Hết"
                } else {
                    (partIndex + 1) * DEFAULT_PART_SIZE
                }
            name = formatPartName(startChapter, endChapter)
        }
        return name
    }

    private fun formatPartName(
        start: Int,
        end: Int,
    ): String = "Chương " + start + "→" + (if (end == -1) "Hết" else end.toString())

    private fun addVolumeSection(
        volumeIndex: Int,
        volumeName: String,
        partIndexes: List<Int>,
    ) {
        val volumeId = "Q" + (volumeIndex + 1)

        tocBuilder.append(
            createNavPoint(
                "nav" + navPointId,
                volumeName,
                "Text/" + volumeId + ".html",
                navPointId++,
                "    ",
            ),
        )

        contentBuilder.append(
            "\t<item id=\"$volumeId\" href=\"Text/$volumeId.html\"" +
                OPF_ITEM_XHTML_SUFFIX,
        )

        partNameList.add(volumeName)

        val startIndex = partIndexes[volumeIndex]
        val endIndex =
            if (volumeIndex == partIndexes.size - 1) {
                chapList.size
            } else {
                partIndexes[volumeIndex + 1]
            }

        makePart(startIndex, endIndex, "      ")
        tocBuilder.append("    </navPoint>\n")
    }

    /**
     * Tìm điểm chia tập: part đánh dấu sẵn → marker "Quyển" → chương 1 reset → chia cố định
     * cho truyện dài.
     *
     * @return index chương where các tập chia
     */
    fun splitPart(): List<Int> {
        val splitPoints = ArrayList<Int>()

        // Strategy 1: chương đầu hợp lệ hoặc part đánh dấu sẵn
        val startIndex = findFirstSplitPoint()
        if (startIndex < chapList.size) {
            splitPoints.add(startIndex)
        }

        // Strategy 2: marker "Quyển"
        findVolumeIndicators(startIndex + 1, splitPoints)
        if (splitPoints.size > 1) {
            return splitPoints
        }

        // Strategy 3: chương 1 reset
        findChapterRestarts(startIndex + 1, splitPoints)
        if (splitPoints.size > 1) {
            return splitPoints
        }

        // Strategy 4: chia cố định cho truyện dài
        if (chapList.size >= AUTO_SPLIT_THRESHOLD) {
            createFixedSizeSplits(startIndex + 1, splitPoints)
        }

        return splitPoints
    }

    private fun findFirstSplitPoint(): Int {
        for (i in chapList.indices) {
            val chapter = chapList[i]
            if (!(chapter.partName ?: "").isEmpty()) return i
            val chapterNum = regexFind(chapter.chapName, CHAPTER_PATTERN, 1) ?: ""
            if (chapterNum.isNotEmpty()) return i
        }
        return 0
    }

    private fun findVolumeIndicators(
        startIndex: Int,
        splitPoints: MutableList<Int>,
    ) {
        var expectedVolume = FIRST_VOLUME_CHAPTER
        for (i in startIndex until chapList.size) {
            val volumeNum = regexFind(chapList[i].chapName, VOLUME_PATTERN, 2)
            if (parseInt(volumeNum) == expectedVolume) {
                splitPoints.add(i)
                expectedVolume++
            }
        }
    }

    private fun findChapterRestarts(
        startIndex: Int,
        splitPoints: MutableList<Int>,
    ) {
        for (i in startIndex until chapList.size) {
            val currentChapter = regexFind(chapList[i].chapName, CHAPTER_PATTERN, 1)
            if (parseInt(currentChapter) == 1) {
                val previousChapter = regexFind(chapList[i - 1].chapName, CHAPTER_PATTERN, 1)
                if (parseInt(previousChapter) != 1) {
                    splitPoints.add(i)
                }
            }
        }
    }

    private fun createFixedSizeSplits(
        startIndex: Int,
        splitPoints: MutableList<Int>,
    ) {
        var targetChapter = DEFAULT_PART_SIZE

        var i = startIndex
        while (i < chapList.size) {
            val chapterNum = regexFind(chapList[i].chapName, CHAPTER_PATTERN, 1)
            val currentChapter = parseInt(chapterNum)

            val difference = kotlin.math.abs(targetChapter - currentChapter)
            if (difference in 0..CHAPTER_TOLERANCE) {
                val adjustedIndex = findNearestChapterMatch(i, CHAPTER_SEARCH_RANGE, targetChapter)
                if (adjustedIndex != -1) {
                    i = adjustedIndex
                    splitPoints.add(i)
                    targetChapter += DEFAULT_PART_SIZE
                }
            }
            i++
        }
    }

    /**
     * Tìm chương gần target trong range.
     *
     * @return index match tốt nhất, -1 nếu không có
     */
    private fun findNearestChapterMatch(
        startPoint: Int,
        range: Int,
        targetChapter: Int,
    ): Int {
        for (offset in 0 until range) {
            val index = startPoint + offset
            if (index >= chapList.size) break
            val chapterNum = regexFind(chapList[index].chapName, CHAPTER_PATTERN, 1)
            if (parseInt(chapterNum) > targetChapter) return index
        }
        return -1
    }

    private fun makePart(
        startIndex: Int,
        endIndex: Int,
        indent: String,
    ) {
        val partHtml = StringBuilder()

        for (i in startIndex until endIndex) {
            val chapter = chapList[i]
            val chapterId = chapter.id ?: ""
            val chapterName = chapter.chapName ?: ""

            tocBuilder.append(
                createNavPoint(
                    "nav" + navPointId,
                    chapterName,
                    "Text/" + chapterId + ".html",
                    navPointId++,
                    indent,
                ),
            )
            tocBuilder.append(indent).append("</navPoint>\n")
            partHtml.append(createChapterLink(chapterId, chapterName))
            contentBuilder.append(
                "\t<item id=\"C$i\" href=\"Text/$chapterId.html\"" +
                    OPF_ITEM_XHTML_SUFFIX,
            )
        }

        partList.add(partHtml.toString())
    }

    private fun createNavPoint(
        id: String,
        label: String,
        src: String,
        playOrder: Int,
    ): String = createNavPoint(id, label, src, playOrder, "\t")

    private fun createNavPoint(
        id: String,
        label: String,
        src: String,
        playOrder: Int,
        indent: String,
    ): String = "$indent<navPoint id=\"$id\" playorder=\"$playOrder\">\n" +
        "$indent  <navLabel>\n" +
        "$indent    <text>${escapeXml(label)}</text>\n" +
        "$indent  </navLabel>\n" +
        "$indent  <content src=\"$src\"/>\n"

    private fun createChapterLink(
        chapterId: String,
        chapterName: String,
    ): String = "<div class=\"lv2\"><a href=\"../Text/$chapterId.html\">${escapeHtml(chapterName)}</a></div>\n"

    private fun saveData() {
        // toc.ncx
        val ncxContent =
            TOC_NCX_TEMPLATE
                .replace("[NAME]", properties.name ?: "")
                .replace("[AUTHOR]", properties.author ?: "")
                .replace("[NAVPOINT]", tocBuilder.toString())
        ncxContent.writeTo(properties.savePath + "/data/toc.ncx", charset, log)

        // content.opf
        val currentDate = formatDateDDMMYYYY()
        val manifest = contentBuilder.toString()
        val spine = manifest.replace("<item id=(.*?)\\s*href=.*?/>".toRegex(), "<itemref idref=$1/>")
        val opfContent =
            CONTENT_OPF_TEMPLATE
                .replace("[NAME]", properties.name ?: "")
                .replace("[AUTHOR]", properties.author ?: "")
                .replace("[DATE]", currentDate)
                .replace("[MANIFEST]", manifest)
                .replace("[COVER]", getCoverManifest())
                .replace("[IMAGE]", getImageManifest())
                .replace("[NCX]", spine)
        opfContent.writeTo(properties.savePath + "/data/content.opf", charset, log)

        // html files
        val htmlHeader = settings.htmlSyntax.replace("(?s)(.*?<body.*?>).*".toRegex(), "$1")
        if (!autoSplit) {
            (
                htmlHeader
                    .replace(TOC_TITLE_REGEX.toRegex(), "<title>Mục lục</title>") +
                    htmlToCBuilder.toString() +
                    partList[0] +
                    TOC_HTML_FOOTER
                ).writeTo(properties.savePath + "/data/Text/mucluc.html", charset, log)
        } else {
            saveMultiPartToCFiles(htmlHeader)
        }
    }

    private fun saveMultiPartToCFiles(header: String) {
        // Main ToC
        var mainToC =
            header
                .replace(TOC_TITLE_REGEX.toRegex(), "<title>Mục lục</title>") +
                htmlToCBuilder.toString()

        if (partList.isNotEmpty()) {
            mainToC += partList[0]
        }

        mainToC += TOC_HTML_FOOTER
        mainToC.writeTo(properties.savePath + "/data/Text/mucluc.html", charset, log)

        // File từng tập
        for (i in 1 until partList.size) {
            val volumeTitle = partNameList[i - 1]

            // Thêm link tập vào main ToC
            htmlToCBuilder.append(
                "<div class=\"lv2\"><a href=\"../Text/Q$i.html\">$volumeTitle</a></div>\n",
            )

            val volumeContent =
                header
                    .replace(TOC_TITLE_REGEX.toRegex(), "<title>" + volumeTitle + "</title>") +
                    "\n<h4>" + volumeTitle + "</h4>\n" +
                    partList[i] +
                    TOC_HTML_FOOTER
            volumeContent.writeTo(properties.savePath + "/data/Text/Q" + i + ".html", charset, log)
        }
    }

    private fun getImageManifest(): String {
        if (!includeImg) return "\n"

        val imagesDir = (properties.savePath ?: return "\n") + "/data/Images"
        val imageFiles =
            runCatching { FileSystem.SYSTEM.list(imagesDir.toPath()) }
                .getOrDefault(emptyList())
                .map { it.name }

        return buildString {
            for (filename in imageFiles) {
                val mimeType = imageMimeType(filename) ?: continue
                append("<item id=\"$filename\" href=\"Images/$filename\" media-type=\"$mimeType\"/>\n")
            }
        }
    }

    /** Ext file ảnh → mime OPF; không phải ảnh (subfolder, file lạ) → null. */
    private fun imageMimeType(filename: String): String? = when (filename.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        else -> null
    }

    /** Cover item theo file thật trong data/ — ext/mime sniff từ magic bytes (Ebook.ensureCoverFile). */
    private fun getCoverManifest(): String {
        val dataDir = properties.savePath ?: return ""
        for (ext in listOf("jpg", "png", "webp", "gif")) {
            val f = "$dataDir/data/cover.$ext"
            if (FileSystem.SYSTEM.exists(f.toPath())) {
                return "<item id=\"cover\" href=\"cover.$ext\" media-type=\"${imageMimeType("x.$ext")}\"/>\n"
            }
        }
        return ""
    }

    private fun parseInt(value: String?): Int {
        if (value.isNullOrEmpty()) return 0
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun escapeXml(input: String?): String = (input ?: "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun escapeHtml(input: String?): String = (input ?: "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    companion object {
        private const val CHAPTER_PATTERN = "Ch..ng\\s*(\\d+)"
        private const val VOLUME_PATTERN = "(Q|Quy.n\\s*)(\\d+)"
        private const val DEFAULT_PART_SIZE = 100
        private const val CHAPTER_SEARCH_RANGE = 10
        private const val CHAPTER_TOLERANCE = 5
        private const val AUTO_SPLIT_THRESHOLD = 300
        private const val FIRST_VOLUME_CHAPTER = 2
    }
}
