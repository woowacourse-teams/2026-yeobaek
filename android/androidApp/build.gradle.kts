import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    // shared module
    implementation(project(":shared"))

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // compose & android
    implementation(libs.androidx.activity.compose)

    constraints {
        implementation(libs.androidx.fragment) {
            because("Activity Result APIs require Fragment 1.3.0 or newer.")
        }
    }

    // material3
    debugImplementation(libs.compose.material3)

    // navigation
    debugImplementation(libs.navigation.compose)

    // kotlinx serialization
    debugImplementation(libs.kotlinx.serialization.json)

    // compose tooling (preview & debug)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.foundation)
    debugImplementation(libs.compose.material3)
    debugImplementation(libs.androidx.lifecycle.viewmodelCompose)

    // coroutines (android dispatcher)
    implementation(libs.kotlinx.coroutines.android)

    // analytics
    implementation(libs.posthog.android)

    // test
    androidTestImplementation(libs.androidx.ui.test.junit4.android)
    debugImplementation(libs.ui.test.manifest)
}

android {
    namespace = "com.yeobaek"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.yeobaek"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = "${libs.versions.android.versionCode.get()}${libs.versions.android.majorVersion.get()}${libs.versions.android.minorVersion.get()}${libs.versions.android.patchVersion.get()}".toInt()
        versionName = "${libs.versions.android.majorVersion.get()}.${libs.versions.android.minorVersion.get()}.${libs.versions.android.patchVersion.get()}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
