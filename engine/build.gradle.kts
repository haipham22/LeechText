import java.io.File

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    // AGP 9: com.android.library không tương thích KMP — dùng KML plugin
    id("com.android.kotlin.multiplatform.library")
    id("jacoco")
}

kotlin {
    jvmToolchain(17)

    // P3: Android share toàn bộ code jvmMain (Rhino/okhttp/jsoup thuần JVM, không AWT).
    // iOS/wasm (P4) thêm sau theo design doc.
    jvm()
    // KML thay androidTarget{} + android{} block bên dưới (namespace/compileSdk/minSdk).
    // KGP 2.4: accessor tên `android` (androidLibrary{} deprecated)
    android {
        namespace = "dev.haipham22.leechtext.engine"
        compileSdk = 37
        minSdk = 26
    }
    // P5.1: macOS host target cho QuickJS seam slice (iOS engine gate) —
    // ./gradlew :engine:macosArm64Test chạy native trên host, không cần simulator
    macosArm64()
    // P5.2: iOS targets (compile gate; app iOS ở P5.3)
    iosArm64()
    iosSimulatorArm64()
    // -Xexpect-actual-classes: trước đặt trong androidTarget{}.compilations — KML không
    // expose chỗ đó, top-level compilerOptions áp cho mọi target (flag vô hại với jvm)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // P5.2a: intermediate source sets (pattern app-shared sharedMain) — hierarchy
        // template OFF nên wire dependsOn tay.
        // sharedJvmMain: toàn bộ engine code JVM-dùng-chung (Rhino/okhttp thuần JVM,
        // không AWT) compile cho cả jvm + android, THAY trick androidMain.srcDir(jvmMain).
        val sharedJvmMain = create("sharedJvmMain") { dependsOn(commonMain.get()) }
        // appleMain: QuickJS sandbox (quickjs-kt) cho macosArm64 + 2 iOS targets
        val appleMain = create("appleMain") { dependsOn(commonMain.get()) }
        jvmMain { dependsOn(sharedJvmMain) }
        androidMain { dependsOn(sharedJvmMain) }
        macosArm64Main { dependsOn(appleMain) }
        iosArm64Main { dependsOn(appleMain) }
        iosSimulatorArm64Main { dependsOn(appleMain) }
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            // P4 (design 2A): Gson không chạy wasm → kotlinx.serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            // P5.2b: file IO common (okio FileSystem KMP) — api vì EnginePaths.dataDir
            // (okio.Path) là public API consumed bởi app-shared/android-app
            api("com.squareup.okio:okio:3.18.1")
            // P5.2c: wrappers HTML (JSDocument/JSElement) + Html/Http API common
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
        }
        // Deps dùng chung jvm + android (đủ cho compile cả 2 target, trừ org.json/
        // playwright như ghi chú dưới)
        sharedJvmMain.dependencies {
            // Pin theo design doc: Rhino 1.7.15 là contract version
            implementation("org.mozilla:rhino:1.7.15")
            // P5.2b→c: okhttp 5 — HttpEngine.jvm actual (không có artifact native nên
            // common Http đi qua seam, okhttp chỉ ở sharedJvm)
            implementation("com.squareup.okhttp3:okhttp:5.5.0")
            // Logging: kotlin-logging facade + slf4j-simple in stdout (giữ output console như cũ);
            // trên Android slf4j-simple in System.err → logcat. Thiếu này thì NOP logger —
            // mọi EngineLog (lỗi install plugin...) vô hình trên Android
            implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
            implementation("org.slf4j:slf4j-simple:2.0.18")
            implementation("net.lingala.zip4j:zip4j:2.11.5")
        }
        jvmMain {
            // Browser.kt (Playwright desktop-only) tách dir riêng: giữ nó khỏi
            // androidMain/iOS — jvmMain giờ chỉ chứa desktop-only bits
            kotlin.srcDir("src/browserMain/kotlin")
        }
        jvmMain.dependencies {
            // org.json giữ cho tới khi fold sang Gson (design 2A) — CHỈ jvm:
            // Android có sẵn trong framework (thêm vào sẽ xung đột class)
            implementation("org.json:json:20230618")
            // Browser API — Playwright (headless Chromium, pass Cloudflare, network intercept)
            implementation("com.microsoft.playwright:playwright:1.62.0")
            // WebP decode cho EPUB cover (bìa nguồn webp — reader không render, 260907).
            // Thuần Java, SPI tự đăng ký; Android dùng BitmapFactory framework
            implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
        }
        // Apple deps: quickjs-kt (engine mục tiêu iOS) + ktor Darwin (HTTP — okhttp
        // không có artifact native); ksoup từ commonMain
        appleMain.dependencies {
            implementation("io.github.dokar3:quickjs-kt:1.0.14")
            implementation("io.ktor:ktor-client-core:3.5.2")
            implementation("io.ktor:ktor-client-darwin:3.5.2")
        }
        macosArm64Test {
            kotlin.srcDir("src/macosTest/kotlin")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // P4 ksoup feasibility gate (design doc): so sánh ksoup vs jsoup trên selector
        // plugin thật — pass thì wasm khả thi, fail thì CUT wasm. Chỉ test-scope.
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // jsoup: oracle so sánh trong KsoupGateTest (main đã dùng ksoup)
            implementation("org.jsoup:jsoup:1.22.1")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
        }
    }
}

// N4: sinh AppInfo.VERSION từ gradle.properties (app.version) — single source of truth,
// mọi nơi hiển thị/so sánh version đọc AppInfo.VERSION. File generated, không commit.
// Configuration cache: val script-level bị doLast capture như script reference →
// khai báo local trong configure block (chạy config-time) để doLast chỉ hold File/String.
val generateAppInfo =
    tasks.register("generateAppInfo") {
        val version = providers.gradleProperty("app.version").get()
        val outFile = File(projectDir, "src/commonMain/generated/dev/haipham22/leechtext/AppInfo.kt")
        outputs.file(outFile)
        doLast {
            outFile.parentFile.mkdirs()
            outFile.writeText(
                """
                package dev.haipham22.leechtext

                /** Sinh bởi :engine:generateAppInfo từ app.version — KHÔNG sửa tay, KHÔNG commit. */
                object AppInfo {
                    const val VERSION = "$version"
                }

                """.trimIndent(),
            )
        }
    }
kotlin.sourceSets.commonMain { kotlin.srcDir("src/commonMain/generated") }
// KML single-variant: compileDebugKotlinAndroid/compileReleaseKotlinAndroid → compileAndroidMain
tasks.matching { it.name in setOf("compileKotlinJvm", "compileKotlinMetadata", "compileAndroidMain", "compileKotlinMacosArm64", "compileKotlinIosArm64", "compileKotlinIosSimulatorArm64") }
    .configureEach { dependsOn(generateAppInfo) }
// detekt quét cả src/ (gồm generated dir) — Gradle đòi khai báo thứ tự với task ghi vào đó
tasks.matching { it.name == "detekt" }.configureEach { mustRunAfter(generateAppInfo) }
// Generated: spotless không format file máy sinh (regen sẽ ghi đè mỗi build)
spotless {
    kotlin { targetExclude("src/commonMain/generated/**") }
}

/** Coverage JVM cho SonarQube (XML); jvmTest chạy kèm. */
tasks.register<JacocoReport>("jacocoJvmReport") {
    dependsOn(tasks.named("jvmTest"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(fileTree("${layout.buildDirectory.get()}/classes/kotlin/jvm/main"))
    sourceDirectories.setFrom(files("src/sharedJvmMain/kotlin", "src/commonMain/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
}
