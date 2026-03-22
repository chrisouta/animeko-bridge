import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "me.him188.ani"
version = "0.1"
description = "Desktop local media bridge for Animeko"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.simple)

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "me.him188.ani.localbridge.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            modules("java.desktop", "jdk.httpserver")
        }
        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Exe,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )
            packageName = "AnimekoLocalMediaBridge"
            description = project.description
            vendor = "OpenAni contributors"
            packageVersion = project.version.toString()
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
