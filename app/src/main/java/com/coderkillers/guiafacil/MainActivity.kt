package com.coderkillers.guiafacil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coderkillers.guiafacil.ui.screens.AssistantScreen
import com.coderkillers.guiafacil.ui.screens.DocumentReaderScreen
import com.coderkillers.guiafacil.ui.screens.HomeScreen
import com.coderkillers.guiafacil.ui.screens.LanguageSelectionScreen
import com.coderkillers.guiafacil.ui.screens.SavedProceduresScreen
import com.coderkillers.guiafacil.ui.theme.GuiaFacilTheme
import com.coderkillers.guiafacil.ui.theme.SoftCreamBackground
import com.coderkillers.guiafacil.ui.viewmodel.GuiaFacilViewModel

object NavRoutes {
    const val LANGUAGE_SELECTION = "language_selection"
    const val HOME = "home"
    const val ASSISTANT = "assistant"
    const val DOCUMENT_READER = "document_reader"
    const val SAVED_PROCEDURES = "saved_procedures"
}

class MainActivity : ComponentActivity() {

    private val viewModel: GuiaFacilViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted callback handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            GuiaFacilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SoftCreamBackground
                ) {
                    GuiaFacilApp(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }
}

@Composable
fun GuiaFacilApp(viewModel: GuiaFacilViewModel) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.LANGUAGE_SELECTION
    ) {
        // Pantalla 1: Selección Universal de Idioma (Onboarding)
        composable(NavRoutes.LANGUAGE_SELECTION) {
            LanguageSelectionScreen(
                state = state,
                onLanguageSelected = { language ->
                    viewModel.selectLanguage(language)
                },
                onContinueToHome = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LANGUAGE_SELECTION) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Pantalla 2: Inicio ("Hola, ¿en qué te ayudo hoy?")
        composable(NavRoutes.HOME) {
            HomeScreen(
                state = state,
                onNavigateToLanguageSelection = {
                    navController.navigate(NavRoutes.LANGUAGE_SELECTION)
                },
                onNavigateToAssistant = {
                    navController.navigate(NavRoutes.ASSISTANT)
                },
                onNavigateToDocumentReader = {
                    navController.navigate(NavRoutes.DOCUMENT_READER)
                },
                onNavigateToSavedProcedures = {
                    navController.navigate(NavRoutes.SAVED_PROCEDURES)
                },
                onToggleHelpDialog = { show ->
                    viewModel.toggleHelpDialog(show)
                }
            )
        }

        // Pantalla 3: Asistente Guiado ("Hablemos con calma")
        composable(NavRoutes.ASSISTANT) {
            AssistantScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLanguageSelection = { navController.navigate(NavRoutes.LANGUAGE_SELECTION) },
                onProfileSelected = { profile -> viewModel.setProfile(profile) },
                onReadingLevelSelected = { level -> viewModel.setReadingLevel(level) },
                onAskQuestion = { question, locCtx, lat, lng -> viewModel.askQuestion(question, locCtx, lat, lng) },
                onAskQuestionFromAudio = { audioBytes, locCtx, lat, lng -> viewModel.askQuestionFromAudio(audioBytes, locCtx, lat, lng) },
                onSaveResponse = { viewModel.saveAssistantResponseAsProcedure() },
                onSpokenTextChange = { text -> viewModel.setSpokenText(text) },
                onClearState = { viewModel.clearAssistantState() }
            )
        }

        // Pantalla 4: Lector Multimodal de Documentos ("Leer un documento")
        composable(NavRoutes.DOCUMENT_READER) {
            DocumentReaderScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLanguageSelection = { navController.navigate(NavRoutes.LANGUAGE_SELECTION) },
                onAnalyzeDocument = { bitmap -> viewModel.analyzeDocument(bitmap) },
                onToggleStepCompleted = { stepId -> viewModel.toggleDocumentStepCompleted(stepId) },
                onSaveDocumentResult = { viewModel.saveDocumentResultAsProcedure() },
                onClearState = { viewModel.clearDocumentState() }
            )
        }

        // Pantalla 5: Mis Trámites Guardados (Modo Offline)
        composable(NavRoutes.SAVED_PROCEDURES) {
            SavedProceduresScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLanguageSelection = { navController.navigate(NavRoutes.LANGUAGE_SELECTION) },
                onToggleStepCompleted = { procedureId, stepId ->
                    viewModel.toggleStepCompleted(procedureId, stepId)
                },
                onDeleteProcedure = { procedureId ->
                    viewModel.deleteProcedure(procedureId)
                }
            )
        }
    }
}
