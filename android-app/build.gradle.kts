plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.haipham22.leechtext"
    compileSdk = 37

    // AGP 9 tắt resValue mặc định — bật lại thay vì hardcode app_name 2 chỗ
    buildFeatures.resValues = true
    // Compose 1.12 × KML: assets resources của :app-shared không tự đến APK —
    // nạp output copyAndroidMainComposeResourcesToAndroidAssets (đã wire tay ở app-shared)
    sourceSets.getByName("main") {
        assets.srcDirs(project(":app-shared").file("build/generated/composeAndroidAssets"))
    }

    defaultConfig {
        applicationId = "dev.haipham22.leechtext"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        // Một nguồn version/name: gradle.properties (đồng bộ desktop)
        versionName = project.property("app.version").toString()
        resValue("string", "app_name", project.property("app.name").toString())
    }

    buildTypes {
        release {
            // R8 + ký debug-key — app cá nhân, cài trực tiếp được. Debug build không
            // R8 giải thích chậm cold start trên máy yếu (Skipped N frames).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }

    // lintVital embedded Kotlin 1.9 không đọc được metadata Kotlin 2.1 của stdlib
    // — release build fail oan. Tắt lint release (SonarQube lo phần chất lượng).
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":app-shared"))
    implementation(project(":engine"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    // Baseline Profile — AOT compile hot path startup, cắt "Skipped N frames"
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(project(":app-shared").tasks.named { it == "copyAndroidMainComposeResourcesToAndroidAssets" })
}
