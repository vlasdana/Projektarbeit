package com.example.voiceadapt

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.RequiresApi
import android.util.Log

class ChoiceActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var playButton: Button? = null
    private var playText: TextView? = null
    private var backButton: Button? = null
    private var backText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

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

        playButton?.setOnClickListener {
            goToLevelsActivity()
        }

        backButton?.setOnClickListener {
            navigateBack()
        }
    }

    private fun goToLevelsActivity() {
        // Oprește TTS înainte de a trece la următoarea activitate
        if (tts.isSpeaking) {
            tts.stop()
        }
        val intent = Intent(this, LevelsActivity::class.java)
        startActivity(intent)

    }

    private fun navigateBack() {
        // Oprește TTS înainte de a reveni la activitatea anterioară
        if (tts.isSpeaking) {
            tts.stop()
        }
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Verifică limba germană
            if (tts.isLanguageAvailable(Locale.GERMAN) == TextToSpeech.LANG_MISSING_DATA ||
                tts.isLanguageAvailable(Locale.GERMAN) == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Deutche Sparache nicht erkannt!", Toast.LENGTH_LONG).show()
            }

            tts.language = Locale.GERMAN

            // Listener pentru TTS
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        when (utteranceId) {
                            "intro_message" -> {
                                playText?.setTextColor(
                                    ContextCompat.getColor(this@ChoiceActivity, R.color.happy_green)
                                )
                            }

                            "play_instruction" -> {
                                playText?.setTextColor(
                                    ContextCompat.getColor(this@ChoiceActivity, R.color.black)
                                )
                                backText?.setTextColor(
                                    ContextCompat.getColor(this@ChoiceActivity, R.color.happy_green)
                                )
                            }

                            "back_instruction" -> {
                                backText?.setTextColor(
                                    ContextCompat.getColor(this@ChoiceActivity, R.color.black)
                                )
                                // Adăugăm mesajul prompt
                                tts.speak(
                                    "Bitte sagen Sie 'Play' oder 'Zurück'",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "prompt_instruction"
                                )
                            }

                            "prompt_instruction" -> {
                                // După mesajul prompt, începem ascultarea
                                handler.postDelayed({
                                    startListening()
                                }, 1000) // Pauză mai mare pentru a evita eroarea 8
                            }
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })

            // Mesajele inițiale
            Thread {
                tts.speak(
                    "Hey, diese sind die Commandos für den Spiel",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "intro_message"
                )
                tts.speak(
                    "Um das Spiel zu starten, drücken Sie auf den Play-Knopf oder sagen Sie 'Play'.",
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "play_instruction"
                )
                tts.speak(
                    "Um zurückzugehen, drücken Sie auf den Zurück-Knopf oder sagen Sie 'Zurück'.",
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "back_instruction"
                )
            }.start()
        }
    }

    private fun startListening() {
        Log.d("SpeechRecognizer", "Zuhören startet...")

        // Oprim TTS înainte de ascultare
        if (tts.isSpeaking) {
            Log.d("SpeechRecognizer", "TTS noch aktiv! Zwangss Gestoppt.")
            tts.stop()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bitte sagen Sie 'Play' oder 'Zurück'")
        }

        handler.postDelayed({
            speechRecognizer.startListening(intent)
        }, 1000) // Pauză mai mare pentru a asigura eliberarea microfonului
    }

    private fun initSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Kein match. Versuchen Sie erneut!"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Keine Stimme erkannt!"
                    else -> "Fehler unerkannt! Cod: $error"
                }
                Toast.makeText(this@ChoiceActivity, message, Toast.LENGTH_SHORT).show()

                handler.postDelayed({
                    startListening()
                }, 1000)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val command = matches[0].lowercase(Locale.GERMAN)
                    if (command.contains("play")) {
                        goToLevelsActivity()
                    } else if (command.contains("zurück") || command.contains("zuruck")) {
                        navigateBack()
                    } else {
                        Toast.makeText(this@ChoiceActivity, "Ungültige Auswahl!", Toast.LENGTH_SHORT).show()
                        startListening()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onDestroy() {
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
