package com.example.voiceadapt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class LevelsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Initiate coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Variabiles for TTS and SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var playText: TextView? = null
    private var backText: TextView? = null
    private var isListening = false  // Indicator pentru ascultare activă

    // Variable for tracking navigation from ColorsActivity or NumbersActivity
    private var navigateFromSubActivity = false


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        // Initialize TTS
        textToSpeech = TextToSpeech(this, this)

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        // Play Button for Colors
        val colorsPlayButton: ImageButton = findViewById(R.id.colorsPlayButton)
        colorsPlayButton.setOnClickListener {
            stopAllProcesses()
            navigateFromSubActivity = true // Set the indicator for returning
            val intent = Intent(this, ColorsActivity::class.java)
            startActivity(intent)

        }

        // Play Button for Numbers
        val numbersPlayButton: ImageButton = findViewById(R.id.numbersPlayButton)
        numbersPlayButton.setOnClickListener {
            stopAllProcesses()
            navigateFromSubActivity = true
            val intent = Intent(this, NumbersActivity::class.java)
            startActivity(intent)
        }

        // Add tap functionality to the back button
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, ChoiceActivity::class.java)
            startActivity(intent)
             finish() // Closes the LevelsActivity and returns to the previous one
        }

    }


    private fun goToColorsActivity() {
        pauseAllProcesses()

// Set the indicator that we are coming from a sub-activity (ColorsActivity)
        navigateFromSubActivity = true
        val intent = Intent(this, ColorsActivity::class.java)
        startActivity(intent)

    }

    private fun goToNumbersActivity() {

        pauseAllProcesses()

// Set the indicator that we are coming from a sub-activity (NumbersActivity)
        navigateFromSubActivity = true
        val intent = Intent(this, NumbersActivity::class.java)
        startActivity(intent)

    }

    private fun navigateBack() {
        stopAllProcesses() // Stop all active processes
        val intent = Intent(this, ChoiceActivity::class.java)
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
                                // Bring prompt message
                                speakPrompt()
                            }

                            "prompt_instruction" -> {
                                delay(1000) // Pause to avoid Error 8
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
            speak("Super!", "intro_message")
            speak(
                "Was möchtest du spielen? Sag einfach „Farben“ oder „Zahlen“. Oder drück auf den Play-Knopf.",
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
            speak("Bitte sag 'Farben', 'Zahlen', oder 'Zurück'", "prompt_instruction")
        }
    }

    private fun startListening() {
        Log.d("SpeechRecognizer", "Zuhören startet...")

        // Stop TTS if active
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sag 'Farben', 'Zahlen', oder 'Zurück'")
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
                    command.contains("farben") -> {navigateFromSubActivity = true
                        goToColorsActivity()}
                    command.contains("zahlen") -> {navigateFromSubActivity = true
                        goToNumbersActivity()}
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

        pauseAllProcesses() // Put on Pause the active processing
        coroutineScope.cancel() // Stop all active coroutines

        // Complete destruction of TTS
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown() // Release TTS resources
        }

        // Complete destruction of SpeechRecognizer
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.cancel()
            speechRecognizer.destroy() // Release SpeechRecognizer resources
        }

        isListening = false // Mark listening as inactive

    }

    private fun stopTTS() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    private fun stopSpeechRecognizer() {
        speechRecognizer.cancel()
        speechRecognizer.destroy()
    }

    private fun updateTextColor(textView: TextView?, colorId: Int) {
        textView?.setTextColor(ContextCompat.getColor(this@LevelsActivity, colorId))
    }

    override fun onPause() {
        super.onPause()
        pauseAllProcesses()
    }

    private fun pauseAllProcesses() {
        // Pause the TTS if it is speaking
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

        if (navigateFromSubActivity) {
            navigateFromSubActivity = false // Reset indicator
            startInitialMessages()
        }else if(!isListening && !textToSpeech.isSpeaking){
            startInitialMessages()
        }
    }

    override fun onDestroy() {
        stopAllProcesses()
        super.onDestroy()
    }
}
