package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.alarm.AlarmManagerController
import com.example.audio.ClapDetector
import com.example.data.AppSettings
import com.example.data.SettingsRepository
import com.example.speech.SpeechRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VoiceFinderService : Service() {

    companion object {
        const val CHANNEL_ID_MONITOR = "voice_finder_monitoring_channel"
        const val CHANNEL_ID_ALARM = "voice_finder_alarm_channel"
        const val NOTIFICATION_ID_MONITOR = 1001
        const val NOTIFICATION_ID_ALARM = 1002

        const val ACTION_START_MONITORING = "com.example.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.action.STOP_MONITORING"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_TRIGGER_ALARM = "com.example.action.TRIGGER_ALARM"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM"

        // Global singleton access for ViewModel binding
        var instance: VoiceFinderService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var alarmController: AlarmManagerController
    private lateinit var clapDetector: ClapDetector
    private var speechHelper: SpeechRecognitionHelper? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentSettings = AppSettings()
    private var settingsJob: Job? = null

    val isListeningFlow: StateFlow<Boolean>?
        get() = clapDetector.isListening

    val currentRmsFlow: StateFlow<Float>?
        get() = clapDetector.currentRms

    val clapHitsFlow: StateFlow<Int>?
        get() = clapDetector.clapHitsInWindow

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true
        settingsRepository = SettingsRepository(applicationContext)
        alarmController = AlarmManagerController(applicationContext)

        clapDetector = ClapDetector {
            triggerAlarm("Clap detected!")
        }

        speechHelper = SpeechRecognitionHelper(applicationContext) { spokenText ->
            triggerAlarm("Voice phrase heard: \"$spokenText\"")
        }

        createNotificationChannels()
        acquireWakeLock()
        observeSettings()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceFinderLock::MonitorWakeLock")
            wakeLock?.acquire(10 * 60 * 1000L) // 10 min safe timeout or renewed
        } catch (e: Exception) {
            Log.e("VoiceFinderService", "WakeLock error: ${e.message}")
        }
    }

    private fun observeSettings() {
        settingsJob = serviceScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
                clapDetector.sensitivity = settings.clapSensitivity
                clapDetector.targetClapCount = settings.clapCount
                clapDetector.cooldownMs = settings.clapCooldownSeconds * 1000L

                speechHelper?.targetPhrase = settings.voicePhrase
                speechHelper?.isContinuousMode = settings.voiceEnabled

                updateMonitoringState(settings)
            }
        }
    }

    private fun updateMonitoringState(settings: AppSettings) {
        if (!settings.clapEnabled && !settings.voiceEnabled) {
            clapDetector.stop()
            speechHelper?.stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Start clap detector if enabled
        if (settings.clapEnabled) {
            clapDetector.start(serviceScope)
        } else {
            clapDetector.stop()
        }

        // Start speech recognizer if enabled
        if (settings.voiceEnabled) {
            speechHelper?.isContinuousMode = true
            speechHelper?.startListening()
        } else {
            speechHelper?.stopListening()
        }

        updateNotification(settings)
    }

    private fun triggerAlarm(source: String) {
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            alarmController.startAlarm(
                source = source,
                soundType = settings.alarmSoundType,
                volume = settings.alarmVolume,
                vibrationEnabled = settings.vibrationEnabled,
                flashlightEnabled = settings.flashlightEnabled
            )

            showAlarmNotification(source)

            // Launch Activity to full-screen alert
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_ALARM_ACTIVE", true)
                putExtra("EXTRA_TRIGGER_SOURCE", source)
            }
            startActivity(intent)
        }
    }

    fun manualStopAlarm() {
        alarmController.stopAlarm()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_ALARM)
    }

    fun manualTriggerAlarm(source: String = "Manual Find My Phone") {
        triggerAlarm(source)
    }

    fun getAlarmController(): AlarmManagerController = alarmController

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                manualStopAlarm()
                clapDetector.stop()
                speechHelper?.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_ALARM -> {
                manualTriggerAlarm("App action")
            }
            ACTION_STOP_ALARM -> {
                manualStopAlarm()
            }
            else -> {
                val notification = buildMonitoringNotification(currentSettings)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID_MONITOR,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID_MONITOR, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun buildMonitoringNotification(settings: AppSettings): Notification {
        val stopIntent = Intent(this, VoiceFinderService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeFeatures = buildList {
            if (settings.clapEnabled) add("Clap (${settings.clapCount}x)")
            if (settings.voiceEnabled) add("Voice (\"${settings.voicePhrase}\")")
        }.joinToString(" & ")

        val text = if (activeFeatures.isNotEmpty()) {
            "Listening for: $activeFeatures"
        } else {
            "Voice Finder monitoring is active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID_MONITOR)
            .setContentTitle("Voice Finder Lock is Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "STOP MONITORING", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(settings: AppSettings) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_MONITOR, buildMonitoringNotification(settings))
    }

    private fun showAlarmNotification(triggerSource: String) {
        val stopAlarmIntent = Intent(this, VoiceFinderService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopAlarmPending = PendingIntent.getService(
            this,
            2,
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAlarmScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ALARM_ACTIVE", true)
            putExtra("EXTRA_TRIGGER_SOURCE", triggerSource)
        }
        val openAlarmPending = PendingIntent.getActivity(
            this,
            3,
            openAlarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setContentTitle("PHONE LOCATED! ($triggerSource)")
            .setContentText("Voice Finder alarm is sounding. Tap to dismiss.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openAlarmPending)
            .setFullScreenIntent(openAlarmPending, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "SILENCE ALARM", stopAlarmPending)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_ALARM, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val monitorChannel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                "Microphone Monitoring Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while background clap/voice detection is active."
                setShowBadge(false)
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Find My Phone Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm notifications when clap or voice phrase is detected."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        settingsJob?.cancel()
        serviceScope.cancel()

        clapDetector.stop()
        speechHelper?.destroy()
        alarmController.stopAlarm()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
