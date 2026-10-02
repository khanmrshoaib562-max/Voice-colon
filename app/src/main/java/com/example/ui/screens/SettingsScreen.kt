package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmSoundType
import com.example.data.ClapSensitivity
import com.example.ui.components.SecurityBoundaryNotice
import com.example.ui.components.SecurityCard
import com.example.ui.components.ToggleSettingRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTestVoice: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    var phraseText by remember(settings.voicePhrase) { mutableStateOf(settings.voicePhrase) }
    var hasMicPermission by remember { mutableStateOf(viewModel.hasAudioPermission()) }
    var hasNotifPermission by remember { mutableStateOf(viewModel.hasNotificationPermission()) }
    var hasCameraPermission by remember { mutableStateOf(viewModel.hasCameraPermission()) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMicPermission = it
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasNotifPermission = it
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }

    BackHandler {
        onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy900)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .testTag("settings_back_button")
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
                    text = "Settings & Configuration",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personalize sound triggers, alarms, and lock",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Section: VOICE DETECTION
        SectionHeader("Voice Detection")

        SecurityCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column {
                ToggleSettingRow(
                    title = "Enable Voice Detection",
                    subtitle = "Listens for your custom phrase in the background",
                    icon = Icons.Default.RecordVoiceOver,
                    checked = settings.voiceEnabled,
                    onCheckedChange = { viewModel.toggleVoiceEnabled(it) },
                    testTag = "settings_voice_toggle"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Custom Trigger Phrase",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phraseText,
                        onValueChange = { phraseText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_phrase_input"),
                        singleLine = true,
                        placeholder = { Text("e.g. find my phone", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Navy600,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Navy900,
                            unfocusedContainerColor = Navy900
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.setVoicePhrase(phraseText) },
                        modifier = Modifier.testTag("save_phrase_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToTestVoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_test_voice_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice Recognition Now", color = ElectricCyan, fontSize = 13.sp)
                }
            }
        }

        // Section: CLAP DETECTION
        SectionHeader("Clap Detection")

        SecurityCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column {
                ToggleSettingRow(
                    title = "Enable Clap Finder",
                    subtitle = "Listens for quick acoustic peaks",
                    icon = Icons.Default.WavingHand,
                    checked = settings.clapEnabled,
                    onCheckedChange = { viewModel.toggleClapEnabled(it) },
                    testTag = "settings_clap_toggle"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sensitivity selector
                Text(
                    text = "Detection Sensitivity",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClapSensitivity.entries.forEach { sens ->
                        val isSelected = settings.clapSensitivity == sens
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricBlue else Navy900)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else Navy600,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setClapSensitivity(sens) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sens.name,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Number of claps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Claps Required", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text = "${settings.clapCount} claps",
                        color = ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.clapCount.toFloat(),
                    onValueChange = { viewModel.setClapCount(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.testTag("clap_count_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Navy900
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cooldown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Trigger Cooldown", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text = "${settings.clapCooldownSeconds} seconds",
                        color = ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.clapCooldownSeconds.toFloat(),
                    onValueChange = { viewModel.setClapCooldown(it.toInt()) },
                    valueRange = 2f..10f,
                    steps = 7,
                    modifier = Modifier.testTag("clap_cooldown_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Navy900
                    )
                )
            }
        }

        // Section: ALARM & SIGNALS
        SectionHeader("Alarm & Signal Customization")

        SecurityCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column {
                // Sound Type Selector
                Text(
                    text = "Alarm Sound Pattern",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlarmSoundType.entries.forEach { soundType ->
                        val isSelected = settings.alarmSoundType == soundType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricBlue.copy(alpha = 0.2f) else Navy900)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else Navy700,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setAlarmSoundType(soundType) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isSelected) ElectricCyan else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = soundType.title,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Alarm Volume", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text = "${(settings.alarmVolume * 100).toInt()}%",
                        color = ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.alarmVolume,
                    onValueChange = { viewModel.setAlarmVolume(it) },
                    valueRange = 0.2f..1.0f,
                    modifier = Modifier.testTag("alarm_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Navy900
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToggleSettingRow(
                    title = "Vibration",
                    subtitle = "Vibrate phone in alarm pattern",
                    icon = Icons.Default.Vibration,
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                    testTag = "settings_vibration_toggle"
                )

                ToggleSettingRow(
                    title = "Flashlight Strobe",
                    subtitle = "Blink rear camera LED flash repeatedly",
                    icon = Icons.Default.FlashOn,
                    checked = settings.flashlightEnabled,
                    onCheckedChange = { viewModel.setFlashlightEnabled(it) },
                    testTag = "settings_flashlight_toggle"
                )
            }
        }

        // Section: IN-APP SECURITY (APP PIN & BIOMETRIC)
        SectionHeader("In-App Security (Settings Lock)")

        SecurityCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (settings.appLockEnabled) ElectricBlue.copy(alpha = 0.2f) else Navy700),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.appLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (settings.appLockEnabled) ElectricCyan else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Settings PIN Lock",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (settings.appLockEnabled) "App settings protected with PIN" else "Disabled",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                viewModel.configureAppLock(false)
                            }
                        },
                        modifier = Modifier.testTag("app_lock_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Navy900
                        )
                    )
                }

                if (settings.appLockEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_pin_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Navy600)
                    ) {
                        Text("Change In-App PIN", color = TextPrimary, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ToggleSettingRow(
                        title = "Biometric Unlock",
                        subtitle = "Allow Fingerprint / Face authentication",
                        icon = Icons.Default.Fingerprint,
                        checked = settings.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        testTag = "biometric_switch"
                    )
                }
            }
        }

        // Section: PERMISSIONS & SYSTEM
        SectionHeader("Permissions & Background Monitoring")

        SecurityCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PermissionStatusRow(
                    name = "Microphone (Record Audio)",
                    isGranted = hasMicPermission,
                    onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
                PermissionStatusRow(
                    name = "Notifications",
                    isGranted = hasNotifPermission,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                PermissionStatusRow(
                    name = "Camera (Flashlight Strobe)",
                    isGranted = hasCameraPermission,
                    onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Battery Optimization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Battery Optimization",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Disable aggressive battery killing on devices like Realme/Xiaomi",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.testTag("battery_opt_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700)
                    ) {
                        Text("Configure", fontSize = 11.sp, color = ElectricCyan)
                    }
                }

                ToggleSettingRow(
                    title = "Auto-Start on Boot",
                    subtitle = "Restart sound detection after phone reboot",
                    icon = Icons.Default.Security,
                    checked = settings.autoStartOnBoot,
                    onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
                    testTag = "auto_boot_switch"
                )
            }
        }

        // Section: PRIVACY & SYSTEM BOUNDARY
        SectionHeader("Privacy & Security Guarantee")

        SecurityCard(modifier = Modifier.padding(bottom = 20.dp)) {
            Column {
                SecurityBoundaryNotice()
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToPrivacy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_privacy_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy600)
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read Full Privacy Statement", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = ""
            },
            title = {
                Text(
                    text = "Configure App Settings PIN",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a 4-digit PIN to restrict access to this application's settings. Note: this does not affect your Android device lock screen.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                pinInput = it
                                pinError = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_dialog_input"),
                        singleLine = true,
                        placeholder = { Text("Enter 4 digits", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Navy600,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Navy900,
                            unfocusedContainerColor = Navy900
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (pinError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pinError, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.configureAppLock(true, pinInput)
                            showPinDialog = false
                            pinInput = ""
                            pinError = ""
                        } else {
                            pinError = "PIN must be exactly 4 digits"
                        }
                    },
                    modifier = Modifier.testTag("confirm_pin_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinInput = ""
                    pinError = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Navy800,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = ElectricCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun PermissionStatusRow(
    name: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (isGranted) "Granted ✓" else "Not Granted",
                color = if (isGranted) SuccessGreen else AlertRed,
                fontSize = 11.sp
            )
        }
        if (!isGranted) {
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Allow", fontSize = 11.sp)
            }
        }
    }
}
