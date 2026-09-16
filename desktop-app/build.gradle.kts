import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":app-shared"))
    implementation(project(":engine"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation("io.sentry:sentry:8.53.0")
    // viewModelScope dùng Dispatchers.Main — desktop JVM cần bản Swing của Main dispatcher
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
}

// Application + jpackage native installer từng OS (task: packageReleaseBinaries) —
// cần build TRÊN OS đích. Dùng compose DSL thay `application` plugin (tránh đụng task run).
compose.desktop {
    application {
        mainClass = project.property("app.mainClass").toString() // nguồn: gradle.properties
        // Gradle daemon set java.awt.headless=true và JavaExec kế thừa — Compose cần display thật
        // apple.awt.application.name — macOS menu bar hiện tên app thay "MainKt"
        jvmArgs("-Djava.awt.headless=false", "-Dapple.awt.application.name=LeechText")
        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg, // macOS
                TargetFormat.Msi, // Windows
                TargetFormat.Deb, // Linux
            )
            packageName = "leechtext" // deb bắt buộc lowercase
            packageVersion = project.property("app.version").toString() // nguồn duy nhất: gradle.properties
            vendor = "haipham22"
            description = "LeechText — tải truyện từ nguồn plugin vBook"
            macOS { iconFile.set(rootProject.file("desktop-app/src/main/resources/icons/leechtext.icns")) }
            windows { iconFile.set(rootProject.file("desktop-app/src/main/resources/icons/leechtext.ico")) }
            linux { iconFile.set(rootProject.file("desktop-app/src/main/resources/icons/leechtext.png")) }
        }
        // Rhino sandbox + Browser chạy bằng reflection — ProGuard strip là gãy.
        // Disable hẳn: rules vốn no-op mà proguardReleaseJars fail (getStandardOutput null).
        buildTypes.release.proguard.isEnabled.set(false)
    }
}

// Fat jar — chạy không cần gradle: java -jar desktop-app/build/libs/leechtext-desktop-*.jar
// ponytail: jpackage (.app/.dmg) khi cần phân phối chính thức — fat jar đủ cho dev/dogfood
val fatJar = tasks.register<Jar>("fatJar") {
    // Tên pin kèm version — project không set version nên default là
    // "leechtext-desktop.jar" (không dash), CI smoke glob expect dash
    archiveFileName.set("leechtext-desktop-${project.property("app.version")}.jar")
    manifest { attributes["Main-Class"] = "dev.haipham22.leechtext.desktop.MainKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class", "module-info.class")
    from(sourceSets.main.get().output)
    // Lazy provider — resolve runtimeClasspath lúc execute (eager .get() phá KMP + config-on-demand)
    from(provider {
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
