package dev.haipham22.leechtext.util.zip

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.gmtime_r
import platform.posix.time
import platform.posix.time_tVar
import platform.posix.tm
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.crc32
import platform.zlib.deflate
import platform.zlib.deflateBound
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2_
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream
import platform.zlib.zlibVersion

/**
 * Byte-level ZIP codec cho Apple (P5.4) — KHÔNG dependency mới: tự parse central directory
 * (đọc entry list + raw compressed bytes) + platform.zlib cho raw inflate/deflate (windowBits
 * -15) + CRC32.
 *
 * Chỉ hỗ trợ subset cần cho engine: method 0 (STORED) + 8 (DEFLATE), zip chuẩn (không
 * ZIP64, không mã hoá) — plugin .plugin và EPUB đều nhỏ (< 100MB, cap của
 * ZipSecurityValidator).
 */
internal object ZipAppleCodec {
    private const val EOCD_SIG = 0x06054b50
    private const val CD_SIG = 0x02014b50
    private const val LOCAL_SIG = 0x04034b50
    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATE = 8

    /** Cap mỗi entry khi nén/giải nén (Int index) — zips engine xử lý đều nhỏ hơn xa. */
    private const val MAX_ENTRY_BYTES = 512L * 1024 * 1024

    /** 1 entry trong central directory — đủ dữ liệu để đọc/ghi lại raw data. */
    internal class Entry(
        val name: String,
        val method: Int,
        val crc: Long,
        val compressedSize: Int,
        val size: Int,
        val dosTime: Int,
        val dosDate: Int,
        val isDirectory: Boolean,
        val localOffset: Int = 0,
    ) {
        /** Raw compressed bytes (đã giải nén nếu [decompressed] = true thì [data] là nội dung). */
        var data: ByteArray = ByteArray(0)

        /** true nếu [data] đang là nội dung gốc (chưa nén) — ghi ra zip thì nén lại theo level. */
        var decompressed: Boolean = false
    }

    /** Entry thư mục ("Text/") — STORED rỗng, flag directory. */
    fun dirEntry(name: String): Entry {
        val (t, d) = dosDateTime()
        return Entry(
            name = if (name.endsWith("/")) name else "$name/",
            method = METHOD_STORED,
            crc = 0,
            compressedSize = 0,
            size = 0,
            dosTime = t,
            dosDate = d,
            isDirectory = true,
        )
    }

    // ---------- u16/u32 little-endian ----------

    private fun u16(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, o: Int): Long = (u16(b, o).toLong() and 0xFFFF) or ((u16(b, o + 2).toLong() and 0xFFFF) shl 16)

    private fun put16(
        b: ByteArray,
        o: Int,
        v: Int,
    ) {
        b[o] = (v and 0xFF).toByte()
        b[o + 1] = ((v shr 8) and 0xFF).toByte()
    }

    private fun put32(
        b: ByteArray,
        o: Int,
        v: Long,
    ) {
        b[o] = (v and 0xFF).toByte()
        b[o + 1] = ((v shr 8) and 0xFF).toByte()
        b[o + 2] = ((v shr 16) and 0xFF).toByte()
        b[o + 3] = ((v shr 24) and 0xFF).toByte()
    }

    // ---------- đọc (EOCD → central directory) ----------

    /** Tìm End Of Central Directory (quét ngược từ cuối, chịu zip comment ≤ 64KB). */
    fun findEocd(b: ByteArray): Int {
        if (b.size < 22) error("Not a zip file (too small)")
        var i = b.size - 22
        val floor = maxOf(0, b.size - 22 - 65536)
        while (i >= floor) {
            if (u32(b, i) == EOCD_SIG.toLong()) return i
            i--
        }
        error("Not a zip file (EOCD not found)")
    }

    /**
     * Đọc toàn bộ central directory → entry list (chưa có data — fill qua
     * [readLocalData] khi cần nội dung).
     */
    fun parseCentralDirectory(b: ByteArray): List<Entry> {
        val eocd = findEocd(b)
        val count = u16(b, eocd + 10)
        val cdOffset = u32(b, eocd + 16).toInt()
        if (count == 0xFFFF || cdOffset <= 0) error("ZIP64 not supported")

        val entries = ArrayList<Entry>(count)
        var o = cdOffset
        repeat(count) {
            if (o + 46 > b.size || u32(b, o) != CD_SIG.toLong()) {
                error("Corrupt central directory at $o")
            }
            val nameLen = u16(b, o + 28)
            val extraLen = u16(b, o + 30)
            val commentLen = u16(b, o + 32)
            val extAttrs = u32(b, o + 38)
            val name = b.decodeToString(o + 46, o + 46 + nameLen)
            entries.add(
                Entry(
                    name = name,
                    method = u16(b, o + 10),
                    crc = u32(b, o + 16),
                    compressedSize = u32(b, o + 20).toInt(),
                    size = u32(b, o + 24).toInt(),
                    dosTime = u16(b, o + 12),
                    dosDate = u16(b, o + 14),
                    isDirectory = name.endsWith("/") || (extAttrs and 0x10L) != 0L,
                    localOffset = u32(b, o + 42).toInt(),
                ),
            )
            o += 46 + nameLen + extraLen + commentLen
        }
        return entries
    }

