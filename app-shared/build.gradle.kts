plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    // AGP 9: com.android.library không tương thích KMP — dùng KML plugin
    id("com.android.kotlin.multiplatform.library")
    id("jacoco")
}

kotlin {
    jvmToolchain(17)

    jvm()
    // KML thay androidTarget{} + android{} block bên dưới (namespace/compileSdk/minSdk).
    // KGP 2.4: accessor tên `android` (androidLibrary{} deprecated)
    android {
        namespace = "dev.haipham22.leechtext.appshared"
        compileSdk = 37
        minSdk = 26
    }
    // P5.3: iOS — framework tĩnh nhúng vào iosApp.xcodeproj qua task
    // embedAndSignAppleFrameworkForXcode (build phase "Compile Kotlin")
    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()
    listOf(iosArm64Target, iosSimulatorArm64Target).forEach {
        it.binaries.framework {
            baseName = "appshared"
            isStatic = true
        }
    }
    // -Xexpect-actual-classes: trước đặt trong androidTarget{}.compilations — KML không
    // expose chỗ đó, top-level compilerOptions áp cho mọi target (flag vô hại với jvm)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // Code UI dùng chung desktop + Android (gọi :engine KMP) — intermediate
        // source set chuẩn KMP, THAY cho trick androidMain.srcDir(jvmMain)+exclude:
        // IDE không honor exclude nên 3 cặp actual (ImageDecoder/ShareFile/
        // PlatformDownload) báo "conflicting overloads" dù Gradle build green.
        // P5.3: sharedJvmMain (jvm+android share — reflection JVM-compat như
        // ResourceLocale/JFileChooser) + iosMain (actuals UIKit) tách từ sharedMain.
        val sharedMain = create("sharedMain") { dependsOn(commonMain.get()) }
        val sharedJvmMain = create("sharedJvmMain") { dependsOn(sharedMain) }
        val iosMain = create("iosMain") { dependsOn(sharedMain) }
        jvmMain { dependsOn(sharedJvmMain) }
        androidMain { dependsOn(sharedJvmMain) }
        iosArm64Main { dependsOn(iosMain) }
        iosSimulatorArm64Main { dependsOn(iosMain) }

        // Components UI common dùng chung mọi target hiện tại + tương lai (wasm/iOS)
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons.extended)
            implementation(libs.compose.components.resources)
        }
        // Screens + state components share desktop + Android — cần engine
        sharedMain.dependencies {
            implementation(project(":engine"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons.extended)
            // Decompose (nav + component lifecycle) + Koin (DI) — api vì entry modules
            // và screens reference types trực tiếp
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            api(libs.essenty.lifecycle.coroutines)
            api(libs.koin.core)
            api(libs.koin.compose)
            // UpdateCheck parse JSON GitHub API (parseToJsonElement — không cần compiler plugin)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
        }
        // JVM-only actuals (ImageDecoder/ShareFile/PlatformDownload desktop) —
        // nằm src/jvmMain, compile riêng cho desktop target
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs) // skiko native cho offscreen render
            // Main dispatcher cho essenty coroutineScope()/viewModelScope (như desktop-app)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            implementation(kotlin("test"))
        }
        // Android glue: actuals + AppContext/DownloadService — androidx phụ trợ
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.13.1")
            // PlatformBackHandler actual — BackHandler activity-compose
            implementation("androidx.activity:activity-compose:1.9.3")
        }
    }
}

compose {
    resources {
        packageOfResClass = "dev.haipham22.leechtext.resources"
    }
}

// Compose 1.12 chưa wire outputDirectory cho copyAndroidMainComposeResourcesToAndroidAssets
// khi dùng KML plugin (com.android.kotlin.multiplatform.library) — assets không đến APK,
// runtime crash MissingResourceException strings.cvr (2026-08-27). Gán tay + android-app
// nạp dir này vào assets (xem android-app/build.gradle.kts).
val composeAndroidAssets = layout.buildDirectory.dir("generated/composeAndroidAssets")
tasks.matching { it.name == "copyAndroidMainComposeResourcesToAndroidAssets" }.configureEach {
    // Task class internal — set outputDirectory qua reflection (getter public)
    javaClass.methods.first { it.name == "setOutputDirectory" && it.parameterCount == 1 }
        .invoke(this, composeAndroidAssets)
}

// Race compose-resources × parallel build (2026-08-26): compile không dependsOn
// generate accessors → với org.gradle.parallel=true, compileKotlinJvm/Android chạy
// khi accessor files chưa ghi xong → "Unresolved reference" string key mới
// (build lại lần 2 thì pass — dấu hiệu race kinh điển). Wire tay cho mọi compile.
tasks.matching { it.name.startsWith("compile") && it.name.endsWith("Kotlin") }.configureEach {
    dependsOn("generateResourceAccessorsForCommonMain")
}

/** Coverage JVM cho SonarQube (XML); jvmTest chạy kèm. */
tasks.register<JacocoReport>("jacocoJvmReport") {
    dependsOn(tasks.named("jvmTest"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(fileTree("${layout.buildDirectory.get()}/classes/kotlin/jvm/main"))
    sourceDirectories.setFrom(files("src/jvmMain/kotlin", "src/sharedMain/kotlin", "src/commonMain/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
}
