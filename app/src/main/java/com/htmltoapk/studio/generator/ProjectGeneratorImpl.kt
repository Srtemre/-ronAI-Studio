package com.htmltoapk.studio.generator

import android.content.Context
import com.htmltoapk.studio.core.di.IoDispatcher
import com.htmltoapk.studio.core.util.FileUtil
import com.htmltoapk.studio.data.model.AdvancedConfig
import com.htmltoapk.studio.data.model.CacheMode
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.data.model.PermissionsConfig
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.data.model.SourceType
import com.htmltoapk.studio.data.model.WebViewConfig
import com.htmltoapk.studio.importer.SourceImporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ProjectGenerator {
    suspend fun generate(project: ProjectConfig): File
}

@Singleton
class ProjectGeneratorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: SourceImporter,
    @IoDispatcher private val io: CoroutineDispatcher
) : ProjectGenerator {

    override suspend fun generate(project: ProjectConfig): File = withContext(io) {
        val outRoot = File(context.filesDir, "generated/${project.id}_${sanitize(project.appName)}")
        FileUtil.deleteRecursivelySafe(outRoot)
        FileUtil.ensureDir(outRoot)

        // 1. Import source assets into app/src/main/assets/
        val projectDir = File(outRoot, "project").apply { mkdirs() }
        val mainDir = File(projectDir, "app/src/main").apply { mkdirs() }
        val assetsDir = File(mainDir, "assets").apply { mkdirs() }
        val import = importer.import(project.sourceType, project.sourceUri, projectDir)
        // Move produced assets dir contents into the project assets folder
        import.assetsDir.listFiles()?.forEach { file ->
            file.copyRecursively(File(assetsDir, file.name), overwrite = true)
        }
        val entryHtmlName = import.entryHtml.relativeTo(import.assetsDir).path.replace("\\", "/")

        // 2. Top-level project files
        writeRootFiles(projectDir, project)

        // 3. App module files
        writeAppModule(projectDir, project, entryHtmlName)

        // 4. Sources
        writeSources(projectDir, project, entryHtmlName)

        // 5. Resources
        writeResources(projectDir, project)

        // 6. Manifest
        writeManifest(projectDir, project)

        // 7. Wrapper / proguard
        writeBuildSupport(projectDir, project)

        projectDir
    }

    // --------------------------------------------------------------------------

    private fun writeRootFiles(projectDir: File, project: ProjectConfig) {
        FileUtil.writeText(File(projectDir, "settings.gradle.kts"), Templates.settingsGradle())
        FileUtil.writeText(File(projectDir, "build.gradle.kts"), Templates.rootBuildGradle())
        FileUtil.writeText(File(projectDir, "gradle.properties"), Templates.gradleProperties())
        FileUtil.writeText(File(projectDir, "README.md"), Templates.readme(project))
        FileUtil.writeText(File(projectDir, ".gitignore"), Templates.gitignore())
        FileUtil.writeText(File(projectDir, "gradle/wrapper/gradle-wrapper.properties"), Templates.wrapperProps())
        // Note: gradle-wrapper.jar and gradlew scripts are bundled separately (see ApkBuilder / docs).
    }

    private fun writeAppModule(projectDir: File, project: ProjectConfig, entryHtml: String) {
        val appDir = File(projectDir, "app")
        FileUtil.writeText(File(appDir, "build.gradle.kts"), Templates.appBuildGradle(project))
        FileUtil.writeText(File(appDir, "proguard-rules.pro"), Templates.proguardRules(project.packageName))
    }

    private fun writeSources(projectDir: File, project: ProjectConfig, entryHtml: String) {
        val pkg = project.packageName
        val srcRoot = File(projectDir, "app/src/main/java/${pkg.replace('.', '/')}")
        FileUtil.ensureDir(srcRoot)

        FileUtil.writeText(File(srcRoot, "${project.appSanitized()}App.kt"), Templates.appClass(project))
        FileUtil.writeText(File(srcRoot, "MainActivity.kt"), Templates.mainActivity(project, entryHtml))
        FileUtil.writeText(File(srcRoot, "WebViewScreen.kt"), Templates.webViewScreen(project))
    }

    private fun writeResources(projectDir: File, project: ProjectConfig) {
        val resDir = File(projectDir, "app/src/main/res")
        FileUtil.ensureDir(File(resDir, "values"))
        FileUtil.ensureDir(File(resDir, "drawable"))
        FileUtil.ensureDir(File(resDir, "mipmap-anydpi-v26"))

        FileUtil.writeText(File(resDir, "values/strings.xml"), Templates.stringsXml(project))
        FileUtil.writeText(File(resDir, "values/colors.xml"), Templates.colorsXml(project))
        FileUtil.writeText(File(resDir, "values/themes.xml"), Templates.themesXml(project))
        FileUtil.writeText(File(resDir, "drawable/ic_launcher_foreground.xml"), Templates.launcherForeground())
        FileUtil.writeText(File(resDir, "drawable/ic_launcher_background.xml"), Templates.launcherBackground(project))
        FileUtil.writeText(File(resDir, "mipmap-anydpi-v26/ic_launcher.xml"), Templates.adaptiveIcon())
        FileUtil.writeText(File(resDir, "mipmap-anydpi-v26/ic_launcher_round.xml"), Templates.adaptiveIcon())
    }

    private fun writeManifest(projectDir: File, project: ProjectConfig) {
        FileUtil.writeText(
            File(projectDir, "app/src/main/AndroidManifest.xml"),
            Templates.manifest(project)
        )
    }

    private fun writeBuildSupport(projectDir: File, project: ProjectConfig) {
        // Place gradlew + wrapper jar from bundled raw resources (if packaged) — copied at build time.
        // For now, instruct the user (and ApkBuilder) to copy from this app's bundled gradle distribution.
    }

    private fun sanitize(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "app" }

    private fun ProjectConfig.appSanitized(): String = sanitize(appName).replaceFirstChar { it.uppercase() }
}
