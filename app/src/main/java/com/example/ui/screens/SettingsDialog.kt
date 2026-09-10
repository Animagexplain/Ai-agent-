package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var geminiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(prefs.geminiModel) }
    var autoSpeak by remember { mutableStateOf(prefs.isAutoSpeakEnabled) }
    var screenOffMode by remember { mutableStateOf(prefs.isScreenOffModeEnabled) }
    var showGeminiKey by remember { mutableStateOf(false) }

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
                    text = "Rika Settings 💜",
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
                // Gemini API Key
                Column {
                    Text(
                        text = "Google Gemini API Key",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        placeholder = { Text("Using pre-configured key", color = TextTertiary, fontSize = 12.sp) },
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
                }

                // Gemini Model Selection
                Text(
                    text = "Active Gemini Model:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val geminiModels: List<Pair<String, String>> = listOf(
                    PreferencesManager.MODEL_GEMINI_2_5_FLASH to "Gemini 2.5 Flash (Super Fast ⚡ Recommended)",
                    PreferencesManager.MODEL_GEMINI_FLASH_LATEST to "Gemini Flash Latest (Auto Fast)",
                    PreferencesManager.MODEL_GEMINI_3_1_FLASH_LITE to "Gemini Flash Lite (Ultra Snappy)"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    geminiModels.forEach { (modelId, label) ->
                        val isSelected = geminiModel.trim() == modelId
                        Surface(
                            onClick = { geminiModel = modelId },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanPrimary else DarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                Text(
                                    text = label,
                                    color = if (isSelected) CyanPrimary else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Auto-Speak Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Speak Voice Replies", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Text chat me bhi Rika aawaz se jawab degi", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = { autoSpeak = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanPrimary,
                                checkedTrackColor = CyanDark
                            )
                        )
                    }
                }

                // Screen-Off Mode Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (screenOffMode) CyanPrimary.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (screenOffMode) CyanPrimary.copy(alpha = 0.5f) else DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen-Off Mode 🌙",
                                color = if (screenOffMode) CyanPrimary else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Screen lock hone par bhi continuous voice active rahegi",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
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

                // Clear Chat Button
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
                        provider = PreferencesManager.PROVIDER_GEMINI,
                        geminiKey = geminiKey,
                        groqKey = prefs.groqApiKey,
                        geminiModel = geminiModel,
                        groqModel = prefs.groqModel,
                        speechRate = prefs.speechRate,
                        speechPitch = prefs.speechPitch,
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
