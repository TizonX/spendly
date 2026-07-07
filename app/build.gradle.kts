plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.expensetracker"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

fun showApkPopup(apkFile: File) {
    val folder = apkFile.parent
    exec {
        commandLine(
            "osascript",
            "-e", "set apkFolder to \"$folder\"",
            "-e", "try",
            "-e", "  set r to button returned of (display dialog \"APK generated successfully!\" & return & return & apkFolder with title \"Build Complete\" buttons {\"Dismiss\", \"Open in Finder\"} default button \"Open in Finder\")",
            "-e", "  if r is \"Open in Finder\" then do shell script \"open \" & quoted form of apkFolder",
            "-e", "end try"
        )
        isIgnoreExitValue = true
    }
}

afterEvaluate {
    // Covers: Build > Build APK(s) and Generate Signed APK wizard
    listOf("assembleDebug", "assembleRelease", "packageDebug", "packageRelease").forEach { taskName ->
        tasks.findByName(taskName)?.doLast {
            val variant = when {
                taskName.endsWith("Release") -> "release"
                else -> "debug"
            }
            // Check both standard build output and wizard output (app/release/)
            val candidates = listOf(
                layout.buildDirectory.dir("outputs/apk/$variant").get().asFile,
                projectDir.resolve(variant)
            )
            val apk = candidates
                .flatMap { dir -> if (dir.exists()) dir.walkTopDown().filter { it.name.endsWith(".apk") }.toList() else emptyList() }
                .maxByOrNull { it.lastModified() } ?: return@doLast
            showApkPopup(apk)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // ---------------------------
    // Room Database
    // ---------------------------
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.code.gson:gson:2.10.1")

}