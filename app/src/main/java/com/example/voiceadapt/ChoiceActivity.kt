package com.example.voiceadapt


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.Locale
import android.speech.tts.UtteranceProgressListener
import android.util.Log

class ChoiceActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Declare coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Variabiles for TTS and SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var playButton: Button? = null
    private var playText: TextView? = null
    private var backButton: Button? = null
    private var backText: TextView? = null
    private var isListening = false  // Indicator for active listening

    //Variable for tracking navigation from LevelsActivity
    private var navigateFromLevelActivity = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)

        // Initialise TTS
        textToSpeech = TextToSpeech(this, this)

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        // Initialize buttons and text
        playButton = findViewById(R.id.playButton)
        playText = findViewById(R.id.playText)
        backButton = findViewById(R.id.backButton)
        backText = findViewById(R.id.backText)

        // Set actions for buttons
        playButton?.setOnClickListener {
            stopAllProcesses() // Stops everything before navigation
            navigateFromLevelActivity = true // Mark tap navigation
            goToLevelsActivity()
        }

        backButton?.setOnClickListener {
            stopAllProcesses() // Stops all processes before navigation
            navigateBack()
        }
    }

    private fun goToLevelsActivity() {
        val intent = Intent(this, LevelsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateBack() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.GERMAN
            textToSpeech.setSpeechRate(1.5f)

            // Listener for TTS
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    coroutineScope.launch {
                        when (utteranceId) {
                            "intro_message" -> updateTextColor(playText, R.color.happy_green)
                            "play_instruction" -> {
                                updateTextColor(playText, R.color.black)
                                updateTextColor(backText, R.color.happy_green)
                            }

                            "back_instruction" -> {
                                updateTextColor(backText, R.color.black)
                                // prompt message
                                speakPrompt()
                            }

                            "prompt_instruction" -> {
                                delay(1000) // Pause to avoid error 8
                                startListening()
                            }
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })

            startInitialMessages()
        }
    }

    private fun startInitialMessages() {
        coroutineScope.launch {
            speak("Diese sind die Commandos für den Spiel", "intro_message")
            speak(
                "Um das Spiel zu starten, drück auf den Play-Knopf oder sag 'Play'.",
                "play_instruction"
            )
            speak(
                "Um zurückzugehen, drück auf den Zurück-Knopf oder sag 'Zurück'.",
                "back_instruction"
            )
        }
    }

    private suspend fun speak(text: String, utteranceId: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        while (textToSpeech.isSpeaking) {
            delay(500) // Wait to stop talking
        }
    }

    private fun speakPrompt() {
        coroutineScope.launch {
            speak("Bitte sag 'Play' oder 'Zurück'", "prompt_instruction")
        }
    }

    private fun startListening() {

        // Stop TTS if active
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sag 'Play' oder 'Zurück'")
        }

        isListening = true
        speechRecognizer.startListening(intent)
    }

    private fun initSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio-Fehler: Probleme bei der Audioaufnahme."
                    SpeechRecognizer.ERROR_CLIENT -> "Client-Fehler: Interne Anwendungskommunikation fehlgeschlagen."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Berechtigungsfehler: Mikrofonzugriff verweigert."
                    SpeechRecognizer.ERROR_NETWORK -> "Netzwerkfehler: Keine Verbindung zum Server."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Netzwerk-Timeout: Server antwortet nicht."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Keine Übereinstimmung: Bitte wiederholen Sie Ihre Eingabe."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Erkennungsfehler: Das System ist derzeit ausgelastet."
                    SpeechRecognizer.ERROR_SERVER -> "Server-Fehler: Problem mit dem Erkennungsdienst."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Zeitüberschreitung: Keine Sprache erkannt."
                    else -> "Unbekannter Fehler: $error"
                }

                showToastMessage(errorMessage)

                coroutineScope.launch {
                    delay(1000)
                    if (isListening) startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.get(0)?.lowercase(Locale.GERMAN) ?: ""

                when {
                    command.contains("play") || command.contains("pley") || command.contains("blay") -> {navigateFromLevelActivity = true
                    goToLevelsActivity()}
                    command.contains("zurück") || command.contains("zuruck") -> navigateBack()
                    else -> startListening()
                }
            }


        })
    }

    private fun showToastMessage(message: String) {
        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAllProcesses() {
        pauseAllProcesses()
        coroutineScope.cancel()
    }

    private fun stopTTS() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    private fun updateTextColor(textView: TextView?, colorId: Int) {
        textView?.setTextColor(ContextCompat.getColor(this@ChoiceActivity, colorId))
    }

    override fun onPause() {
        super.onPause()
        pauseAllProcesses()
    }

    private fun pauseAllProcesses() {

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop() // Stop only the active speech without destroying the instance
        }
        // Suspend voice recognition
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false // Mark listening as inactive
        }
    }

   override fun onResume() {
        super.onResume()

        // Reactivate components without recreating them
        if (!::textToSpeech.isInitialized) {
            textToSpeech = TextToSpeech(this, this)
        }

        if (!::speechRecognizer.isInitialized) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            initSpeechRecognizer()
        }

        // Reset UI
        updateTextColor(playText, R.color.black)
        updateTextColor(backText, R.color.black)

       if (navigateFromLevelActivity) {
           navigateFromLevelActivity = false // Reset indicator
           startInitialMessages()
       } else if (!isListening && !textToSpeech.isSpeaking) {
           startInitialMessages()
       }

    }

    override fun onDestroy() {
        stopAllProcesses()
        super.onDestroy()
    }
}
