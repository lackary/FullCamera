import org.apache.commons.io.output.ByteArrayOutputStream
import org.bouncycastle.util.Integers

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs")
    id("kotlin-parcelize")
    id("kotlin-android")
    id("kotlin-kapt")

}

val gitTag = ByteArrayOutputStream().use {
    project.exec {
        executable("git")
        args("describe", "--abbrev=0", "--tag")
        standardOutput = it
    }
    String(it.toByteArray()).trim()
}

val gitCommitCount = ByteArrayOutputStream().use {
    project.exec {
        executable("git")
        args("rev-list", "--count", "HEAD")
        standardOutput = it
    }
    String(it.toByteArray()).trim().toInt()
}

android {
    namespace = "com.lacklab.app.fullcamera"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lacklab.app.fullcamera"
        minSdk = 30
        targetSdk = 34
        versionCode = gitCommitCount
        versionName = gitTag

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    val lifecycleVersion = "2.7.0"
    val navVersion = "2.7.7"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Logcat
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Unit Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}