import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.aboutlibraries.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "co.adityarajput.fileflow"
    compileSdk {
        version = release(36)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "co.adityarajput.fileflow"
        minSdk = 29
        targetSdk = 36
        versionCode = 7
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name_launcher", "FileFlow Debug")
            manifestPlaceholders["allowBackup"] = false
            buildConfigField("boolean", "HAS_NETWORK_FEATURE", "true")
        }
        create("nightly") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard/nightly.pro",
            )
            applicationIdSuffix = ".nightly"
            resValue("string", "app_name_launcher", "FileFlow Nightly")
            manifestPlaceholders["allowBackup"] = true
            buildConfigField("boolean", "HAS_NETWORK_FEATURE", "true")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard/release.pro",
            )
            manifestPlaceholders["allowBackup"] = true
            buildConfigField("boolean", "HAS_NETWORK_FEATURE", "false")
        }
        create("releaseWithNetwork") {
            initWith(getByName("release"))
            applicationIdSuffix = ".network"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "HAS_NETWORK_FEATURE", "true")
        }
    }
    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.aboutlibraries.compose)
    implementation(libs.cron.utils)
    implementation(libs.sshj)
    implementation(libs.google.crypto.tink)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
