package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.Orientation
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.ui.screens.create.CreateAppScreen
import com.example.ui.screens.create.CreateAppViewModel
import com.example.ui.screens.create.CreateModeSelectionScreen
import com.example.ui.screens.create.FastCreateViewModel
import com.example.ui.screens.create.FastSourceType
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.util.ProjectFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AppCreationFlowRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var fileManager: ProjectFileManager
    private lateinit var projectRepository: ProjectRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        fileManager = ProjectFileManager(context)
        projectRepository = ProjectRepository(database.projectDao(), fileManager)
    }

    @Test
    fun testHomeScreenOnlyHasOneCreateAppButton() {
        val homeViewModel = HomeViewModel(projectRepository)
        var createClicked = false

        composeTestRule.setContent {
            HomeScreen(
                viewModel = homeViewModel,
                language = AppLanguage.ENGLISH,
                onCreateAppClick = { createClicked = true },
                onViewProjectsClick = {},
                onProjectClick = {}
            )
        }

        // Must display single primary create button
        composeTestRule.onNodeWithTag("btn_create_app").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_create_app").performClick()
        assertTrue(createClicked)

        // PWA builder button must NOT exist
        composeTestRule.onNodeWithTag("btn_pwa_builder").assertDoesNotExist()
    }

    @Test
    fun testCreateModeSelectionScreenDisplaysFastAndExpertOptions() {
        var selectedFast = false
        var selectedExpert = false

        composeTestRule.setContent {
            CreateModeSelectionScreen(
                language = AppLanguage.ENGLISH,
                onBackClick = {},
                onSelectFastMode = { selectedFast = true },
                onSelectExpertMode = { selectedExpert = true }
            )
        }

        composeTestRule.onNodeWithTag("create_mode_selection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_mode_fast").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_mode_expert").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_mode_fast").performClick()
        assertTrue(selectedFast)

        composeTestRule.onNodeWithTag("btn_mode_expert").performClick()
        assertTrue(selectedExpert)
    }

    @Test
    fun testFastCreateViewModelState() {
        val viewModel = FastCreateViewModel(projectRepository, fileManager)

        viewModel.updateName("My Quick App")
        viewModel.updateOrientation(Orientation.LANDSCAPE)
        viewModel.updateFastSourceType(FastSourceType.HTML)
        viewModel.updateHtmlCode("<h1>Fast Quick Test</h1>")

        assertEquals("My Quick App", viewModel.uiState.value.name)
        assertEquals(Orientation.LANDSCAPE, viewModel.uiState.value.orientation)
        assertEquals(FastSourceType.HTML, viewModel.uiState.value.fastSourceType)
        assertEquals("<h1>Fast Quick Test</h1>", viewModel.uiState.value.htmlCode)
    }

    @Test
    fun testProjectIconManagement() {
        val projectId = 9999L
        val iconBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // PNG magic header

        assertFalse(fileManager.hasProjectIcon(projectId))
        assertNull(fileManager.getProjectIconFile(projectId))

        // Save project icon
        fileManager.saveProjectIconBytes(projectId, iconBytes, "icon.png")
        assertTrue(fileManager.hasProjectIcon(projectId))
        val savedIcon = fileManager.getProjectIconFile(projectId)
        assertNotNull(savedIcon)
        assertTrue(savedIcon!!.exists())
        assertEquals(iconBytes.size.toLong(), savedIcon.length())

        // Save temp icon and copy to project
        val tempIcon = fileManager.saveTempIconBytes(iconBytes)
        assertTrue(tempIcon.exists())

        val copiedTarget = fileManager.copyTempIconToProject(tempIcon, projectId)
        assertNotNull(copiedTarget)
        assertTrue(copiedTarget!!.exists())

        // Delete icon
        val deleted = fileManager.deleteProjectIcon(projectId)
        assertTrue(deleted)
        assertFalse(fileManager.hasProjectIcon(projectId))
    }

    @Test
    fun testExpertCreateAppViewModelState() {
        val viewModel = CreateAppViewModel(projectRepository, fileManager)

        viewModel.updateName("Expert Tool")
        viewModel.updatePackageName("com.expert.tool")
        viewModel.updateVersion("2.5.0")
        viewModel.updateVersionCode(15)

        assertEquals("Expert Tool", viewModel.uiState.value.name)
        assertEquals("com.expert.tool", viewModel.uiState.value.packageName)
        assertEquals("2.5.0", viewModel.uiState.value.version)
        assertEquals(15, viewModel.uiState.value.versionCode)
    }
}
