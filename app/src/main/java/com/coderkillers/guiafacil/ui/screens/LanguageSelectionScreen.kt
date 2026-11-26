package com.coderkillers.guiafacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coderkillers.guiafacil.data.model.GuiaFacilUiState
import com.coderkillers.guiafacil.ui.components.PrimaryAccessibleButton
import com.coderkillers.guiafacil.ui.theme.*

val defaultLanguagesList = listOf(
    "Español",
    "Chontal (Yokot'an)",
    "Tzotzil",
    "Maya",
    "Náhuatl",
    "Zoque",
    "Kreyòl Ayisyen (Criollo Haitiano)",
    "English"
)

@Composable
fun LanguageSelectionScreen(
    state: GuiaFacilUiState,
    onLanguageSelected: (String) -> Unit,
    onContinueToHome: () -> Unit
) {
    var customLanguageText by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(state.selectedLanguage) }
    var shouldNavigate by remember { mutableStateOf(false) }

    LaunchedEffect(state.isTranslatingUi, shouldNavigate) {
        if (!state.isTranslatingUi && shouldNavigate) {
            shouldNavigate = false
            onContinueToHome()
        }
    }

    val welcomeTitle = state.dictionary["welcome_title"] ?: "Bienvenido a GuíaFácil — Elige tu idioma o lengua para comenzar"
    val customPlaceholder = state.dictionary["custom_lang_placeholder"] ?: "¿No encuentras tu idioma? Escríbelo aquí"
    val btnContinue = state.dictionary["btn_continue"] ?: "Continuar"

    Scaffold(
        containerColor = SoftCreamBackground,
        contentWindowInsets = WindowInsets.ime
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon & Title
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(InstitutionalBlue, shape = RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = welcomeTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = InstitutionalBlue
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // List of preset language buttons
                items(defaultLanguagesList) { lang ->
                    val isSelected = selectedLang.equals(lang, ignoreCase = true)
                    Surface(
                        onClick = {
                            selectedLang = lang
                            customLanguageText = ""
                            onLanguageSelected(lang)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) InstitutionalBlue else Color.White,
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.5.dp, InstitutionalBlue.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else InstitutionalBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = lang,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else DarkText
                                    )
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Language TextField section
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = customPlaceholder,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            )
                            OutlinedTextField(
                                value = customLanguageText,
                                onValueChange = { customLanguageText = it },
                                placeholder = { Text("Ej. Zapoteco, Mixteco, Purépecha...", fontSize = 16.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = InstitutionalBlue,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                singleLine = true
                            )
                            if (customLanguageText.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val target = customLanguageText.trim()
                                        selectedLang = target
                                        onLanguageSelected(target)
                                        if (!state.isTranslatingUi) {
                                            // The translation started, so it will be true shortly. 
                                            // But let's just set the flag to navigate when it finishes.
                                            shouldNavigate = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Text(
                                        text = "Traducir a ${customLanguageText.trim()}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Continue Button ONLY visible when customLanguageText is blank
                if (customLanguageText.isBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryAccessibleButton(
                            text = btnContinue,
                            onClick = {
                                if (!state.isTranslatingUi) {
                                    onContinueToHome()
                                } else {
                                    shouldNavigate = true
                                }
                            },
                            containerColor = HealthGreen
                        )
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }

            // Loading Overlay when Translating via Gemini
            if (state.isTranslatingUi) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = InstitutionalBlue,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = state.dictionary["loading_translating_ui"] ?: "Adaptando toda la interfaz con inteligencia empática...",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