    /** Đọc raw compressed data của 1 entry (đi qua local header lấy data offset). */
    fun readLocalData(
        b: ByteArray,
        entry: Entry,
    ): ByteArray {
        val localOffset = entry.localOffset
        if (localOffset + 30 > b.size || u32(b, localOffset) != LOCAL_SIG.toLong()) {
            error("Corrupt local header at $localOffset for ${entry.name}")
        }
        val nameLen = u16(b, localOffset + 26)
        val extraLen = u16(b, localOffset + 28)
        val start = localOffset + 30 + nameLen + extraLen
        val end = start + entry.compressedSize
        if (end > b.size) error("Entry data truncated: ${entry.name}")
        return b.copyOfRange(start, end)
    }

    // ---------- zlib ----------

    /** CRC32 qua platform.zlib. */
    @OptIn(ExperimentalForeignApi::class)
    fun crc32(data: ByteArray): Long = data.usePinned { p ->
        crc32(0uL, p.addressOf(0).reinterpret(), data.size.toUInt()).toLong() and 0xFFFFFFFFL
    }

    /** Raw inflate (windowBits -15 — zip entry không có zlib header), kích thước đã biết từ CD. */
    @OptIn(ExperimentalForeignApi::class)
    fun inflateRaw(
        data: ByteArray,
        expectedSize: Int,
    ): ByteArray {
        if (expectedSize < 0 || expectedSize.toLong() > MAX_ENTRY_BYTES) {
            error("Entry size out of range: $expectedSize")
        }
        val out = ByteArray(expectedSize)
        memScoped {
            val zs = alloc<z_stream>()
            check(inflateInit2_(zs.ptr, -15, zlibVersion()?.toKString(), sizeOf<z_stream>().toInt()) == Z_OK) {
                "zlib inflateInit2 failed"
            }
            try {
                data.usePinned { pinIn ->
                    out.usePinned { pinOut ->
                        inflatePinned(zs.ptr, pinIn, data.size, pinOut, expectedSize)
                    }
                }
            } finally {
                inflateEnd(zs.ptr)
            }
        }
        return out
    }

    /** Vòng inflate trên buffer đã pin — Z_OK hết chỗ = data to hơn size khai báo trong CD. */
    @OptIn(ExperimentalForeignApi::class)
    private fun inflatePinned(
        zsPtr: CPointer<z_stream>,
        pinIn: Pinned<ByteArray>,
        inputSize: Int,
        pinOut: Pinned<ByteArray>,
        expectedSize: Int,
    ) {
        zsPtr.pointed.next_in = pinIn.addressOf(0).reinterpret()
        zsPtr.pointed.avail_in = inputSize.toUInt()
        zsPtr.pointed.next_out = pinOut.addressOf(0).reinterpret()
        zsPtr.pointed.avail_out = expectedSize.toUInt()
        while (true) {
            when (val rc = inflate(zsPtr, Z_NO_FLUSH)) {
                Z_STREAM_END -> break

                Z_OK ->
                    if (zsPtr.pointed.avail_out == 0u) {
                        error("Inflated data larger than declared size")
                    }

                else -> error("zlib inflate error $rc")
            }
        }
    }

    /** Raw deflate (windowBits -15) — output size qua deflateBound, nén 1 lần Z_FINISH. */
    @OptIn(ExperimentalForeignApi::class)
    fun deflateRaw(
        data: ByteArray,
        level: Int,
    ): ByteArray {
        memScoped {
            val zs = alloc<z_stream>()
            check(
                deflateInit2_(
                    zs.ptr,
                    level,
                    Z_DEFLATED,
                    -15,
                    8,
                    Z_DEFAULT_STRATEGY,
                    zlibVersion()?.toKString(),
                    sizeOf<z_stream>().toInt(),
                ) == Z_OK,
            ) { "zlib deflateInit2 failed" }
            try {
                val bound = deflateBound(zs.ptr, data.size.toULong()).toInt()
                val out = ByteArray(bound)
                data.usePinned { pinIn ->
                    out.usePinned { pinOut ->
                        zs.next_in = pinIn.addressOf(0).reinterpret()
                        zs.avail_in = data.size.toUInt()
                        zs.next_out = pinOut.addressOf(0).reinterpret()
                        zs.avail_out = bound.toUInt()
                        check(deflate(zs.ptr, Z_FINISH) == Z_STREAM_END) { "zlib deflate error" }
                        return out.copyOf(bound - zs.avail_out.toInt())
                    }
                }
                error("unreachable")
            } finally {
                deflateEnd(zs.ptr)
            }
        }
    }

