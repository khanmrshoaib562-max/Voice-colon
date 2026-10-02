package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.AlarmSoundType
import com.example.flashlight.FlashlightManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlarmManagerController(private val context: Context) {

    private val flashlightManager = FlashlightManager(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _triggerSource = MutableStateFlow<String>("")
    val triggerSource: StateFlow<String> = _triggerSource.asStateFlow()

    private var soundJob: Job? = null
    private var vibratorJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun startAlarm(
        source: String,
        soundType: AlarmSoundType,
        volume: Float = 0.95f,
        vibrationEnabled: Boolean = true,
        flashlightEnabled: Boolean = true
    ) {
        if (_isAlarmActive.value) return

        _triggerSource.value = source
        _isAlarmActive.value = true

        // 1. Play synthesized loud alarm
        startSound(soundType, volume)

        // 2. Start repeating vibration
        if (vibrationEnabled) {
            startVibration()
        }

        // 3. Start flashlight strobe
        if (flashlightEnabled) {
            flashlightManager.startStrobe(scope, intervalMs = 180L)
        }
    }

    fun stopAlarm() {
        if (!_isAlarmActive.value) return

        _isAlarmActive.value = false
        _triggerSource.value = ""

        stopSound()
        stopVibration()
        flashlightManager.stopStrobe()
    }

    private fun startSound(soundType: AlarmSoundType, volume: Float) {
        soundJob?.cancel()
        soundJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRate / 2)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.setVolume(volume.coerceIn(0.1f, 1.0f))
            track.play()

            val buffer = ShortArray(bufferSize)
            var phase = 0.0

            try {
                var tick = 0L
                while (isActive && _isAlarmActive.value) {
                    for (i in buffer.indices) {
                        val t = (tick + i).toDouble() / sampleRate
                        val freq = calculateFrequency(soundType, t)
                        phase += 2.0 * Math.PI * freq / sampleRate
                        val amplitude = calculateAmplitude(soundType, t)
                        buffer[i] = (sin(phase) * 32767 * amplitude).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    tick += buffer.size
                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("AlarmController", "Error during sound playback: ${e.message}")
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun calculateFrequency(soundType: AlarmSoundType, timeSec: Double): Double {
        return when (soundType) {
            AlarmSoundType.SIREN -> {
                // Modulating siren 600Hz to 1600Hz every 1.2s
                val cycle = (timeSec % 1.2) / 1.2
                val modulation = sin(cycle * 2.0 * Math.PI)
                1100.0 + (500.0 * modulation)
            }
            AlarmSoundType.RADAR -> {
                // High pitch radar pulses 1800Hz
                1800.0
            }
            AlarmSoundType.CHIME -> {
                // Ascending 3-tone arpeggio 523Hz, 659Hz, 784Hz
                val step = ((timeSec * 4) % 3).toInt()
                when (step) {
                    0 -> 523.25
                    1 -> 659.25
                    else -> 783.99
                }
            }
            AlarmSoundType.ALARM_BELL -> {
                // Alternating two-tone bell 900Hz and 1300Hz
                val step = ((timeSec * 6) % 2).toInt()
                if (step == 0) 900.0 else 1300.0
            }
        }
    }

    private fun calculateAmplitude(soundType: AlarmSoundType, timeSec: Double): Double {
        return when (soundType) {
            AlarmSoundType.SIREN -> 0.95
            AlarmSoundType.RADAR -> {
                // Beeping radar (300ms on, 200ms off)
                val pulseTime = timeSec % 0.5
                if (pulseTime < 0.3) 0.95 else 0.0
            }
            AlarmSoundType.CHIME -> 0.9
            AlarmSoundType.ALARM_BELL -> {
                val cycle = timeSec % 0.166
                if (cycle < 0.12) 0.95 else 0.05
            }
        }
    }

    private fun stopSound() {
        soundJob?.cancel()
        soundJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun startVibration() {
        vibratorJob?.cancel()
        vibratorJob = scope.launch(Dispatchers.Default) {
            val pattern = longArrayOf(0, 400, 200, 400, 200, 800, 300)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 0)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                Log.e("AlarmController", "Vibration error: ${e.message}")
            }
        }
    }

    private fun stopVibration() {
        vibratorJob?.cancel()
        vibratorJob = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
