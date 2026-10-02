package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AudioMeterVisualizer
import com.example.ui.components.SecurityCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Navy600
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TestVoiceScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.testListening.collectAsState()
    val heardText by viewModel.testHeardText.collectAsState()
    val status by viewModel.testStatus.collectAsState()
    val rms by viewModel.testRms.collectAsState()
    val isMatched by viewModel.testMatched.collectAsState()

    var hasMicPermission by remember { mutableStateOf(viewModel.hasAudioPermission()) }
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startVoiceTest()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopVoiceTest()
        }
    }

    BackHandler {
        viewModel.stopVoiceTest()
        onNavigateBack()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy900)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.stopVoiceTest()
                    onNavigateBack()
                },
                modifier = Modifier
                    .testTag("test_voice_back_button")
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Navy800)
                    .border(1.dp, Navy600, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Test Voice Detection",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verify speech recognition accuracy",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Target Phrase Card
        SecurityCard(
            modifier = Modifier.padding(bottom = 20.dp),
            borderColor = ElectricBlue.copy(alpha = 0.5f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TARGET PHRASE",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${settings.voicePhrase}\"",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Speak this phrase into the microphone to test detection.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Live Mic Button & Feedback
        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .then(if (isListening) Modifier.scale(pulseScale) else Modifier)
                    .clip(CircleShape)
                    .background(
                        if (isMatched) SuccessGreen.copy(alpha = 0.2f)
                        else if (isListening) ElectricBlue.copy(alpha = 0.25f)
                        else Navy800
                    )
                    .border(
                        2.dp,
                        if (isMatched) SuccessGreen
                        else if (isListening) ElectricCyan
                        else Navy600,
                        CircleShape
                    )
            )

            IconButton(
                onClick = {
                    if (!hasMicPermission) {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (isListening) {
                            viewModel.stopVoiceTest()
                        } else {
                            viewModel.startVoiceTest()
                        }
                    }
                },
                modifier = Modifier
                    .testTag("toggle_voice_test_mic")
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMatched) SuccessGreen
                        else if (isListening) ElectricBlue
                        else Navy700
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Test Mic",
                    tint = TextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Text(
            text = if (isListening) "Listening..." else "Tap microphone to test",
            color = if (isListening) ElectricCyan else TextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Audio Level Meter
        if (isListening) {
            AudioMeterVisualizer(
                rmsLevel = rms,
                label = "Speech Input Volume",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Status Card
        SecurityCard(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "STATUS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status,
                    color = if (isMatched) SuccessGreen else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                if (heardText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "LAST HEARD PHRASE",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$heardText\"",
                        color = ElectricCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Match celebration banner
        AnimatedVisibility(visible = isMatched) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Phrase Match Confirmed!",
                            color = SuccessGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SpeechRecognizer recognized your voice. The background service will trigger the alarm reliably.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Troubleshooting & Tips
        SecurityCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tips",
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Voice Recognition Tips",
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Speak clearly at a moderate volume.\n• Choose a distinct 3-5 word phrase like \"hello find my phone\".\n• Ensure Google Speech Services / Assistant is enabled on your phone.\n• Background noise can affect accuracy.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
