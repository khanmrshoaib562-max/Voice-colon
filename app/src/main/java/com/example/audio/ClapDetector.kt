package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.data.ClapSensitivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

class ClapDetector(
    private val onClapDetected: () -> Unit
) {
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    private val _clapHitsInWindow = MutableStateFlow(0)
    val clapHitsInWindow: StateFlow<Int> = _clapHitsInWindow.asStateFlow()

    // Configurable parameters
    var sensitivity: ClapSensitivity = ClapSensitivity.MEDIUM
    var targetClapCount: Int = 2
    var cooldownMs: Long = 4000L

    private val recentClapTimestamps = mutableListOf<Long>()
    private var lastTriggerTime = 0L
    private var lastPeakTime = 0L

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        if (_isListening.value) return true

        try {
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = max(minBufSize, 2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("ClapDetector", "AudioRecord initialization failed.")
                stop()
                return false
            }

            audioRecord?.startRecording()
            _isListening.value = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(512)
                var noiseFloor = 150.0 // dynamic ambient floor

                while (isActive && _isListening.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        var sumSquare = 0.0
                        var maxPeak = 0

                        for (i in 0 until read) {
                            val sample = buffer[i].toInt()
                            sumSquare += sample * sample
                            val absVal = kotlin.math.abs(sample)
                            if (absVal > maxPeak) {
                                maxPeak = absVal
                            }
                        }

                        val rms = sqrt(sumSquare / read)
                        _currentRms.value = (rms / 32767.0).coerceIn(0.0, 1.0).toFloat()

                        // Smooth ambient noise floor
                        noiseFloor = (noiseFloor * 0.95) + (rms * 0.05)

                        val now = System.currentTimeMillis()

                        // Threshold computation based on sensitivity and ambient noise
                        val baseThreshold = when (sensitivity) {
                            ClapSensitivity.LOW -> 7500.0
                            ClapSensitivity.MEDIUM -> 4500.0
                            ClapSensitivity.HIGH -> 2600.0
                        }
                        val dynamicThreshold = max(baseThreshold, noiseFloor * 2.8)

                        // Check if an impulse peak occurred
                        val isPeak = maxPeak > dynamicThreshold
                        val isRefractoryPassed = (now - lastPeakTime) > 220L // minimum 220ms between claps

                        if (isPeak && isRefractoryPassed) {
                            lastPeakTime = now

                            // Clean up claps older than 1800ms window
                            recentClapTimestamps.removeAll { now - it > 1800L }
                            recentClapTimestamps.add(now)
                            _clapHitsInWindow.value = recentClapTimestamps.size

                            Log.d("ClapDetector", "Clap registered! Count in window: ${recentClapTimestamps.size}/$targetClapCount (Peak: $maxPeak)")

                            if (recentClapTimestamps.size >= targetClapCount) {
                                if (now - lastTriggerTime > cooldownMs) {
                                    lastTriggerTime = now
                                    recentClapTimestamps.clear()
                                    _clapHitsInWindow.value = 0
                                    launch(Dispatchers.Main) {
                                        onClapDetected()
                                    }
                                } else {
                                    Log.d("ClapDetector", "Clap ignored due to cooldown.")
                                    recentClapTimestamps.clear()
                                    _clapHitsInWindow.value = 0
                                }
                            }
                        } else {
                            // Periodically clear window if time expired
                            if (recentClapTimestamps.isNotEmpty() && now - recentClapTimestamps.last() > 1800L) {
                                recentClapTimestamps.clear()
                                _clapHitsInWindow.value = 0
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("ClapDetector", "Exception starting clap detector: ${e.message}")
            stop()
            return false
        }
    }

    fun stop() {
        _isListening.value = false
        _currentRms.value = 0f
        _clapHitsInWindow.value = 0
        recentClapTimestamps.clear()

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
