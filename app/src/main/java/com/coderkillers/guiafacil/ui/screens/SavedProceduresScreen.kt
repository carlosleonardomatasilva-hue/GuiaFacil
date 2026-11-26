package com.coderkillers.guiafacil.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.data.model.SavedProcedure
import com.coderkillers.guiafacil.ui.components.GuiaFacilHeader
import com.coderkillers.guiafacil.ui.components.SpeakTextButton
import com.coderkillers.guiafacil.ui.components.rememberTtsManager
import com.coderkillers.guiafacil.ui.theme.*

@Composable
fun SavedProceduresScreen(
    state: GuiaFacilUiState,
    onNavigateBack: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onToggleStepCompleted: (String, Int) -> Unit,
    onDeleteProcedure: (String) -> Unit
) {
    val context = LocalContext.current
    val ttsManager = rememberTtsManager(context)

    val savedTitle = state.dictionary["saved_title"] ?: "Mis Trámites Guardados (Modo Offline)"
    val savedEmpty = state.dictionary["saved_empty"] ?: "No tienes trámites guardados aún."
    val btnDelete = state.dictionary["btn_delete"] ?: "Eliminar"
    val offlineBadge = state.dictionary["offline_badge"] ?: "Disponible sin internet"

    Scaffold(
        topBar = {
            GuiaFacilHeader(
                currentLanguage = state.selectedLanguage,
                subtitle = state.dictionary["app_subtitle"] ?: "Puentes de Oportunidad",
                onLanguagePillClick = onNavigateToLanguageSelection
            )
        },
        containerColor = SoftCreamBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        tint = CardBorderDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = savedTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(offlineBadge, color = HealthGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (state.savedProcedures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = savedEmpty,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                color = SecondaryText
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Guarda tus consultas o lecturas de documentos para revisarlas en cualquier lugar sin gastar datos.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = Color.Gray
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.savedProcedures, key = { it.id }) { procedure ->
                        SavedProcedureCard(
                            procedure = procedure,
                            languageName = state.selectedLanguage,
                            ttsManager = ttsManager,
                            btnDeleteLabel = btnDelete,
                            onToggleStep = { stepId -> onToggleStepCompleted(procedure.id, stepId) },
                            onDelete = { onDeleteProcedure(procedure.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProcedureCard(
    procedure: SavedProcedure,
    languageName: String,
    ttsManager: com.coderkillers.guiafacil.ui.components.AndroidTtsManager,
    btnDeleteLabel: String,
    onToggleStep: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val completedCount = procedure.steps.count { it.isCompleted }
    val totalCount = procedure.steps.size

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorderDark.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title + Progress Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = procedure.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = InstitutionalBlue
                        )
                    )
                }

                Surface(
                    color = if (completedCount == totalCount && totalCount > 0) HealthGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$completedCount/$totalCount listo",
                        color = if (completedCount == totalCount && totalCount > 0) HealthGreen else AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Summary text
            Surface(
                color = SoftCreamBackground,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = procedure.summary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Checklist
            Text(
                text = "📋 Lista de pasos (Presiona para marcar):",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )

            procedure.steps.forEach { step ->
                Surface(
                    onClick = { onToggleStep(step.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (step.isCompleted) HealthGreen.copy(alpha = 0.08f) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (step.isCompleted) HealthGreen else Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = step.isCompleted,
                            onCheckedChange = { onToggleStep(step.id) },
                            colors = CheckboxDefaults.colors(checkedColor = HealthGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = if (step.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (step.isCompleted) Color.Gray else DarkText
                            )
                        )
                    }
                }
            }

            if (procedure.officialInfo.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = InstitutionalBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = procedure.officialInfo,
                        fontSize = 14.sp,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(color = Color.LightGray)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SpeakTextButton(
                        textToSpeak = "${procedure.title}. ${procedure.summary}. Pasos: ${procedure.steps.joinToString(". ") { it.text }}",
                        languageName = languageName,
                        ttsManager = ttsManager,
                        label = "Escuchar"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = btnDeleteLabel,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
