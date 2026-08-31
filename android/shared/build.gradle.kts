import java.util.Properties
import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.androidRuntimeClasspath
import org.gradle.kotlin.dsl.buildkonfig
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.ktlint
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.sourceSets
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.buildkonfig)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val baseUrl = localProperties.getProperty("BASE_URL")
val testBaseUrl = localProperties.getProperty("TEST_BASE_URL")
val postHogApiKey = localProperties.getProperty("POSTHOG_API_KEY").orEmpty()
val postHogHost = localProperties.getProperty("POSTHOG_HOST") ?: "https://us.i.posthog.com"

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.yeobaek.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(project.dependencies.platform(libs.firebase.bom))
            // compose tooling
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)

            // ktor engine
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            // ktor engine
            implementation(libs.ktor.client.darwin)
        }

        commonMain.dependencies {
            // compose multiplatform
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            // lifecycle & viewmodel
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)

            // navigation
            implementation(libs.navigation.compose)

            // coroutines
            implementation(libs.kotlinx.coroutines.core)

            // network (ktor & ktorfit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktorfit.lib)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktorfit.converters.response)

            // kotlinx-serialization (json parsing)
            implementation(libs.kotlinx.serialization.json)

            // image loading (coil3)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            // multiplatform settings
            implementation(libs.multiplatform.settings)

            //
            implementation(libs.firebase.crashlytics)
        }

        commonTest.dependencies {
            // test
            implementation(libs.kotlin.test)
            implementation(libs.jetbrains.ui.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.named("runKtlintFormatOverCommonMainSourceSet") {
    dependsOn("kspCommonMainKotlinMetadata")
}

private val ktlintEngineVersion = libs.versions.ktlintEngine

ktlint {
    // 사용할 Ktlint 엔진 버전 명시
    version.set(ktlintEngineVersion)
    debug.set(false)
    verbose.set(true)

    // 안드로이드 타겟이 포함되어 있으므로 true로 설정
    android.set(true)

    // .editorconfig 파일을 적극적으로 준수하도록 강제
    enableExperimentalRules.set(true)

    filter {
        exclude { it.file.path.contains("/build/generated/") }
    }
}

buildkonfig {
    packageName = "com.yeobaek"

    defaultConfigs {
        buildConfigField(
            type = com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            name = "BASE_URL",
            value = baseUrl,
        )

        buildConfigField(
            type = com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            name = "TEST_BASE_URL",
            value = testBaseUrl,
        )

        buildConfigField(
            type = com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            name = "POSTHOG_API_KEY",
            value = postHogApiKey,
        )

        buildConfigField(
            type = com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            name = "POSTHOG_HOST",
            value = postHogHost,
        )
    }
}
