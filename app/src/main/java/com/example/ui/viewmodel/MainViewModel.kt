package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmManagerController
import com.example.data.AlarmSoundType
import com.example.data.AppSettings
import com.example.data.ClapSensitivity
import com.example.data.SettingsRepository
import com.example.security.SecurityManager
import com.example.service.VoiceFinderService
import com.example.speech.SpeechRecognitionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val settingsRepository = SettingsRepository(context)
    val securityManager = SecurityManager(context)
    val localAlarmController = AlarmManagerController(context)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _alarmTriggerReason = MutableStateFlow("")
    val alarmTriggerReason: StateFlow<String> = _alarmTriggerReason.asStateFlow()

    private val _isUnlocked = MutableStateFlow(true)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Test Voice states
    private var testSpeechHelper: SpeechRecognitionHelper? = null
    private val _testListening = MutableStateFlow(false)
    val testListening: StateFlow<Boolean> = _testListening.asStateFlow()

    private val _testHeardText = MutableStateFlow("")
    val testHeardText: StateFlow<String> = _testHeardText.asStateFlow()

    private val _testStatus = MutableStateFlow("Ready to test")
    val testStatus: StateFlow<String> = _testStatus.asStateFlow()

    private val _testRms = MutableStateFlow(0f)
    val testRms: StateFlow<Float> = _testRms.asStateFlow()

    private val _testMatched = MutableStateFlow(false)
    val testMatched: StateFlow<Boolean> = _testMatched.asStateFlow()

    // Live audio meter flows polled from service when running
    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private val _clapHits = MutableStateFlow(0)
    val clapHits: StateFlow<Int> = _clapHits.asStateFlow()

    private var pollJob: Job? = null

    init {
        // App Lock check
        viewModelScope.launch {
            settings.collect { current ->
                if (current.appLockEnabled && current.appPin.isNotEmpty() && _isUnlocked.value) {
                    // Will be set to false if user newly enabled lock, but on initial startup we lock
                }
            }
        }

        // Start polling service status & meters
        startServicePolling()
    }

    fun checkInitialLockState() {
        val current = settings.value
        if (current.appLockEnabled && current.appPin.isNotEmpty()) {
            _isUnlocked.value = false
        } else {
            _isUnlocked.value = true
        }
    }

    private fun startServicePolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val service = VoiceFinderService.instance
                val running = service != null && VoiceFinderService.isServiceRunning.value
                _isServiceActive.value = running

                if (running && service != null) {
                    val serviceAlarm = service.getAlarmController()
                    val alarmActive = serviceAlarm.isAlarmActive.value || localAlarmController.isAlarmActive.value
                    _isAlarmActive.value = alarmActive
                    if (serviceAlarm.isAlarmActive.value) {
                        _alarmTriggerReason.value = serviceAlarm.triggerSource.value
                    } else if (localAlarmController.isAlarmActive.value) {
                        _alarmTriggerReason.value = localAlarmController.triggerSource.value
                    }
                    _micLevel.value = service.currentRmsFlow?.value ?: 0f
                    _clapHits.value = service.clapHitsFlow?.value ?: 0
                } else {
                    _isAlarmActive.value = localAlarmController.isAlarmActive.value
                    _alarmTriggerReason.value = localAlarmController.triggerSource.value
                    _micLevel.value = 0f
                    _clapHits.value = 0
                }
                delay(300L)
            }
        }
    }

    fun startFindMyPhone() {
        val current = settings.value
        val service = VoiceFinderService.instance
        if (service != null) {
            service.manualTriggerAlarm("FIND MY PHONE BUTTON")
        } else {
            localAlarmController.startAlarm(
                source = "FIND MY PHONE BUTTON",
                soundType = current.alarmSoundType,
                volume = current.alarmVolume,
                vibrationEnabled = current.vibrationEnabled,
                flashlightEnabled = current.flashlightEnabled
            )
        }
        _isAlarmActive.value = true
        _alarmTriggerReason.value = "FIND MY PHONE BUTTON"
    }

    fun stopAlarm() {
        VoiceFinderService.instance?.manualStopAlarm()
        localAlarmController.stopAlarm()
        _isAlarmActive.value = false
        _alarmTriggerReason.value = ""
    }

    fun toggleVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVoiceEnabled(enabled)
            val updated = settings.value.copy(voiceEnabled = enabled)
            syncService(updated)
        }
    }

    fun toggleClapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setClapEnabled(enabled)
            val updated = settings.value.copy(clapEnabled = enabled)
            syncService(updated)
        }
    }

    fun startService() {
        if (!hasAudioPermission()) return
        val intent = Intent(context, VoiceFinderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService() {
        val intent = Intent(context, VoiceFinderService::class.java).apply {
            action = VoiceFinderService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    private fun syncService(settings: AppSettings) {
        if (settings.clapEnabled || settings.voiceEnabled) {
            if (hasAudioPermission()) {
                startService()
            }
        } else {
            stopService()
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Voice test
    fun startVoiceTest() {
        _testMatched.value = false
        _testHeardText.value = ""
        _testStatus.value = "Listening..."
        _testListening.value = true

        testSpeechHelper?.destroy()
        testSpeechHelper = SpeechRecognitionHelper(context) { heard ->
            _testMatched.value = true
            _testStatus.value = "MATCH DETECTED! ✓"
            _testHeardText.value = heard
        }.apply {
            targetPhrase = settings.value.voicePhrase
            isContinuousMode = false
            startListening()
        }

        viewModelScope.launch {
            testSpeechHelper?.speechRms?.collect { rms ->
                _testRms.value = rms
            }
        }
        viewModelScope.launch {
            testSpeechHelper?.statusMessage?.collect { status ->
                _testStatus.value = status
            }
        }
        viewModelScope.launch {
            testSpeechHelper?.lastRecognizedText?.collect { text ->
                if (text.isNotEmpty()) {
                    _testHeardText.value = text
                }
            }
        }
        viewModelScope.launch {
            testSpeechHelper?.isListening?.collect { listening ->
                _testListening.value = listening
            }
        }
    }

    fun stopVoiceTest() {
        testSpeechHelper?.stopListening()
        testSpeechHelper?.destroy()
        testSpeechHelper = null
        _testListening.value = false
        _testRms.value = 0f
    }

    // Settings mutators
    fun setVoicePhrase(phrase: String) {
        viewModelScope.launch { settingsRepository.setVoicePhrase(phrase) }
    }

    fun setClapSensitivity(sensitivity: ClapSensitivity) {
        viewModelScope.launch { settingsRepository.setClapSensitivity(sensitivity) }
    }

    fun setClapCount(count: Int) {
        viewModelScope.launch { settingsRepository.setClapCount(count) }
    }

    fun setClapCooldown(seconds: Int) {
        viewModelScope.launch { settingsRepository.setClapCooldown(seconds) }
    }

    fun setAlarmVolume(volume: Float) {
        viewModelScope.launch { settingsRepository.setAlarmVolume(volume) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    fun setFlashlightEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFlashlightEnabled(enabled) }
    }

    fun setAlarmSoundType(type: AlarmSoundType) {
        viewModelScope.launch { settingsRepository.setAlarmSoundType(type) }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoStartOnBoot(enabled) }
    }

    // App Lock PIN & Biometrics
    fun configureAppLock(enabled: Boolean, newPin: String = "") {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
            if (newPin.isNotEmpty()) {
                val hashed = securityManager.hashPin(newPin)
                settingsRepository.setAppPin(hashed)
            }
            if (!enabled) {
                _isUnlocked.value = true
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun verifyPinInput(pin: String): Boolean {
        val storedHash = settings.value.appPin
        val isValid = securityManager.verifyPin(pin, storedHash)
        if (isValid) {
            _isUnlocked.value = true
        }
        return isValid
    }

    fun unlockWithBiometric(activity: FragmentActivity, onError: (String) -> Unit) {
        securityManager.authenticateWithBiometrics(
            activity = activity,
            onSuccess = {
                _isUnlocked.value = true
            },
            onError = onError
        )
    }

    fun lockApp() {
        if (settings.value.appLockEnabled && settings.value.appPin.isNotEmpty()) {
            _isUnlocked.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        testSpeechHelper?.destroy()
        localAlarmController.stopAlarm()
    }
}
