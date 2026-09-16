package dev.haipham22.leechtext.util

// Font subset chỉ có glyph NFC (ạ, ữ...) — thiếu combining marks NFD (U+0302, U+031B...),
// text NFD từ nguồn web render tofu. Chuẩn hoá mọi chuỗi từ plugin về NFC.
expect fun String.nfc(): String
