package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.builder.ApkBuildEngine
import com.example.builder.ApkValidator
import com.example.builder.BuildStep
import com.example.builder.KeystoreConfig
import com.example.builder.KeystoreManager
import com.example.domain.model.DisplayMode
import com.example.domain.model.Orientation
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.util.ProjectFileManager
import com.example.util.SecurityValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionQaEndToEndVerificationTest {

    private lateinit var context: Context
    private lateinit var fileManager: ProjectFileManager
    private lateinit var buildEngine: ApkBuildEngine
    private lateinit var apkValidator: ApkValidator
    private lateinit var keystoreManager: KeystoreManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fileManager = ProjectFileManager(context)
        buildEngine = ApkBuildEngine(context, fileManager)
        apkValidator = ApkValidator()
        keystoreManager = KeystoreManager(context)
    }

    /**
     * TEST 1 & TEST 2 & TEST 4:
     * LOCAL HTML Project -> Full compilation -> REAL APK generated and verified.
     */
    @Test
    fun testLocalHtmlProjectGeneratesRealValidatedSignedApk() = runBlocking {
        val projectId = 1001L

        // 1. Create index.html with rich HTML/CSS/JS and relative assets
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Test App</title>
              <link rel="stylesheet" href="style.css">
            </head>
            <body>
              <div id="root">
                <h1>Hello Test App</h1>
                <p id="desc">Testing dynamic web app compilation</p>
                <button id="btn" onclick="handleClick()">Click Me</button>
              </div>
              <script src="script.js"></script>
            </body>
            </html>
        """.trimIndent()

        val cssContent = """
            body { font-family: sans-serif; background: #f0f2f5; color: #111; }
            h1 { color: #007aff; }
        """.trimIndent()

        val jsContent = """
            function handleClick() {
                document.getElementById('desc').innerText = 'Button clicked!';
            }
        """.trimIndent()

        fileManager.saveTextFile(projectId, "index.html", htmlContent)
        fileManager.saveTextFile(projectId, "style.css", cssContent)
        fileManager.saveTextFile(projectId, "script.js", jsContent)

        val project = Project(
            id = projectId,
            name = "Test App",
            packageName = "com.zoneland.testapp",
            version = "1.0.0",
            versionCode = 1,
            sourceType = SourceType.HTML,
            targetUrl = "index.html",
            htmlContent = htmlContent,
            displayMode = DisplayMode.STANDALONE,
            orientation = Orientation.PORTRAIT,
            enableJavaScript = true,
            enableLocalStorage = true,
            enableOfflineCaching = true,
            iconColorHex = "#007AFF"
        )

        // 2. Build and sign APK
        val buildResult = buildEngine.buildAndSignApk(
            project = project,
            keystoreConfig = KeystoreConfig(alias = "releasekey")
        )

        // 3. Verification of build outcome
        assertTrue("Build must succeed", buildResult.isSuccess)
        assertEquals(BuildStep.COMPLETE, buildResult.finalStep)
        assertNotNull("Generated APK file must not be null", buildResult.apkFile)

        val apkFile = buildResult.apkFile!!
        assertTrue("Generated APK file must exist on disk", apkFile.exists())
        assertTrue("Generated APK file size must be non-empty", apkFile.length() > 1000)

        // 4. TEST 3 Verification: Download file extension
        assertTrue("Generated APK name must end with .apk", apkFile.name.endsWith(".apk"))
        assertFalse("Generated APK name must not end with .zip", apkFile.name.endsWith(".zip"))
        assertFalse("Generated APK name must not end with .html", apkFile.name.endsWith(".html"))
        assertFalse("Generated APK name must not end with .txt", apkFile.name.endsWith(".txt"))

        // 5. TEST 2 Verification: Deep APK verification
        val validation = apkValidator.validateApk(apkFile, project)
        assertTrue("ApkValidator must confirm APK validity: ${validation.errors}", validation.isValid)
        assertTrue(validation.errors.isEmpty())

        val meta = validation.metadata
        assertNotNull("Metadata must be extracted", meta)
        assertEquals("Package name must match", "com.zoneland.testapp", meta?.packageName)
        assertEquals("Version name must match", "1.0.0", meta?.versionName)
        assertEquals("Version code must match", 1, meta?.versionCode)
        assertTrue("APK must be signed with certificate", meta?.isSigned == true)
        assertTrue("APK must contain entries", (meta?.entryCount ?: 0) > 0)

        // Verify web assets and core binary files packaged inside APK
        val zip = ZipFile(apkFile)
        assertNotNull("AndroidManifest.xml must exist in APK", zip.getEntry("AndroidManifest.xml"))
        assertNotNull("classes.dex must exist in APK", zip.getEntry("classes.dex"))
        assertNotNull("resources.arsc must exist in APK", zip.getEntry("resources.arsc"))
        assertNotNull("assets/index.html must exist in APK", zip.getEntry("assets/index.html"))
        assertNotNull("assets/style.css must exist in APK", zip.getEntry("assets/style.css"))
        assertNotNull("assets/script.js must exist in APK", zip.getEntry("assets/script.js"))
        assertNotNull("assets/app_meta.json must exist in APK", zip.getEntry("assets/app_meta.json"))
        assertNotNull("META-INF/MANIFEST.MF must exist in APK", zip.getEntry("META-INF/MANIFEST.MF"))
        assertNotNull("META-INF/CERT.SF must exist in APK", zip.getEntry("META-INF/CERT.SF"))
        assertNotNull("META-INF/CERT.RSA must exist in APK", zip.getEntry("META-INF/CERT.RSA"))
        zip.close()
    }

    /**
     * TEST 5: Project Persistence and Data Integrity
     */
    @Test
    fun testProjectPersistenceAndFileIsolation() {
        val projId1 = 2001L
        val projId2 = 2002L

        fileManager.saveTextFile(projId1, "index.html", "<h1>Project 1</h1>")
        fileManager.saveTextFile(projId2, "index.html", "<h1>Project 2</h1>")

        val proj1Files = fileManager.listProjectFiles(projId1)
        val proj2Files = fileManager.listProjectFiles(projId2)

        assertEquals(1, proj1Files.size)
        assertEquals(1, proj2Files.size)
        assertEquals("<h1>Project 1</h1>", fileManager.readTextFile(projId1, "index.html"))
        assertEquals("<h1>Project 2</h1>", fileManager.readTextFile(projId2, "index.html"))

        // Duplicate test
        val projId3 = 2003L
        fileManager.duplicateProjectFiles(projId1, projId3)
        assertEquals("<h1>Project 1</h1>", fileManager.readTextFile(projId3, "index.html"))
    }

    /**
     * TEST 6: PWA Builder Workflow & Validation
     */
    @Test
    fun testPwaBuilderWorkflowAndApkGeneration() = runBlocking {
        val pwaProject = Project(
            id = 3001L,
            name = "PWA News",
            packageName = "com.zoneland.pwanews",
            version = "2.1.0",
            versionCode = 21,
            sourceType = SourceType.PWA,
            targetUrl = "https://example.com/pwa",
            htmlContent = "",
            displayMode = DisplayMode.FULLSCREEN,
            orientation = Orientation.PORTRAIT,
            enableJavaScript = true,
            enableLocalStorage = true,
            enableOfflineCaching = true,
            iconColorHex = "#34C759"
        )

        // Save offline fallback page
        fileManager.saveTextFile(pwaProject.id, "index.html", "<h1>Offline PWA</h1>")

        // Build PWA APK
        val result = buildEngine.buildAndSignApk(pwaProject)
        assertTrue("PWA build must succeed: ${result.humanReadableError}", result.isSuccess)
        assertNotNull(result.apkFile)
        assertTrue(result.apkFile!!.name.endsWith(".apk"))

        val validation = apkValidator.validateApk(result.apkFile!!, pwaProject)
        assertTrue(validation.isValid)
        assertEquals("com.zoneland.pwanews", validation.metadata?.packageName)
        assertEquals("2.1.0", validation.metadata?.versionName)
    }

    /**
     * STABILITY & FAILING GRACEFULLY TEST:
     * Invalid inputs, broken zip, path traversal attempts must fail safely without crash.
     */
    @Test
    fun testSecurityAndMalformedInputsFailGracefully() = runBlocking {
        // 1. Invalid Package Name
        val invalidPkgProject = Project(
            id = 4001L,
            name = "Bad Package",
            packageName = "123.class.invalid", // starts with number and contains keyword
            version = "1.0.0",
            sourceType = SourceType.HTML,
            targetUrl = "index.html",
            htmlContent = "<h1>Test</h1>"
        )
        val result1 = buildEngine.buildAndSignApk(invalidPkgProject)
        assertFalse("Must fail on invalid package name", result1.isSuccess)
        assertEquals(BuildStep.VALIDATING, result1.finalStep)
        assertNotNull(result1.humanReadableError)

        // 2. Corrupted ZIP import test
        val corruptedZipBytes = "NOT A ZIP FILE CONTENT".toByteArray()
        val zipResult = fileManager.importZip(4002L, ByteArrayInputStream(corruptedZipBytes))
        assertFalse("Corrupted ZIP must be rejected", zipResult.success)
        assertNotNull(zipResult.errorMessage)

        // 3. Security Validator Path Traversal
        val sanitized = SecurityValidator.sanitizeRelativePath("../../../etc/passwd")
        assertFalse("Sanitized path must not contain traversal", sanitized.contains(".."))
    }
}
