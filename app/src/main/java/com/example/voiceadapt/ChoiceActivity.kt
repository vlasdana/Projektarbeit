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

    // Gestionare coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Variabile pentru TTS și SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var playButton: Button? = null
    private var playText: TextView? = null
    private var backButton: Button? = null
    private var backText: TextView? = null
    private var isListening = false  // Indicator pentru ascultare activă

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)

        // Verifică permisiunile audio
        checkAudioPermission()

        // Inițializăm TTS
        tts = TextToSpeech(this, this)

        // Inițializăm Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        // Inițializăm butoanele și textele
        playButton = findViewById(R.id.playButton)
        playText = findViewById(R.id.playText)
        backButton = findViewById(R.id.backButton)
        backText = findViewById(R.id.backText)

        // Setăm acțiunile pentru butoane
        playButton?.setOnClickListener {
            stopAllProcesses() // Oprește totul înainte de a naviga
            goToLevelsActivity()
        }

        backButton?.setOnClickListener {
            stopAllProcesses() // Oprește totul înainte de a naviga
            navigateBack()
        }
    }

    private fun goToLevelsActivity() {
        val intent = Intent(this, LevelsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateBack() {
        val intent = Intent(this, MainActivity::class.java)
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
            speak("Hey, diese sind die Commandos für den Spiel", "intro_message")
            speak(
                "Um das Spiel zu starten, drücken Sie auf den Play-Knopf oder sagen Sie 'Play'.",
                "play_instruction"
            )
            speak(
                "Um zurückzugehen, drücken Sie auf den Zurück-Knopf oder sagen Sie 'Zurück'.",
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
            speak("Bitte sagen Sie 'Play' oder 'Zurück'", "prompt_instruction")
        }
    }

    private fun startListening() {
        Log.d("SpeechRecognizer", "Zuhören startet...")

        // Oprim TTS dacă e activ
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sagen Sie 'Play' oder 'Zurück'")
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

            override fun onError(error: Int) {
                Toast.makeText(this@ChoiceActivity, "Eroare: $error", Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    delay(1000)
                    if (isListening) startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.get(0)?.lowercase(Locale.GERMAN) ?: ""

                when {
                    command.contains("play") -> goToLevelsActivity()
                    command.contains("zurück") || command.contains("zuruck") -> navigateBack()
                    else -> startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun stopAllProcesses() {
        stopTTS()
        stopSpeechRecognizer()
        coroutineScope.cancel()
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
        textView?.setTextColor(ContextCompat.getColor(this@ChoiceActivity, colorId))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onPause() {
        stopAllProcesses()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Resetare completă la revenire
        updateTextColor(playText, R.color.black)
        updateTextColor(backText, R.color.black)
       // startInitialMessages()
    }

    override fun onDestroy() {
        stopAllProcesses()
        super.onDestroy()
    }
}
