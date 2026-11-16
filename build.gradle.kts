import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.ComposedJarTask
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Replaces the former Maven build (pom.xml + assembly/plugin-assembly.xml).
// 取代原先的 Maven 构建（pom.xml + assembly/plugin-assembly.xml）。
plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "com.workreport"
version = providers.gradleProperty("version").get()

repositories {
    // Aliyun mirrors Maven Central for this network; Maven Central stays as the fallback.
    // 阿里云镜像解决本机访问不到 Maven Central 的问题，官方仓库留作后备。
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()

    // IntelliJ Platform artifacts (IDE distributions, bundled plugins, verifier).
    // IntelliJ 平台制品仓库。
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Local install wins: no 1.5 GB IDE download for everyday builds.
        // CI (or a machine without IDEA) falls back to the release distribution.
        // 本机装了 IDEA 就用安装目录，省掉 ~1.5 GB 下载；CI 回退到在线发行包。
        val ideaHome = providers.environmentVariable("IDEA_HOME")
        if (ideaHome.isPresent) {
            local(file(ideaHome.get()))
        } else {
            intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        }

        // Git4Idea brings git4idea.* (GitRepository / GitLineHandler / GitCommand).
        // The Maven era needed git-backend because 2026.3 split that plugin into artifacts.
        // bundledPlugin("Git4Idea") 直接拿 IDE 自带的 Git 插件，不再关心制品拆分。
        bundledPlugin("Git4Idea")
    }

    // kotlin-stdlib / coroutines come from the IDE at runtime.
    // 标准库与协程由 IDE 提供，不进插件包。
    compileOnly(kotlin("stdlib"))

    // HTTP calls to the LLM endpoints; shipped with the plugin.
    // OkHttp drags in an old Kotlin runtime that would shadow the IDE-bundled one,
    // so the whole kotlin group is excluded from its transitive graph.
    // 调用 LLM 接口用的 OkHttp，随插件一起分发。它会传递拉进旧版 Kotlin 运行时，
    // 盖住 IDE 自带的那份，因此整条传递依赖里的 kotlin 组全部排除。
    implementation("com.squareup.okhttp3:okhttp:4.12.0") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // Keep the same language level as the Maven build.
        // 与 Maven 构建保持一致的语言级别。
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

// The project-level Kotlin DSL did not reach the compileKotlin task, so pin it here as well.
// 项目级 kotlin 配置没有落到 compileKotlin 任务上，这里再钉一次。
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

intellijPlatform {
    // No .form files and no @NotNull instrumentation needed: skip both, they are the slowest steps.
    // 没有 .form、也不需要 @NotNull 插桩，关掉这两步（它们最费时间）。
    instrumentCode = false
    buildSearchableOptions = false

    // Do not let the plugin widen the compatibility range to the local platform build.
    // 不让插件把兼容区间改成当前平台版本，保持 plugin.xml 的声明。
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "263.*"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Match the Maven-era jar name (it was derived from the artifactId).
// The plugin zip ships the composed jar, so both tasks get the same name.
// jar 名沿用 Maven 时代的 workreport-ai-assistant-<version>.jar：
// zip 里装的是 composedJar，所以两个任务都改。
tasks.jar {
    archiveBaseName.set("workreport-ai-assistant")
}

tasks.withType<ComposedJarTask>().configureEach {
    archiveBaseName.set("workreport-ai-assistant")
}

// Keep the artifact name the Maven build produced.
// 产物名沿用 Maven 时代的 workreport-ai-assistant-<version>.zip。
tasks.named<BuildPluginTask>("buildPlugin") {
    archiveFileName = "workreport-ai-assistant-${project.version}.zip"
}

// Bundle the packaged Agent Core (scripts/build_core.ps1 output) so users need no Python.
// 内置 Agent Core（scripts/build_core.ps1 的产物），用户机器不需要 Python。
tasks.named<PrepareSandboxTask>("prepareSandbox") {
    from(layout.projectDirectory.dir("core/dist")) {
        include("agent-core", "agent-core.exe")
        into("${rootProject.name}/agent-core")
    }
}
