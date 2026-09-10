package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preference.PreferencesManager
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.JarvisViewModel

@Composable
fun SettingsDialog(
    viewModel: JarvisViewModel,
    onDismiss: () -> Unit
) {
    val prefs = viewModel.preferences
    var provider by remember { mutableStateOf(prefs.activeProvider) }
    var geminiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var groqKey by remember { mutableStateOf(prefs.groqApiKey) }
    var geminiModel by remember { mutableStateOf(prefs.geminiModel) }
    var groqModel by remember { mutableStateOf(prefs.groqModel) }
    var speechRate by remember { mutableFloatStateOf(prefs.speechRate) }
    var speechPitch by remember { mutableFloatStateOf(prefs.speechPitch) }
    var autoSpeak by remember { mutableStateOf(prefs.isAutoSpeakEnabled) }
    var screenOffMode by remember { mutableStateOf(prefs.isScreenOffModeEnabled) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "BYOK & Assistant Settings",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Security Notice Card
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keys are saved locally in Android encrypted SharedPreferences on this device only. Never sent to any 3rd-party server.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Provider Toggle
                Text(
                    text = "Select Active AI Provider:",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Gemini Option
                    val isGemini = provider == PreferencesManager.PROVIDER_GEMINI
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGemini) CyanDark.copy(alpha = 0.3f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGemini) CyanPrimary else DarkCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { provider = PreferencesManager.PROVIDER_GEMINI }
                            .testTag("provider_gemini_selector")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "Google Gemini",
                                fontWeight = FontWeight.Bold,
                                color = if (isGemini) CyanPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Native Audio / Flash",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    // Groq Option
                    val isGroq = provider == PreferencesManager.PROVIDER_GROQ
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGroq) Color(0xFFF97316).copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGroq) Color(0xFFF97316) else DarkCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { provider = PreferencesManager.PROVIDER_GROQ }
                            .testTag("provider_groq_selector")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "Groq (BYOK)",
                                fontWeight = FontWeight.Bold,
                                color = if (isGroq) Color(0xFFF97316) else TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Llama 3.3 + Whisper",
                                fontSize = 10.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }

                // Key inputs
                if (provider == PreferencesManager.PROVIDER_GEMINI) {
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Google Gemini API Key") },
                        placeholder = { Text("Paste AI Studio Gemini Key") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_key_input")
                    )

                    // Quick Selection for Latest Gemini Models
                    Text(
                        text = "Select Gemini Model:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val geminiModels = listOf(
                        PreferencesManager.MODEL_GEMINI_3_5_FLASH to "Gemini 3.5 Flash (Latest & Fast)",
                        PreferencesManager.MODEL_GEMINI_3_1_PRO to "Gemini 3.1 Pro (Reasoning & Code)",
                        PreferencesManager.MODEL_GEMINI_3_1_FLASH_LITE to "Gemini 3.1 Flash Lite (Ultra Fast)",
                        PreferencesManager.MODEL_GEMINI_2_5_NATIVE_AUDIO to "Gemini 2.5 Native Audio (Voice)",
                        PreferencesManager.MODEL_GEMINI_FLASH_LATEST to "Gemini Flash Latest (Auto)"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        geminiModels.forEach { (modelId, label) ->
                            val isSelected = geminiModel.trim() == modelId
                            Surface(
                                onClick = { geminiModel = modelId },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyanPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanPrimary else DarkCardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { geminiModel = modelId },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = CyanPrimary,
                                            unselectedColor = TextSecondary
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            color = if (isSelected) CyanPrimary else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = modelId,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = geminiModel,
                        onValueChange = { geminiModel = it },
                        label = { Text("Custom / Active Model ID") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { groqKey = it },
                        label = { Text("Groq API Key") },
                        placeholder = { Text("gsk_...") },
                        visualTransformation = if (showGroqKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGroqKey = !showGroqKey }) {
                                Icon(
                                    imageVector = if (showGroqKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_key_input")
                    )

                    OutlinedTextField(
                        value = groqModel,
                        onValueChange = { groqModel = it },
                        label = { Text("Groq Model ID") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Voice Tuning Controls
                Text(
                    text = "Voice Output Controls:",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Speak Voice Replies", color = TextSecondary, fontSize = 13.sp)
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { autoSpeak = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanPrimary,
                            checkedTrackColor = CyanDark
                        )
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate", color = TextSecondary, fontSize = 12.sp)
                        Text(String.format("%.1fx", speechRate), color = CyanGlow, fontSize = 12.sp)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary
                        )
                    )
                }

                // Screen-Off Background Operation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (screenOffMode) CyanPrimary.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (screenOffMode) CyanPrimary.copy(alpha = 0.5f) else DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Screen-Off Assistant Mode 🌙",
                                    color = if (screenOffMode) CyanPrimary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Mobile screen lock/off hone par bhi Jarvis sunta aur baat karta rahega (Foreground Service + Wake Lock).",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = screenOffMode,
                                onCheckedChange = { screenOffMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyanPrimary,
                                    checkedTrackColor = CyanDark
                                )
                            )
                        }
                    }
                }

                // Reset / Clear chat
                TextButton(
                    onClick = { viewModel.clearChat() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171)),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Chat History", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveSettings(
                        provider = provider,
                        geminiKey = geminiKey,
                        groqKey = groqKey,
                        geminiModel = geminiModel,
                        groqModel = groqModel,
                        speechRate = speechRate,
                        speechPitch = speechPitch,
                        autoSpeak = autoSpeak,
                        screenOffMode = screenOffMode
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save & Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
