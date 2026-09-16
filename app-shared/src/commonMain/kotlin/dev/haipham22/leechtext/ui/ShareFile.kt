package dev.haipham22.leechtext.ui

/**
 * Mở/chia sẻ file xuất (EPUB/TXT) ra ngoài app.
 * Desktop: mở Finder tại thư mục chứa file. Android: share sheet (app đọc sách, Drive...).
 */
expect fun shareFile(path: String)
