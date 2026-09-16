# R8 rules — release build (ký debug-key, app cá nhân)

# jsoup optional dep
-dontwarn com.google.re2j.**

# Gson reflection — giữ nguyên field name các model serialize ra .plugin/.json
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class dev.haipham22.leechtext.models.** { *; }
-keep class dev.haipham22.leechtext.entities.** { *; }
-keep class dev.haipham22.leechtext.plugin.vbook.model.** { *; }

# Rhino — load class động, rename là gãy runtime
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.javascript.**

# zip4j
-dontwarn net.lingala.zip4j.**

# Playwright desktop-only (không vào APK nhưng tham chiếu compile)
-dontwarn com.microsoft.playwright.**

# SLF4J
-dontwarn org.slf4j.**
