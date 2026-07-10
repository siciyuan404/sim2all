// CI 专用 init script：强制只使用官方仓库，避免阿里云镜像不稳定（502）导致整个仓库被禁用。
// 放置在 ~/.gradle/init.d/ 后会被所有 Gradle 构建自动应用。
beforeSettings {
    pluginManagement {
        repositories {
            mavenCentral()
            google()
            gradlePluginPortal()
        }
    }
    dependencyResolutionManagement {
        repositories {
            mavenCentral()
            google()
        }
    }
}
