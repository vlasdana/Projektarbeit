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

    // Gestionare coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Variabile pentru TTS și SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var playText: TextView? = null
    private var backText: TextView? = null
    private var isListening = false  // Indicator pentru ascultare activă

    // Variabilă pentru urmărirea navigării din ColorsActivity sau NumbersActivity
    private var navigateFromSubActivity = false


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        // Verifică permisiunile audio
        checkAudioPermission()

        // Inițializăm TTS
        tts = TextToSpeech(this, this)

        // Inițializăm Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        // Play Button for Farben
        val colorsPlayButton: ImageButton = findViewById(R.id.colorsPlayButton)
        colorsPlayButton.setOnClickListener {
            stopAllProcesses()
            navigateFromSubActivity = true // Setăm indicatorul pentru revenire
            val intent = Intent(this, ColorsActivity::class.java)
            startActivity(intent)

        }

        // Play Button for Zahlen
        val numbersPlayButton: ImageButton = findViewById(R.id.numbersPlayButton)
        numbersPlayButton.setOnClickListener {
            stopAllProcesses()
            navigateFromSubActivity = true
            val intent = Intent(this, NumbersActivity::class.java)
            startActivity(intent)
        }

        // Back-Button finden und Tap-Funktionalität hinzufügen
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, ChoiceActivity::class.java)
            startActivity(intent)
             finish() // Schließt die LevelsActivity und kehrt zur vorherigen zurück
        }

    }


    private fun goToColorsActivity() {
       // stopAllProcesses() // Oprește toate procesele active
        pauseAllProcesses()

// Setăm indicatorul că venim dintr-o subactivitate
        navigateFromSubActivity = true
        val intent = Intent(this, ColorsActivity::class.java)
        startActivity(intent)

    }

    private fun goToNumbersActivity() {
       // stopAllProcesses() // Oprește toate procesele active
        pauseAllProcesses()

// Setăm indicatorul că venim dintr-o subactivitate
        navigateFromSubActivity = true
        val intent = Intent(this, NumbersActivity::class.java)
        startActivity(intent)

    }

    private fun navigateBack() {
        stopAllProcesses() // Oprește toate procesele active
        val intent = Intent(this, ChoiceActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.GERMAN

            // Listener pentru TTS
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
                                // Adăugăm mesajul prompt
                                speakPrompt()
                            }

                            "prompt_instruction" -> {
                                delay(1000) // Pauză pentru a evita eroarea 8
                                startListening()
                            }
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })

            // Mesaje inițiale
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
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        while (tts.isSpeaking) {
            delay(500) // Așteptăm să termine de vorbit
        }
    }

    private fun speakPrompt() {
        coroutineScope.launch {
            speak("Bitte sag 'Farben', 'Zahlen', oder 'Zurück'", "prompt_instruction")
        }
    }

    private fun startListening() {
        Log.d("SpeechRecognizer", "Zuhören startet...")

        // Oprim TTS dacă e activ
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sagen Sie 'Farben', 'Zahlen', oder 'Zurück'")
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

                //Toast.makeText(this@ChoiceActivity, errorMessage, Toast.LENGTH_LONG).show()
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
        // stopTTS()
        // stopSpeechRecognizer()
       /* pauseAllProcesses()
        coroutineScope.cancel() */
        pauseAllProcesses() // Pune pe pauză procesarea activă

        coroutineScope.cancel() // Oprește toate coroutines-urile active

        // Distrugere completă pentru TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown() // Eliberare resurse TTS
        }

        // Distrugere completă pentru SpeechRecognizer
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.cancel()
            speechRecognizer.destroy() // Eliberare resurse SpeechRecognizer
        }

        isListening = false // Marcam ascultarea ca inactivă

    }

    private fun stopTTS() {
        if (tts.isSpeaking) {
            tts.stop()
        }
    }

    private fun stopSpeechRecognizer() {
        speechRecognizer.cancel()
        speechRecognizer.destroy()
    }

    private fun updateTextColor(textView: TextView?, colorId: Int) {
        textView?.setTextColor(ContextCompat.getColor(this@LevelsActivity, colorId))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onPause() {
        super.onPause()
        pauseAllProcesses()
    }

    private fun pauseAllProcesses() {
        // Punem pe pauză TTS dacă vorbește
        if (tts.isSpeaking) {
            tts.stop() // Oprește doar vorbirea activă, fără să distrugă instanța
        }
        // Suspendăm recunoașterea vocală
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false // Marcam ascultarea ca inactivă
        }
    }

    override fun onResume() {
        super.onResume()

        // Reactivăm componentele fără a le recrea
        if (!::tts.isInitialized) {
            tts = TextToSpeech(this, this)
        }

        if (!::speechRecognizer.isInitialized) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            initSpeechRecognizer()
        }

        // Resetăm UI-ul
        updateTextColor(playText, R.color.black)
        updateTextColor(backText, R.color.black)

        /*  // Reluăm ascultarea și mesajele doar dacă nu sunt deja active
          if (!isListening && !tts.isSpeaking) {
              startInitialMessages()
          }*/

        // Reluăm mesajele și ascultarea doar dacă:
        // 1. Venim din LevelsActivity
        // 2. Procesele nu sunt deja active
        /* if (navigateFromLevelActivity || (!isListening && !tts.isSpeaking)) {
             navigateFromLevelActivity = false
             startInitialMessages()
         }*/
        if (navigateFromSubActivity) {
            navigateFromSubActivity = false // Resetăm indicatorul
            startInitialMessages()
        }else if(!isListening && !tts.isSpeaking){
            startInitialMessages()
        }
    }

    override fun onDestroy() {
        stopAllProcesses()
        super.onDestroy()
    }
}
