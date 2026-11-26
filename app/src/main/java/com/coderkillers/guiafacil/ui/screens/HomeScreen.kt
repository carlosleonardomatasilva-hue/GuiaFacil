package com.coderkillers.guiafacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.ui.components.GuiaFacilHeader
import com.coderkillers.guiafacil.ui.components.HelpSupportDialog
import com.coderkillers.guiafacil.ui.theme.*

@Composable
fun HomeScreen(
    state: GuiaFacilUiState,
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToDocumentReader: () -> Unit,
    onNavigateToSavedProcedures: () -> Unit,
    onToggleHelpDialog: (Boolean) -> Unit
) {
    val homeHeader = state.dictionary["home_header"] ?: "Hola, ¿en qué te ayudo hoy?"
    val cardAssistantTitle = state.dictionary["card_assistant_title"] ?: "Hablar con GuíaFácil"
    val cardAssistantDesc = state.dictionary["card_assistant_desc"] ?: "Pregunta por voz o texto sobre pensiones, becas o trámites"
    val cardReaderTitle = state.dictionary["card_reader_title"] ?: "Leer un Documento"
    val cardReaderDesc = state.dictionary["card_reader_desc"] ?: "Toma foto a recetas médicas, cartas de becas o citatorios"
    val cardSavedTitle = state.dictionary["card_saved_title"] ?: "Mis Trámites Guardados"
    val cardSavedDesc = state.dictionary["card_saved_desc"] ?: "Consulta guías y listas de verificación sin internet"
    val footerText = state.dictionary["footer_text"] ?: "A tu ritmo. Siempre contigo."
    val btnNeedHelp = state.dictionary["btn_need_help"] ?: "¿Necesitas ayuda?"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Welcome Text
            Text(
                text = homeHeader,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InstitutionalBlue
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Card 1: Hablar con GuíaFácil (Azul #0D3B8E)
            LargeActionCard(
                title = cardAssistantTitle,
                description = cardAssistantDesc,
                containerColor = InstitutionalBlue,
                contentColor = Color.White,
                icon = Icons.Default.RecordVoiceOver,
                onClick = onNavigateToAssistant
            )

            // Card 2: Leer un Documento (Verde Salud #14532D)
            LargeActionCard(
                title = cardReaderTitle,
                description = cardReaderDesc,
                containerColor = HealthGreen,
                contentColor = Color.White,
                icon = Icons.Default.DocumentScanner,
                onClick = onNavigateToDocumentReader
            )

            // Card 3: Mis Trámites Guardados (Borde Oscuro)
            Surface(
                onClick = onNavigateToSavedProcedures,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 135.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(2.5.dp, CardBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = CardBorderDark,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cardSavedTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = HealthGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = HealthGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = offlineBadge,
                                    color = HealthGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    softWrap = true,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = cardSavedDesc,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            color = SecondaryText
                        )
                    )

                    if (state.savedProcedures.isNotEmpty()) {
                        Text(
                            text = "📌 Tienes ${state.savedProcedures.size} guía(s) guardada(s)",
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText
                    ),
                    textAlign = TextAlign.Center
                )

                OutlinedButton(
                    onClick = { onToggleHelpDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentOrange
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, AccentOrange)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = btnNeedHelp,
                            tint = AccentOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = btnNeedHelp,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Help Support Modal Dialog
        if (state.showHelpDialog) {
            HelpSupportDialog(
                title = state.dictionary["help_dialog_title"] ?: "Guía Rápida de Uso y Apoyo",
                content = state.dictionary["help_dialog_content"] ?: "GuíaFácil te acompaña en tus trámites:\n\n1. Habla o escribe tus dudas en tu idioma preferido.\n2. Fotografía tus documentos para entenderlos sin enredos.\n3. Guarda tus pasos para consultarlos aun si no tienes internet.\n\n📞 Línea de atención directa: 800-00-GUIA (800-00-4842)\nAtención gratuita y empática.",
                dismissLabel = state.dictionary["btn_close"] ?: "Cerrar",
                onDismiss = { onToggleHelpDialog(false) }
            )
        }
    }
}

@Composable
private fun LargeActionCard(
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
