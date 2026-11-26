package com.coderkillers.guiafacil.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.ui.components.GuiaFacilHeader
import com.coderkillers.guiafacil.ui.components.SpeakTextButton
import com.coderkillers.guiafacil.ui.components.rememberTtsManager
import com.coderkillers.guiafacil.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AssistantScreen(
    state: GuiaFacilUiState,
    onNavigateBack: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onReadingLevelSelected: (String) -> Unit,
    onAskQuestion: (String, String?, Double?, Double?) -> Unit,
    onAskQuestionFromAudio: (ByteArray, String?, Double?, Double?) -> Unit,
    onSaveResponse: () -> Unit,
    onSpokenTextChange: (String) -> Unit,
    onClearState: () -> Unit
) {
    val context = LocalContext.current
    val ttsManager = rememberTtsManager(context)
    var inputText by remember { mutableStateOf(state.spokenText) }
    var showOfficialDialog by remember { mutableStateOf(false) }
    var pendingQuestion by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    val audioFile = remember { java.io.File(context.cacheDir, "voice_query.m4a") }
    var pendingAudioBytes by remember { mutableStateOf<ByteArray?>(null) }

    androidx.activity.compose.BackHandler {
        try { mediaRecorder?.release(); mediaRecorder = null } catch(e: Exception){}
        ttsManager.stop()
        onClearState()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            try { mediaRecorder?.release(); mediaRecorder = null } catch(e: Exception){}
            ttsManager.stop()
            onClearState()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val query = pendingQuestion
        val audioBytes = pendingAudioBytes
        
        if (!query.isNullOrBlank()) {
            pendingQuestion = null
            if (granted) {
                fetchGpsLocationAndAsk(context, query, onAskQuestion)
            } else {
                onAskQuestion(query, null, null, null)
            }
        } else if (audioBytes != null) {
            pendingAudioBytes = null
            if (granted) {
                fetchGpsLocationAndAskFromAudio(context, audioBytes, onAskQuestionFromAudio)
            } else {
                onAskQuestionFromAudio(audioBytes, null, null, null)
            }
        }
    }

    val requestLocationAndExecute: (String) -> Unit = { query ->
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchGpsLocationAndAsk(context, query, onAskQuestion)
        } else {
            pendingQuestion = query
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val requestLocationAndExecuteAudio: (ByteArray) -> Unit = { audioBytes ->
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchGpsLocationAndAskFromAudio(context, audioBytes, onAskQuestionFromAudio)
        } else {
            pendingAudioBytes = audioBytes
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    android.media.MediaRecorder()
                }.apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                isRecording = true
                
                // Stop automatically after 15 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isRecording) {
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecording = false
                            if (audioFile.exists()) {
                                requestLocationAndExecuteAudio(audioFile.readBytes())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, 15000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val profiles = listOf(
        state.dictionary["profile_elderly"] ?: "Adulto mayor",
        state.dictionary["profile_student"] ?: "Estudiante 1ra generación",
        state.dictionary["profile_migrant"] ?: "Migrante",
        state.dictionary["profile_low_conn"] ?: "Baja conectividad"
    )

    val readingLevels = listOf(
        state.dictionary["level_simple"] ?: "Muy sencillo",
        state.dictionary["level_standard"] ?: "Estándar"
    )

    val suggestedQuestions = listOf(
        state.dictionary["q_pension"] ?: "Pensión Bienestar para Adultos Mayores",
        state.dictionary["q_scholarship"] ?: "Becas Universitarias de Primera Generación",
        state.dictionary["q_health"] ?: "Atención médica gratuita sin seguro social",
        state.dictionary["q_migrant"] ?: "Trámites de regulación y empleo para migrantes"
    )

    val assistantTitle = state.dictionary["assistant_title"] ?: "Hablemos con calma"
    val profileLabel = state.dictionary["profile_label"] ?: "Perfil del usuario:"
    val readingLevelLabel = state.dictionary["reading_level_label"] ?: "Nivel de lectura:"
    val suggestedTitle = state.dictionary["suggested_questions_title"] ?: "Preguntas rápidas sugeridas:"
    val inputPlaceholder = state.dictionary["input_placeholder"] ?: "Escribe o presiona el micrófono para hablar..."
    val btnSave = state.dictionary["btn_save"] ?: "Guardar lista"
    val btnOfficialLink = state.dictionary["btn_official_link"] ?: "Ver módulo o enlace oficial"
    val btnConsult = state.dictionary["btn_consult"] ?: "Consultar"
    val voicePrompt = state.dictionary["voice_prompt"] ?: "Habla tu pregunta para GuíaFácil..."

    Scaffold(
        topBar = {
            GuiaFacilHeader(
                currentLanguage = state.selectedLanguage,
                subtitle = state.dictionary["app_subtitle"] ?: "Puentes de Oportunidad",
                onLanguagePillClick = onNavigateToLanguageSelection
            )
        },
        containerColor = SoftCreamBackground,
        contentWindowInsets = WindowInsets.ime
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Top Bar Navigation Back + Screen Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = InstitutionalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = assistantTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = InstitutionalBlue
                    )
                )
            }

            // Profile Selector Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = profileLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles) { profile ->
                            val isSelected = state.currentProfile == profile
                            FilterChip(
                                selected = isSelected,
                                onClick = { onProfileSelected(profile) },
                                label = { Text(profile, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InstitutionalBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = ChipSelectedBg
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = readingLevelLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        readingLevels.forEach { level ->
                            val isSelected = state.readingLevel == level
                            FilterChip(
                                selected = isSelected,
                                onClick = { onReadingLevelSelected(level) },
                                label = { Text(level, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InstitutionalBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = ChipSelectedBg
                                )
                            )
                        }
                    }
                }
            }

            // Suggested Questions Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = suggestedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    )
                    suggestedQuestions.forEach { questionText ->
                        OutlinedButton(
                            onClick = {
                                requestLocationAndExecute(questionText)
                                inputText = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InstitutionalBlue),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, InstitutionalBlue.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "💡 $questionText",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            // Speech Input Box + Microphone Button
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            onSpokenTextChange(it)
                        },
                        placeholder = { Text(inputPlaceholder, fontSize = 16.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    val query = inputText.trim()
                                    requestLocationAndExecute(query)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice Input Button
                        if (isRecording) {
                            Button(
                                onClick = {
                                    try {
                                        mediaRecorder?.stop()
                                        mediaRecorder?.release()
                                        mediaRecorder = null
                                        isRecording = false
                                        if (audioFile.exists()) {
                                            requestLocationAndExecuteAudio(audioFile.readBytes())
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.height(56.dp).weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = "Detener grabación", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escuchando... (toca para enviar)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasMicPermission) {
                                        try {
                                            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                android.media.MediaRecorder(context)
                                            } else {
                                                @Suppress("DEPRECATION")
                                                android.media.MediaRecorder()
                                            }.apply {
                                                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(audioFile.absolutePath)
                                                prepare()
                                                start()
                                            }
                                            isRecording = true
                                            
                                            // Stop automatically after 15 seconds
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                if (isRecording) {
                                                    try {
                                                        mediaRecorder?.stop()
                                                        mediaRecorder?.release()
                                                        mediaRecorder = null
                                                        isRecording = false
                                                        if (audioFile.exists()) {
                                                            requestLocationAndExecuteAudio(audioFile.readBytes())
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }, 15000)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(AccentOrange)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Hablar por micrófono",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Send Text Button
                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val query = inputText.trim()
                                    requestLocationAndExecute(query)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !state.isAssistantLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = InstitutionalBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .padding(start = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(btnConsult, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Loading state
            if (state.isAssistantLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = InstitutionalBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Preparando respuesta clara y paso a paso...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Response Box & Actions
            state.assistantResponse?.let { response ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, InstitutionalBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "💡 ${state.dictionary["summary_title"] ?: "Resumen sencillo:"}",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, color = InstitutionalBlue)
                        )

                        Surface(
                            color = SoftCreamBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = response.summaryText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Text(
                            text = "📋 Pasos a seguir:",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )

                        response.steps.forEach { stepText ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stepText,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 25.sp),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Divider(color = Color.LightGray)

                        // 3 Required Action Buttons below response
                        // Action 1: "Escuchar respuesta"
                        SpeakTextButton(
                            textToSpeak = "${response.summaryText}. ${response.steps.joinToString(". ")}",
                            languageName = state.selectedLanguage,
                            ttsManager = ttsManager,
                            label = state.dictionary["btn_listen"] ?: "Escuchar respuesta"
                        )

                        // Action 2: "Guardar lista"
                        Button(
                            onClick = onSaveResponse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (response.isSaved) HealthGreen else AccentOrange
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(
                                imageVector = if (response.isSaved) Icons.Default.Check else Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (response.isSaved) "✓ Lista guardada para uso sin internet" else btnSave,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Action 3: "Ver módulo o enlace oficial"
                        OutlinedButton(
                            onClick = { showOfficialDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkText)
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = InstitutionalBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(btnOfficialLink, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Official Info Modal Dialog with Google Maps Direct Intent
        if (showOfficialDialog && state.assistantResponse != null) {
            val response = state.assistantResponse!!
            AlertDialog(
                onDismissRequest = { showOfficialDialog = false },
                icon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = InstitutionalBlue, modifier = Modifier.size(36.dp)) },
                title = { Text(btnOfficialLink, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (response.nombreModulo.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = InstitutionalBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = response.nombreModulo,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            }
                        }

                        if (response.horarioAtencion.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = AccentOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = response.horarioAtencion,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText
                                )
                            }
                        }

                        Surface(
                            color = SoftCreamBackground,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = response.officialInfo,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (response.busquedaMaps.isNotBlank()) {
                            Button(
                                onClick = {
                                    val busqueda = response.busquedaMaps
                                    val uri = if (response.lat != null && response.lng != null) {
                                        Uri.parse("geo:${response.lat},${response.lng}?q=${Uri.encode(busqueda)}")
                                    } else {
                                        Uri.parse("geo:0,0?q=${Uri.encode(busqueda)}")
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(busqueda)}")
                                        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                        context.startActivity(browserIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir ubicación en Google Maps", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (response.enlaceOficial.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(response.enlaceOficial))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = InstitutionalBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Visitar sitio web oficial", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showOfficialDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = InstitutionalBlue)
                    ) {
                        Text(state.dictionary["btn_close"] ?: "Cerrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchGpsLocationAndAsk(
    context: Context,
    query: String,
    onAskQuestion: (String, String?, Double?, Double?) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val loc = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (loc != null) {
            var locality = ""
            var stateName = ""
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    locality = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    stateName = addresses[0].adminArea ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val locStr = "Ubicación exacta por GPS: ${if (locality.isNotBlank()) "$locality, " else ""}$stateName (coordenadas: ${loc.latitude}, ${loc.longitude})"
            onAskQuestion(query, locStr, loc.latitude, loc.longitude)
        } else {
            onAskQuestion(query, null, null, null)
        }
    } catch (e: Exception) {
        onAskQuestion(query, null, null, null)
    }
}

@SuppressLint("MissingPermission")
private fun fetchGpsLocationAndAskFromAudio(
    context: Context,
    audioBytes: ByteArray,
    onAskQuestionFromAudio: (ByteArray, String?, Double?, Double?) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val loc = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

        if (loc != null) {
            var locality = ""
            var stateName = ""
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    locality = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    stateName = addresses[0].adminArea ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val locStr = "Ubicación exacta por GPS: ${if (locality.isNotBlank()) "$locality, " else ""}$stateName (coordenadas: ${loc.latitude}, ${loc.longitude})"
            onAskQuestionFromAudio(audioBytes, locStr, loc.latitude, loc.longitude)
        } else {
            onAskQuestionFromAudio(audioBytes, null, null, null)
        }
    } catch (e: Exception) {
        onAskQuestionFromAudio(audioBytes, null, null, null)
    }
}
