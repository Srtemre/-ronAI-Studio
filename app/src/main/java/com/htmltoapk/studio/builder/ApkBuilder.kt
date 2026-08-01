package com.htmltoapk.studio.builder

import android.content.Context
import com.htmltoapk.studio.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BuildOutcome {
    data class Success(val apk: File) : BuildOutcome
    data class Unavailable(val reason: String) : BuildOutcome
    data class Failure(val error: String) : BuildOutcome
}

interface ApkBuilder {
    /**
     * Attempts to build an APK for an already-generated project directory.
     * Returns [BuildOutcome.Unavailable] when no Android SDK / Gradle can be located
     * on this device — in which case callers should instruct the user to build
     * the project on a desktop machine.
     */
    suspend fun build(generatedProjectDir: File, signingConfig: com.htmltoapk.studio.data.model.SigningConfig?): BuildOutcome
}

@Singleton
class ApkBuilderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) : ApkBuilder {

    override suspend fun build(
        generatedProjectDir: File,
        signingConfig: com.htmltoapk.studio.data.model.SigningConfig?
    ): BuildOutcome = withContext(io) {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        val gradlew = File(generatedProjectDir, "gradlew")
        val gradlewBat = File(generatedProjectDir, "gradlew.bat")
        val wrapperJar = File(generatedProjectDir, "gradle/wrapper/gradle-wrapper.jar")

        val hasGradlew = (gradlew.exists() && gradlew.canExecute()) || gradlewBat.exists()
        if (androidHome.isNullOrBlank() || !hasGradlew || !wrapperJar.exists()) {
            return@withContext BuildOutcome.Unavailable(
                "Android SDK or Gradle wrapper not detected on this device. " +
                    "Open the generated project on a desktop with Android Studio to build the APK."
            )
        }

        File(generatedProjectDir, "local.properties").writeText("sdk.dir=$androidHome\n")

        val cmd = if (System.getProperty("os.name").startsWith("Windows")) {
            listOf("cmd", "/c", "gradlew.bat", "assembleRelease")
        } else {
            listOf(gradlew.absolutePath, "assembleRelease")
        }

        val pb = ProcessBuilder(cmd).directory(generatedProjectDir).redirectErrorStream(true)
        if (signingConfig != null && signingConfig.keystoreUri.isNotBlank()) {
            pb.environment()["KEYSTORE_FILE"] = signingConfig.keystoreUri
            pb.environment()["KEYSTORE_PASSWORD"] = signingConfig.keystorePassword
            pb.environment()["KEY_ALIAS"] = signingConfig.keyAlias
            pb.environment()["KEY_PASSWORD"] = signingConfig.keyPassword
        }

        val process = try { pb.start() } catch (t: Throwable) {
            return@withContext BuildOutcome.Failure("Cannot start gradlew: ${t.message}")
        }
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) {
            return@withContext BuildOutcome.Failure("Gradle exited with $exit.\n$output")
        }
        val apk = File(generatedProjectDir, "app/build/outputs/apk/release/app-release.apk")
            .takeIf { it.exists() }
            ?: File(generatedProjectDir, "app/build/outputs/apk/debug/app-debug.apk").takeIf { it.exists() }
            ?: return@withContext BuildOutcome.Failure("Build completed but APK not found.\n$output")
        BuildOutcome.Success(apk)
    }
}
