package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognitionHelper(
    private val context: Context,
    private val onPhraseDetected: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _speechRms = MutableStateFlow(0f)
    val speechRms: StateFlow<Float> = _speechRms.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    var targetPhrase: String = "find my phone"
    var isContinuousMode: Boolean = false

    private var isDestroyed = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            _statusMessage.value = "Speech recognition is not available on this device"
            return
        }

        mainHandler.post {
            if (isDestroyed) return@post
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _statusMessage.value = "Listening for \"$targetPhrase\"..."
            } catch (e: Exception) {
                Log.e("SpeechHelper", "Error starting listening: ${e.message}")
                _statusMessage.value = "Error: ${e.localizedMessage}"
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _statusMessage.value = "Stopped"
        }
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _isListening.value = false
        }
    }

    private fun restartIfContinuous(delayMs: Long = 600L) {
        if (isContinuousMode && !isDestroyed) {
            mainHandler.postDelayed({
                if (isContinuousMode && !isDestroyed) {
                    startListening()
                }
            }, delayMs)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _statusMessage.value = "Ready! Say \"$targetPhrase\""
            }

            override fun onBeginningOfSpeech() {
                _statusMessage.value = "Hearing speech..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges typically from -2 to 10
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _speechRms.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _statusMessage.value = "Processing speech..."
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Recognition error ($error)"
                }
                Log.d("SpeechHelper", "SpeechRecognizer error: $errorMsg ($error)")
                _statusMessage.value = errorMsg
                _isListening.value = false

                // If continuous mode and not fatal permission error, restart
                if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    restartIfContinuous(1000L)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _isListening.value = false
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase().trim()
                    _lastRecognizedText.value = spokenText
                    Log.d("SpeechHelper", "Recognized text: $spokenText (Matches: $matches)")

                    if (checkPhraseMatch(spokenText, matches)) {
                        _statusMessage.value = "Match found: \"$spokenText\"!"
                        onPhraseDetected(spokenText)
                    } else {
                        _statusMessage.value = "Heard: \"$spokenText\""
                    }
                } else {
                    _statusMessage.value = "No match"
                }

                restartIfContinuous(700L)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partials.isNullOrEmpty()) {
                    val partialText = partials[0].lowercase().trim()
                    _lastRecognizedText.value = partialText
                    if (checkPhraseMatch(partialText, partials)) {
                        _statusMessage.value = "Match detected: \"$partialText\"!"
                        onPhraseDetected(partialText)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun checkPhraseMatch(spoken: String, allCandidates: List<String>): Boolean {
        val target = targetPhrase.lowercase().trim()
        if (target.isBlank()) return false

        // 1. Direct contains check
        if (spoken.contains(target) || target.contains(spoken)) {
            return true
        }

        // 2. Check all candidate alternatives
        for (candidate in allCandidates) {
            val cand = candidate.lowercase().trim()
            if (cand.contains(target) || target.contains(cand)) {
                return true
            }
        }

        // 3. Word set overlap match (e.g. "hey find my phone" vs "find my phone")
        val targetWords = target.split(" ").filter { it.isNotBlank() }.toSet()
        val spokenWords = spoken.split(" ").filter { it.isNotBlank() }.toSet()
        val intersection = targetWords.intersect(spokenWords)
        if (targetWords.isNotEmpty() && (intersection.size.toFloat() / targetWords.size.toFloat()) >= 0.75f) {
            return true
        }

        return false
    }
}
