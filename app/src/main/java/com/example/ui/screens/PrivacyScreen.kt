package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SecurityBoundaryNotice
import com.example.ui.components.SecurityCard
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

@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .testTag("privacy_back_button")
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
                    text = "Privacy & Data Policy",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transparency on microphone and device usage",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Top Banner
        SecurityCard(
            modifier = Modifier.padding(bottom = 16.dp),
            borderColor = SuccessGreen.copy(alpha = 0.5f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "100% Local On-Device Processing",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Audio never leaves your phone. Zero audio recordings are stored, saved, or uploaded.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Commitments list
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrivacyItem(
                icon = Icons.Default.Mic,
                title = "Local Audio Analysis Only",
                description = "Microphone data is streamed strictly into transient RAM buffers to calculate sound amplitude (RMS) or match speech phonemes. Buffers are discarded immediately after analysis."
            )

            PrivacyItem(
                icon = Icons.Default.CloudOff,
                title = "Zero Audio Uploads",
                description = "There are no telemetry servers or remote audio databases. Voice Finder Lock does not maintain any cloud backend for audio collection."
            )

            PrivacyItem(
                icon = Icons.Default.NoAccounts,
                title = "No Personal Data Collected",
                description = "The application does not request access to your contacts, SMS messages, call logs, photos, media files, or GPS location."
            )

            PrivacyItem(
                icon = Icons.Default.NotificationsActive,
                title = "Foreground Service Transparency",
                description = "Android requires a persistent notification while the microphone is active in the background. We provide an instant [STOP] button right in the notification bar so you always have full control."
            )

            PrivacyItem(
                icon = Icons.Default.Security,
                title = "Legitimate Security Boundaries",
                description = "In strict accordance with Android OS security guidelines, Voice Finder Lock does not bypass your device's lock screen (PIN, Pattern, FRP, or Biometrics). The in-app PIN feature is solely designed to protect this application's configuration."
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SecurityBoundaryNotice()

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun PrivacyItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Navy700),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
