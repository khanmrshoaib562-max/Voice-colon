package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = SettingsRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = repository.settingsFlow.first()
                    if (settings.autoStartOnBoot && (settings.clapEnabled || settings.voiceEnabled)) {
                        val serviceIntent = Intent(context, VoiceFinderService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to start service on boot: ${e.message}")
                }
            }
        }
    }
}
