plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint)
}

private val ktlintPluginId = libs.plugins.ktlint.get().pluginId
private val ktlintEngineVersion = libs.versions.ktlintEngine

subprojects {
    apply(plugin = ktlintPluginId)

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
}
