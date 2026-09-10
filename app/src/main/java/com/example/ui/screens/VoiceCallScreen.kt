package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.JarvisOrb
import com.example.ui.components.QuickActionChips
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.JarvisViewModel
import com.example.voice.VoiceState

@Composable
fun VoiceCallScreen(
    viewModel: JarvisViewModel,
    onSwitchToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isScreenOffMode by viewModel.isScreenOffMode.collectAsState()
    val latestAssistantMessage = messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startListening()
        }
    }

    val statusText = when (voiceState) {
        VoiceState.LISTENING -> "Listening to you... (Boliyen)"
        VoiceState.PROCESSING -> "Jarvis thinking... (Soch raha hoon)"
        VoiceState.SPEAKING -> "Jarvis speaking... (Live Audio)"
        VoiceState.ERROR -> "Mic or Connection Error"
        VoiceState.IDLE -> "Tap mic to talk • Speak in Hinglish or English"
    }

    val statusColor = when (voiceState) {
        VoiceState.LISTENING -> CyanPrimary
        VoiceState.PROCESSING -> Color(0xFF818CF8)
        VoiceState.SPEAKING -> CyanGlow
        VoiceState.ERROR -> Color(0xFFEF4444)
        VoiceState.IDLE -> TextSecondary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status and screen-off controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screen-Off Mode and Model Info Row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                // Screen-off toggle chip
                Surface(
                    onClick = { viewModel.toggleScreenOffMode(!isScreenOffMode) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isScreenOffMode) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isScreenOffMode) CyanPrimary else DarkCardBorder
                    ),
                    modifier = Modifier.testTag("screen_off_toggle_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isScreenOffMode) "Screen-Off Active 🌙" else "Screen-Off Disabled 💤",
                            color = if (isScreenOffMode) CyanPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Active Model Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    val activeModelName = if (viewModel.preferences.activeProvider == "gemini") {
                        viewModel.preferences.geminiModel
                    } else {
                        viewModel.preferences.groqModel
                    }
                    Text(
                        text = "⚡ $activeModelName",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Center: Futuristic Animated Jarvis Orb
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            JarvisOrb(
                voiceState = voiceState,
                audioLevel = audioLevel,
                size = 240.dp
            )
        }

        // Transcript / Spoken Text Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val displayText = when {
                voiceState == VoiceState.LISTENING && liveTranscript.isNotBlank() ->
                    "\"$liveTranscript\""
                voiceState == VoiceState.SPEAKING && latestAssistantMessage.isNotBlank() ->
                    latestAssistantMessage
                liveTranscript.isNotBlank() ->
                    "\"$liveTranscript\""
                latestAssistantMessage.isNotBlank() ->
                    latestAssistantMessage
                else ->
                    "\"Arey bhai, salaam! Gujranwala technical institute aur YouTube channel kaisa chal raha hai? Koi anime idea ya reminder chahiye?\""
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (voiceState == VoiceState.LISTENING) "You (Speaking):" else "Jarvis:",
                        color = if (voiceState == VoiceState.LISTENING) CyanGlow else CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayText,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Barge-In (Interrupt button) when Jarvis is speaking
            AnimatedVisibility(
                visible = voiceState == VoiceState.SPEAKING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = { viewModel.interruptSpeech() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF87171)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .testTag("interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt Jarvis",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Barge-in / Interrupt (Ruko)", fontSize = 12.sp)
                }
            }
        }

        // Quick Topic Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Tap to speak a topic:",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            QuickActionChips(
                onChipClicked = { prompt ->
                    viewModel.sendMessage(prompt, isSpoken = true)
                }
            )
        }

        // Bottom Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch to Text Chat Mode
            FilledIconButton(
                onClick = onSwitchToText,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextSecondary
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("switch_to_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Switch to Text Mode"
                )
            }

            // Main Microphone Button
            val isListening = voiceState == VoiceState.LISTENING
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isListening) Color(0xFFEF4444) else CyanPrimary,
                                if (isListening) Color(0xFF991B1B) else CyanDark
                            )
                        )
                    )
                    .border(
                        2.dp,
                        if (isListening) Color(0xFFFCA5A5) else CyanGlow,
                        CircleShape
                    )
            ) {
                FilledIconButton(
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("voice_call_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Mute / Stop Listening" else "Start Talking",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Interrupt or End Voice Call
            FilledIconButton(
                onClick = {
                    viewModel.interruptSpeech()
                    viewModel.stopListening()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = if (voiceState == VoiceState.SPEAKING) Color(0xFFF87171) else TextSecondary
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("end_speech_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Silence Audio"
                )
            }
        }
    }
}
