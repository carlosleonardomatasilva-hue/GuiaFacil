package com.coderkillers.guiafacil.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.ui.components.GuiaFacilHeader
import com.coderkillers.guiafacil.ui.components.PrimaryAccessibleButton
import com.coderkillers.guiafacil.ui.components.SpeakTextButton
import com.coderkillers.guiafacil.ui.components.rememberTtsManager
import com.coderkillers.guiafacil.ui.theme.*

@Composable
fun DocumentReaderScreen(
    state: GuiaFacilUiState,
    onNavigateBack: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onAnalyzeDocument: (Bitmap) -> Unit,
    onToggleStepCompleted: (Int) -> Unit,
    onSaveDocumentResult: () -> Unit,
    onClearState: () -> Unit
) {
    val context = LocalContext.current
    val ttsManager = rememberTtsManager(context)
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    androidx.activity.compose.BackHandler {
        ttsManager.stop()
        onClearState()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.stop()
            onClearState()
        }
    }

    // Camera Preview Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            currentBitmap = bitmap
            onAnalyzeDocument(bitmap)
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val rawBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val bitmap = fixExifOrientation(context, uri, rawBitmap)
                currentBitmap = bitmap
                onAnalyzeDocument(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val readerTitle = state.dictionary["reader_title"] ?: "Leer un documento"
    val readerDesc = state.dictionary["reader_desc"] ?: "Toma o sube una foto de tu receta médica, citatorio o carta oficial para recibir una explicación clara sin palabras complicadas."
    val btnTakePhoto = state.dictionary["btn_take_photo"] ?: "Tomar Foto y Explicar"
    val btnChooseGallery = state.dictionary["btn_choose_gallery"] ?: "Elegir foto de la galería"
    val btnDemo = state.dictionary["btn_demo_prescription"] ?: "Probar con receta de ejemplo"
    val summaryTitle = state.dictionary["summary_title"] ?: "Resumen sencillo:"
    val stepsTitle = state.dictionary["steps_title"] ?: "Mis siguientes pasos:"
    val docExplanationHeader = state.dictionary["doc_explanation_header"] ?: "Explicación de tu documento"
    val btnListenVoice = state.dictionary["btn_listen_voice"] ?: "Escuchar en voz alta"
    val btnSaveOffline = state.dictionary["btn_save_offline"] ?: "Guardar para ver sin internet"
    val completedCounter = state.dictionary["completed_counter"] ?: "completados"
    val savedOfflineSuccess = state.dictionary["saved_offline_success"] ?: "Guardado para consultar sin internet"
    val loadingVisionText = state.dictionary["loading_document_vision"] ?: "Leyendo documento con visión inteligente..."

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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Bar Navigation Back + Title
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
                        tint = HealthGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = readerTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthGreen
                    )
                )
            }

            // Description Header
            Text(
                text = readerDesc,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = DarkText)
            )

            // Action Button 1: Tomar Foto con Cámara
            PrimaryAccessibleButton(
                text = btnTakePhoto,
                onClick = { cameraLauncher.launch(null) },
                containerColor = HealthGreen,
                icon = { Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            )

            // Action Button 2: Elegir de la Galería
            PrimaryAccessibleButton(
                text = btnChooseGallery,
                onClick = { galleryLauncher.launch("image/*") },
                containerColor = InstitutionalBlue,
                icon = { Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            )

            // Action Button 3: Probar con Receta de Ejemplo
            OutlinedButton(
                onClick = {
                    val demoBitmap = createDemoPrescriptionBitmap()
                    currentBitmap = demoBitmap
                    onAnalyzeDocument(demoBitmap)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                border = androidx.compose.foundation.BorderStroke(2.dp, AccentOrange)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(btnDemo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }

            // Preview Selected Image
            currentBitmap?.let { bitmap ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Documento a analizar",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            }

            // Loading state
            if (state.isDocumentLoading) {
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
                        CircularProgressIndicator(color = HealthGreen)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(loadingVisionText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Analysis Result Section
            state.documentResult?.let { result ->
                val completedCount = result.steps.count { it.isCompleted }
                val totalCount = result.steps.size

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, HealthGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📄 $docExplanationHeader",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, color = HealthGreen)
                        )

                        Text(
                            text = summaryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        )

                        Surface(
                            color = SoftCreamBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.summary,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        // Button 1: "Escuchar en voz alta"
                        SpeakTextButton(
                            textToSpeak = "${result.summary}. $stepsTitle: ${result.steps.joinToString(". ") { it.text }}",
                            languageName = state.selectedLanguage,
                            ttsManager = ttsManager,
                            label = btnListenVoice
                        )

                        Divider(color = Color.LightGray)

                        // Interactive Checklist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stepsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                color = HealthGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$completedCount de $totalCount $completedCounter",
                                    color = HealthGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        result.steps.forEach { step ->
                            Surface(
                                onClick = { onToggleStepCompleted(step.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (step.isCompleted) HealthGreen.copy(alpha = 0.1f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (step.isCompleted) HealthGreen else Color.LightGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = step.isCompleted,
                                        onCheckedChange = { onToggleStepCompleted(step.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = HealthGreen)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = step.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 18.sp,
                                            fontWeight = if (step.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                            color = if (step.isCompleted) Color.Gray else DarkText
                                        )
                                    )
                                }
                            }
                        }

                        // Button 2: "Guardar para ver sin internet"
                        Button(
                            onClick = onSaveDocumentResult,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (result.isSaved) HealthGreen else AccentOrange
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                        ) {
                            Icon(
                                imageVector = if (result.isSaved) Icons.Default.Check else Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (result.isSaved) "✓ $savedOfflineSuccess" else btnSaveOffline,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Utility to generate a synthetic Prescription Demo Bitmap for testing without physical camera
private fun createDemoPrescriptionBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, 600f, 400f, paint)

    paint.apply {
        color = android.graphics.Color.parseColor("#0D3B8E")
        textSize = 32f
        isFakeBoldText = true
    }
    canvas.drawText("RECETA MÉDICA INSTITUCIONAL", 40f, 60f, paint)

    paint.apply {
        color = android.graphics.Color.DKGRAY
        textSize = 24f
        isFakeBoldText = false
    }
    canvas.drawText("Paciente: María Elena Rodríguez (68 años)", 40f, 110f, paint)
    canvas.drawText("Indicación: Amoxicilina 500mg", 40f, 170f, paint)
    canvas.drawText("• Tomar 1 cápsula cada 8 horas con alimentos.", 40f, 220f, paint)
    canvas.drawText("• Duración total: 7 días completos.", 40f, 270f, paint)
    canvas.drawText("• Presentar carnet en Farmacia IMSS-Bienestar.", 40f, 320f, paint)

    return bitmap
}

private fun fixExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = android.media.ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = android.graphics.Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        inputStream.close()
        rotated
    } catch (e: Exception) {
        bitmap
    }
}