    /** Nén/chuẩn bị entry sẵn sàng ghi: method + data nén + crc + dos time hiện tại. */
    fun prepare(
        name: String,
        content: ByteArray,
        level: Int,
    ): Entry {
        val stored = level <= 0 || content.size < 64 // quá nhỏ nén thường to hơn — store
        val data =
            if (stored) {
                content
            } else {
                deflateRaw(content, level)
            }
        val (t, d) = dosDateTime()
        return Entry(
            name = name,
            method = if (stored) METHOD_STORED else METHOD_DEFLATE,
            crc = crc32(content),
            compressedSize = data.size,
            size = content.size,
            dosTime = t,
            dosDate = d,
            isDirectory = false,
        ).also {
            it.data = data
            it.decompressed = stored
        }
    }

    // ---------- ghi (local headers + central dir + EOCD) ----------

    /**
     * Ghi list entry thành zip bytes hoàn chỉnh (từng entry phải có [Entry.data] nén sẵn).
     * Flag bit 0x0800 (UTF-8 names) — tên chương/tiêu đề tiếng Việt phải giữ đúng Unicode.
     */
    fun writeZip(entries: List<Entry>): ByteArray {
        val utf8Flag = 0x0800
        val nameBytes = entries.map { it.name.encodeToByteArray() }
        val localSizes = entries.mapIndexed { i, e -> 30 + nameBytes[i].size + e.data.size }
        val bodySize = localSizes.sum()
        val cdSize = entries.indices.sumOf { 46 + nameBytes[it].size }
        val out = ByteArray(bodySize + cdSize + 22)
        var o = 0
        val offsets = ArrayList<Int>(entries.size)
        for ((i, e) in entries.withIndex()) {
            offsets.add(o)
            put32(out, o, LOCAL_SIG.toLong())
            put16(out, o + 4, 20) // version needed
            put16(out, o + 6, utf8Flag)
            put16(out, o + 8, e.method)
            put16(out, o + 10, e.dosTime)
            put16(out, o + 12, e.dosDate)
            put32(out, o + 14, e.crc)
            put32(out, o + 18, e.compressedSize.toLong())
            put32(out, o + 22, e.size.toLong())
            put16(out, o + 26, nameBytes[i].size)
            put16(out, o + 28, 0) // extra len
            nameBytes[i].copyInto(out, o + 30)
            e.data.copyInto(out, o + 30 + nameBytes[i].size)
            o += localSizes[i]
        }
        val cdOffset = o
        for ((i, e) in entries.withIndex()) {
            put32(out, o, CD_SIG.toLong())
            put16(out, o + 4, 20) // version made by
            put16(out, o + 6, 20) // version needed
            put16(out, o + 8, utf8Flag)
            put16(out, o + 10, e.method)
            put16(out, o + 12, e.dosTime)
            put16(out, o + 14, e.dosDate)
            put32(out, o + 16, e.crc)
            put32(out, o + 20, e.compressedSize.toLong())
            put32(out, o + 24, e.size.toLong())
            put16(out, o + 28, nameBytes[i].size)
            put16(out, o + 30, 0) // extra
            put16(out, o + 32, 0) // comment
            put16(out, o + 34, 0) // disk start
            put16(out, o + 36, 0) // internal attrs
            put32(out, o + 38, if (e.isDirectory) 0x10L or (0x81ED shl 16) else 0x81A4 shl 16) // external attrs
            put32(out, o + 42, offsets[i].toLong())
            nameBytes[i].copyInto(out, o + 46)
            o += 46 + nameBytes[i].size
        }
        put32(out, o, EOCD_SIG.toLong())
        put16(out, o + 4, 0) // disk
        put16(out, o + 6, 0) // cd disk
        put16(out, o + 8, entries.size)
        put16(out, o + 10, entries.size)
        put32(out, o + 12, cdSize.toLong())
        put32(out, o + 16, cdOffset.toLong())
        put16(out, o + 20, 0) // comment len
        return out
    }

    /** epoch → (DOS time, DOS date); gmtime cho timestamp zip (cosmetic). */
    @OptIn(ExperimentalForeignApi::class)
    private fun dosDateTime(): Pair<Int, Int> = memScoped {
        val t = alloc<time_tVar>()
        time(t.ptr)
        val tmv = alloc<tm>()
        gmtime_r(t.ptr, tmv.ptr)
        val year = tmv.tm_year + 1900
        val time = (tmv.tm_hour shl 11) or (tmv.tm_min shl 5) or (tmv.tm_sec / 2)
        val date = ((year - 1980) shl 9) or ((tmv.tm_mon + 1) shl 5) or tmv.tm_mday
        time to date
    }
}
