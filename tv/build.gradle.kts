/**
 * Noctorium on a television: screens made for a remote's arrows and a room's distance -- Home's rows, search, your
 * library, the queue, radio, and the song playing, large.
 *
 * An Android library rather than an app of its own, as Noctorium-Android-Auto is. The TV runs the same Noctorium as a
 * phone -- the same services, sign-ins, queue and player -- and only the screens differ, so one APK installs on both:
 * Noctorium-Mobile includes this as :tv (from ../Noctorium-Android-TV, or its tv/ submodule) and opens
 * [app.noctorium.tv.NoctoriumTv] from the activity Android TV's home screen launches.
 */
plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.noctorium.tv"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    api(project(":core"))

    // The phone app's Compose, so the two agree.
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    // Compose for TV: cards and buttons that show where the remote's focus is.
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // The remote's Back.
    implementation("androidx.activity:activity-compose:1.9.3")
    // Covers, through the phone app's image loader when inside it.
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
