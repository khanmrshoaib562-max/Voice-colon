package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AudioMeterVisualizer
import com.example.ui.components.SecurityBoundaryNotice
import com.example.ui.components.SecurityCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.ToggleSettingRow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BrightBlue
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
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTestVoice: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isServiceActive by viewModel.isServiceActive.collectAsState()
    val micLevel by viewModel.micLevel.collectAsState()
    val clapHits by viewModel.clapHits.collectAsState()

    var hasMicPermission by remember { mutableStateOf(viewModel.hasAudioPermission()) }
    var hasNotifPermission by remember { mutableStateOf(viewModel.hasNotificationPermission()) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted && (settings.clapEnabled || settings.voiceEnabled)) {
            viewModel.startService()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    // Animation for Find My Phone button glow
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricBlue, ElectricCyan)
                            )
                        )
                        .border(1.5.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "App Icon",
                        tint = Navy900,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Voice Finder Lock",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "Sound Detector & Phone Finder",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .testTag("settings_button")
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Navy800)
                    .border(1.dp, Navy600, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Permission warning banners if needed
        if (!hasMicPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AlertRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Microphone Permission Required",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Grant audio access so the app can hear your claps and voice commands.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.testTag("grant_mic_permission_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notifications",
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Permission",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Required for the background monitoring service notification.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.testTag("grant_notif_permission_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable", color = Navy900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Monitoring Service Status Card
        SecurityCard(
            modifier = Modifier.padding(bottom = 16.dp),
            borderColor = if (isServiceActive) ElectricCyan.copy(alpha = 0.5f) else Navy600
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isServiceActive) ElectricCyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background Monitor",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    StatusBadge(
                        isActive = isServiceActive,
                        activeText = "MONITORING",
                        inactiveText = "OFF"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isServiceActive) {
                    AudioMeterVisualizer(
                        rmsLevel = micLevel,
                        label = "Live Acoustic Energy"
                    )
                    if (clapHits > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Claps registered in window: $clapHits / ${settings.clapCount}",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Turn ON Clap Finder or Voice Command below to start detecting sound in the background.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Feature Toggles Section: [Voice Lock ON] and [Clap Finder ON]
        SecurityCard(
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Column {
                ToggleSettingRow(
                    title = "Clap Finder",
                    subtitle = "Triggers alarm on ${settings.clapCount} claps (${settings.clapSensitivity.name.lowercase().replaceFirstChar { it.uppercase() }} sensitivity)",
                    icon = Icons.Default.WavingHand,
                    checked = settings.clapEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleClapEnabled(checked)
                        }
                    },
                    testTag = "toggle_clap_finder"
                )
            }
        }

        SecurityCard(
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Column {
                ToggleSettingRow(
                    title = "Voice Command",
                    subtitle = "Triggers alarm on phrase: \"${settings.voicePhrase}\"",
                    icon = Icons.Default.RecordVoiceOver,
                    checked = settings.voiceEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleVoiceEnabled(checked)
                        }
                    },
                    testTag = "toggle_voice_command"
                )
            }
        }

        // Large "FIND MY PHONE" Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { viewModel.startFindMyPhone() },
                modifier = Modifier
                    .testTag("find_my_phone_button")
                    .fillMaxWidth()
                    .height(72.dp)
                    .scale(beaconScale)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = ElectricBlue,
                        spotColor = ElectricCyan
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0052D4), Color(0xFF4364F7), Color(0xFF6FB1FC))
                            )
                        )
                        .border(1.5.dp, ElectricCyan.copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alarm",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "FIND MY PHONE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Action Buttons: Test Voice & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToTestVoice,
                modifier = Modifier
                    .testTag("test_voice_button")
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Navy800
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Test Voice",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .testTag("home_settings_button")
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy600),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Navy800
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Notice Card
        SecurityBoundaryNotice()

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy link button
        Row(
            modifier = Modifier
                .clickable { onNavigateToPrivacy() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Policy,
                contentDescription = "Privacy Policy",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Privacy & Local Audio Processing Guarantee",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
