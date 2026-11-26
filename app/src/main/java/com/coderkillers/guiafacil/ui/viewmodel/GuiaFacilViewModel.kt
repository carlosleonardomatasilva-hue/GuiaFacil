package com.coderkillers.guiafacil.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coderkillers.guiafacil.data.local.LocalStorageManager
import com.coderkillers.guiafacil.data.model.CheckableStep
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.data.model.SavedProcedure
import com.coderkillers.guiafacil.data.remote.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuiaFacilViewModel(application: Application) : AndroidViewModel(application) {

    private val localStorageManager = LocalStorageManager(application.applicationContext)
    private val geminiRepository = GeminiRepository()

    private val _uiState = MutableStateFlow(GuiaFacilUiState())
    val uiState: StateFlow<GuiaFacilUiState> = _uiState.asStateFlow()

    init {
        val savedLang = localStorageManager.getSelectedLanguage()
        val savedDict = localStorageManager.getDictionary()
        val savedList = localStorageManager.getSavedProcedures()

        _uiState.update { state ->
            state.copy(
                selectedLanguage = savedLang,
                dictionary = savedDict ?: state.dictionary,
                savedProcedures = savedList
            )
        }

        // Refresh UI dictionary if custom language was set
        if (savedLang != "Español" && savedDict == null) {
            selectLanguage(savedLang)
        }
    }

    fun selectLanguage(language: String) {
        val cleanLang = language.trim()
        if (cleanLang.isBlank()) return

        localStorageManager.saveSelectedLanguage(cleanLang)
        _uiState.update { it.copy(selectedLanguage = cleanLang, isTranslatingUi = true) }

        viewModelScope.launch {
            val translatedDict = geminiRepository.translateUiDictionary(cleanLang)
            localStorageManager.saveDictionary(translatedDict)
            _uiState.update {
                it.copy(
                    dictionary = translatedDict,
                    isTranslatingUi = false
                )
            }
        }
    }

    fun setProfile(profile: String) {
        _uiState.update { it.copy(currentProfile = profile) }
    }

    fun setReadingLevel(level: String) {
        _uiState.update { it.copy(readingLevel = level) }
    }

    fun askQuestion(
        question: String,
        locationContext: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        if (question.isBlank()) return

        _uiState.update { it.copy(isAssistantLoading = true, spokenText = question) }

        viewModelScope.launch {
            val response = geminiRepository.generateAssistantResponse(
                question = question,
                profile = _uiState.value.currentProfile,
                readingLevel = _uiState.value.readingLevel,
                targetLanguage = _uiState.value.selectedLanguage,
                locationContext = locationContext,
                lat = lat,
                lng = lng
            )

            _uiState.update {
                it.copy(
                    assistantResponse = response,
                    isAssistantLoading = false
                )
            }
        }
    }

    fun askQuestionFromAudio(
        audioBytes: ByteArray,
        locationContext: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        _uiState.update { it.copy(isAssistantLoading = true, spokenText = "Transcribiendo audio...") }

        viewModelScope.launch {
            val response = geminiRepository.generateAssistantResponseFromAudio(
                audioBytes = audioBytes,
                profile = _uiState.value.currentProfile,
                readingLevel = _uiState.value.readingLevel,
                targetLanguage = _uiState.value.selectedLanguage,
                locationContext = locationContext,
                lat = lat,
                lng = lng
            )

            _uiState.update {
                it.copy(
                    assistantResponse = response,
                    spokenText = response.question, // The transcription
                    isAssistantLoading = false
                )
            }
        }
    }

    fun analyzeDocument(bitmap: Bitmap) {
        _uiState.update { it.copy(isDocumentLoading = true) }

        viewModelScope.launch {
            val result = geminiRepository.analyzeDocumentImage(
                bitmap = bitmap,
                targetLanguage = _uiState.value.selectedLanguage
            )

            _uiState.update {
                it.copy(
                    documentResult = result,
                    isDocumentLoading = false
                )
            }
        }
    }

    fun clearAssistantState() {
        _uiState.update {
            it.copy(
                spokenText = "",
                assistantResponse = null,
                isAssistantLoading = false
            )
        }
    }

    fun clearDocumentState() {
        _uiState.update {
            it.copy(
                documentResult = null,
                isDocumentLoading = false
            )
        }
    }

    fun saveAssistantResponseAsProcedure() {
        val response = _uiState.value.assistantResponse ?: return
        val steps = response.steps.mapIndexed { idx, text ->
            CheckableStep(id = idx + 1, text = text, isCompleted = false)
        }

        val procedure = SavedProcedure(
            id = System.currentTimeMillis().toString(),
            title = response.question,
            summary = response.summaryText,
            steps = steps,
            officialInfo = response.officialInfo
        )

        localStorageManager.saveProcedure(procedure)

        _uiState.update {
            it.copy(
                savedProcedures = localStorageManager.getSavedProcedures(),
                assistantResponse = response.copy(isSaved = true)
            )
        }
    }

    fun saveDocumentResultAsProcedure() {
        val docResult = _uiState.value.documentResult ?: return

        val procedure = SavedProcedure(
            id = System.currentTimeMillis().toString(),
            title = "Documento Analizado (${_uiState.value.selectedLanguage})",
            summary = docResult.summary,
            steps = docResult.steps,
            officialInfo = "Analizado con GuíaFácil Lector Multimodal"
        )

        localStorageManager.saveProcedure(procedure)

        _uiState.update {
            it.copy(
                savedProcedures = localStorageManager.getSavedProcedures(),
                documentResult = docResult.copy(isSaved = true)
            )
        }
    }

    fun toggleStepCompleted(procedureId: String, stepId: Int) {
        val procedure = _uiState.value.savedProcedures.find { it.id == procedureId } ?: return
        val updatedSteps = procedure.steps.map { step ->
            if (step.id == stepId) step.copy(isCompleted = !step.isCompleted) else step
        }
        val updatedProcedure = procedure.copy(steps = updatedSteps)

        localStorageManager.updateProcedure(updatedProcedure)
        _uiState.update {
            it.copy(savedProcedures = localStorageManager.getSavedProcedures())
        }
    }

    fun toggleDocumentStepCompleted(stepId: Int) {
        val docResult = _uiState.value.documentResult ?: return
        val updatedSteps = docResult.steps.map { step ->
            if (step.id == stepId) step.copy(isCompleted = !step.isCompleted) else step
        }
        _uiState.update {
            it.copy(documentResult = docResult.copy(steps = updatedSteps))
        }
    }

    fun deleteProcedure(id: String) {
        localStorageManager.deleteProcedure(id)
        _uiState.update {
            it.copy(savedProcedures = localStorageManager.getSavedProcedures())
        }
    }

    fun setSpokenText(text: String) {
        _uiState.update { it.copy(spokenText = text) }
    }

    fun setListeningVoice(isListening: Boolean) {
        _uiState.update { it.copy(isListeningVoice = isListening) }
    }

    fun toggleHelpDialog(show: Boolean) {
        _uiState.update { it.copy(showHelpDialog = show) }
    }
}
