pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "doubao-tv"

include(":core:data")
include(":core:util")
include(":core:designsystem")
include(":core:settings")
include(":core:ui")
include(":feature:live")
include(":tv")
// mobile 是半成品，默认不参与 TV 构建；需要时用 -PincludeMobile=true 显式打开。
if (providers.gradleProperty("includeMobile").orNull == "true") {
    include(":mobile")
}
include(":ijkplayer-java")
include(":allinone")

val mediaSettingsFile = file("../media/core_settings.gradle")
if (mediaSettingsFile.exists()) {
    (gradle as ExtensionAware).extra["androidxMediaModulePrefix"] = "media3:"
    apply(from = mediaSettingsFile)
}

// 可选外部模块：目录存在时才引入，避免单仓独立构建失败
val tbsx5Dir = file("../YYKM/tbsx5")
if (tbsx5Dir.exists()) {
    include(":tbsx5")
    project(":tbsx5").projectDir = tbsx5Dir
}