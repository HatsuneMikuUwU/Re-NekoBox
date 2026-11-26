@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

setupApp()

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
        )
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }
    namespace = "io.nekohasekai.sagernet"
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {

    implementation(fileTree("libs"))

    val composeBom = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3:1.6.0-alpha01")
    // Backdrop Blur (exact same miuix-blur stack as AnimeX)
    implementation("top.yukonga.miuix.kmp:miuix-shader-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // CameraX + pure ZXing core (replaces legacy zxing-lite View library)
    val cameraXVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("com.google.zxing:core:3.5.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.work:work-multiprocess:2.9.1")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.3")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.jakewharton:process-phoenix:2.1.2")
    implementation("com.esotericsoftware:kryo:5.2.1")
    implementation("com.google.guava:guava:31.0.1-android")
    implementation("org.ini4j:ini4j:0.5.4")

    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

val buildHevTun by tasks.registering {
    val hevAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    val scriptPath = rootProject.file("buildScript/compile-hevtun.sh").absolutePath
    val workDir = rootProject.projectDir
    doLast {
        val missing = hevAbis.any { !file("src/main/jniLibs/$it/libhev-socks5-tunnel.so").exists() }
        if (missing || System.getenv("FORCE_HEV") == "1") {
            val proc = ProcessBuilder("bash", scriptPath)
                .directory(workDir)
                .inheritIO()
                .start()
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                throw GradleException("compile-hevtun.sh failed with exit code $exitCode")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(buildHevTun)
}
