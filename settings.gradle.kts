// Plugin resolution: the Aliyun mirror is listed first because this machine cannot reach
// plugins.gradle.org / repo1.maven.org directly; the official portal stays as a fallback.
// 插件解析：本机无法直连 plugins.gradle.org / repo1.maven.org，阿里云镜像放在前面，
// 官方 Portal 作为后备（CI 或网络放开的机器会自然命中官方源）。
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
    }
}

// The project name becomes the top-level folder inside the plugin zip.
// Keep it identical to the Maven-era layout: <plugin id>/{lib,agent-core}.
// 项目名会成为插件 zip 的顶层目录，保持与 Maven 时代一致：<插件 id>/{lib,agent-core}。
rootProject.name = "com.workreport.ai-assistant"
