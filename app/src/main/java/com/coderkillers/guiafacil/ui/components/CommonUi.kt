package com.coderkillers.guiafacil.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.coderkillers.guiafacil.R
import androidx.compose.foundation.Image
import com.coderkillers.guiafacil.ui.theme.*
import java.util.Locale

class AndroidTtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale("es", "MX")
        }
    }

    fun speak(text: String, languageName: String) {
        if (!isInitialized || tts == null) return

        val locale = when {
            languageName.contains("English", ignoreCase = true) -> Locale.US
            languageName.contains("Kreyòl", ignoreCase = true) || languageName.contains("Haitiano", ignoreCase = true) -> Locale("ht", "HT")
            languageName.contains("French", ignoreCase = true) || languageName.contains("Francés", ignoreCase = true) -> Locale.FRANCE
            else -> Locale("es", "MX")
        }

        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GuiaFacilTts")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

@Composable
fun rememberTtsManager(context: Context): AndroidTtsManager {
    val ttsManager = remember { AndroidTtsManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }
    return ttsManager
}

@Composable
fun GuiaFacilHeader(
    currentLanguage: String,
    subtitle: String = "Puentes de Oportunidad",
    onLanguagePillClick: () -> Unit
) {
    Surface(
        color = InstitutionalBlue,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo GuíaFácil",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GuíaFácil",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall?.copy(color = Color.White.copy(alpha = 0.85f))
                            ?: MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    )
                }
            }

            // Language Pill Selector
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onLanguagePillClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Cambiar Idioma",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentLanguage,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryAccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = InstitutionalBlue,
    contentColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpeakTextButton(
    textToSpeak: String,
    languageName: String,
    ttsManager: AndroidTtsManager,
    label: String = "Escuchar en voz alta"
) {
    OutlinedButton(
        onClick = { ttsManager.speak(textToSpeak, languageName) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = InstitutionalBlue
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = label,
                tint = InstitutionalBlue,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    color = InstitutionalBlue,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun HelpSupportDialog(
    title: String,
    content: String,
    dismissLabel: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = null,
                tint = InstitutionalBlue,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = InstitutionalBlue),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = dismissLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SoftCreamBackground
    )
}
