package com.example.voiceadapt


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.Guideline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var intentTextView: TextView
    private lateinit var languageTextView: TextView
    private lateinit var recordButton: ToggleButton
    private var isListening = false  // this is for checking if we are listening
    private val handler = Handler(Looper.getMainLooper()) // for pausing
    private var isTTSReady = false  // Flag to track TTS readiness

    private val languageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedLanguage = result.data?.getStringExtra("selected_language")
            languageTextView.text = "Selected language: $selectedLanguage"

            //Initialise TTS after Language Selection
            initializeTTS()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialise the UI
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_first_screen))
        intentTextView = findViewById(R.id.intentView)
        recordButton = findViewById(R.id.startButton)
        languageTextView = findViewById(R.id.language_text_view)
        recordButton.visibility = Button.INVISIBLE

        // check the audio permission
        if (!hasAudioPermission()) {
            requestAudioPermission()
        }
        // linking and setting up the action of the button for language setup
        val btnLanguage = findViewById<Button>(R.id.btn_mothertongue)
        btnLanguage.setOnClickListener {
            recordButton.visibility = Button.VISIBLE
            val intent = Intent(this, LanguageActivity::class.java)
            languageResultLauncher.launch(intent)
        }

        // settings for SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                intentTextView.text = "Bereit zum Hören"
            }
            override fun onBeginningOfSpeech() {
                intentTextView.text = "\uD83E\uDDFF\uD83D\uDC40\uD83E\uDDFF Höre zu..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isListening = false

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio Fehler: Probleme bei der Audioaufnahme."
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        intentTextView.text = "Keine gültige Eingabe erkannt. Bitte nochmal versuchen!"
                        restartListeningAfterDelay()
                        return
                    }
                    SpeechRecognizer.ERROR_NETWORK -> "Netzwerk Fehler: Überprüfen Sie die Internetverbindung."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        intentTextView.text = "Sag etwas bitte!"
                        restartListeningAfterDelay()
                        return
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        restartListeningAfterDelay()
                        return
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        restartListeningAfterDelay()
                        return
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Berechtigungsfehler: Mikrofonzugriff verweigert."
                    else -> "Unbekantes Fehler"
                }

                runOnUiThread {
                    intentTextView.text = "Fehler: $errorMessage"
                }

            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: "Nicht erkannt"
                // answers processing
                processSpeechResponse(spokenText)

            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // pushing the recordButton will also go to the second screen
        recordButton.setOnClickListener {
            stopListening()
            stopTTS()
            try {
                val intent = Intent(this, ChoiceActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // initialising TextToSpeech
        textToSpeech = TextToSpeech(this, { status ->
            if(status == TextToSpeech.SUCCESS){
                textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener(){
                    override fun onDone(utteranceId: String?) {}
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                })
                textToSpeech?.language = Locale("de", "DE")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale("de", "DE")
            textToSpeech?.setSpeechRate(1.5f)
            isTTSReady = true // Mark TTS as ready
        } else {
            isTTSReady = false
            intentTextView.text = "TTS initialization failed."
        }
    }


    private fun startListening() {
        if (isListening || speechRecognizer == null || textToSpeech?.isSpeaking == true) {
            return
        }

        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE") // Setăm limba germană
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Jetzt bitte sprechen...")
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            intentTextView.text = "Fehler: Starten von Sprachbefehl fehlgeschlagen."
            e.printStackTrace()
            isListening = false
        }
    }

    private fun restartListeningAfterDelay() {
        coroutineScope.launch {
            delay(3000) // 3 seconds pause before start listening again
            startListening()
        }
    }


    private fun processSpeechResponse(spokenText: String) {
        when {
            spokenText.contains("nein", ignoreCase = true) || spokenText.contains("nö", ignoreCase = true) -> {
                stopListening()
                speakAndReset("Ohhh, Schade! Tschüss!")  // Replay the message and reset the application.
                return
            }

            spokenText.contains("spielen", ignoreCase = true)
                    || spokenText.contains("ich möchte spielen", ignoreCase = true)
                    || spokenText.contains("ja", ignoreCase = true) -> {
                stopListening()

                runOnUiThread { intentTextView.text = "" } // Clear the error message before transitioning

                speak("Super! Spielen wir!")

                coroutineScope.launch {
                    delay(500) // Allow the SpeechRecognizer to cleanly stop
                    try {
                        speechRecognizer?.destroy()
                        val intent = Intent(this@MainActivity, ChoiceActivity::class.java)
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return
            }

            else -> {
                speak("Bitte sag Ja oder Nein.")
                isListening = false
                restartListeningAfterDelay()
            }
        }
    }


    private fun speakAndReset(message: String) {
        // Complete shutdown of the SpeechRecognizer to avoid errors
        stopListening()
        speechRecognizer?.destroy()
        isListening = false  // Ensure that listening is disabled

        // Clear any displayed error messages
        runOnUiThread {
            intentTextView.text = ""  // Clear the text from the screen
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    // Reset the application to its initial state
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onError(utteranceId: String?) {}
        })
        // Play the voice message
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "reset_message")
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                intentTextView.text = "Permision RECORD_AUDIO refused"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        isTTSReady = false

    }

    companion object {
        private const val REQUEST_RECORD_PERMISSION = 1
    }
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            isListening = false
            Log.d("SpeechRecognizer", "Listening stopped and resources released.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SpeechRecognizer", "Error while stopping SpeechRecognizer: ${e.message}")
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("de", "DE")
                textToSpeech?.setSpeechRate(1.5f)
                isTTSReady = true // Marchez TTS ca fiind inițializat
                speakAfterTTSReady() // Continuă cu mesajele vocale
            } else {
                isTTSReady = false
                intentTextView.text = "TTS initialization failed."
            }
        }
    }

    private fun speakAfterTTSReady() {
        coroutineScope.launch {
                speak("Hey, ich bin Lingo, magst du mit mir spielen?")
                delay(3000)
                startListening()
        }
    }

    private fun stopTTS() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
    }

    private fun speak(text: String) {
        coroutineScope.launch {
            if (!isTTSReady) {
                intentTextView.text = "TTS is not ready. Please wait."
                return@launch
            }

            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            delay(100)
            while (textToSpeech?.isSpeaking == true) {
                delay(1000)
            }
        }
    }


}