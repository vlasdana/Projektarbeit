package com.example.voiceadapt


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.Activity
import android.content.Intent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.Guideline
import java.util.Locale


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var intentTextView: TextView
    private lateinit var languageTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var errorGuideline: Guideline
    private lateinit var recordButton: ToggleButton
    private var isListening = false  // Variabilă pentru a verifica dacă ascultăm sau nu
    private val languageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedLanguage = result.data?.getStringExtra("selected_language")
            languageTextView.text = "Selected language: $selectedLanguage"
            speak("Hallo, ich bin Lingo. Magst du mit mir spielen? Wenn ja, sag play")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inițializare UI
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_first_screen))
        intentTextView = findViewById(R.id.intentView)
        errorTextView = findViewById(R.id.errorView)
        errorGuideline = findViewById(R.id.errorGuideLine)
        recordButton = findViewById(R.id.startButton)
        languageTextView = findViewById(R.id.language_text_view)
        // Verifică permisiunile pentru audio
        if (!hasAudioPermission()) {
            requestAudioPermission()
        }
        // Buton pentru alegerea limbii
        val btnLanguage = findViewById<Button>(R.id.btn_mothertongue)
        btnLanguage.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            languageResultLauncher.launch(intent)
        }

        // Configurare SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                intentTextView.text = "Bereit zum Hören"
            }

            override fun onBeginningOfSpeech() {
                intentTextView.text = "Höre zu..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {

                if (isListening) {
                    intentTextView.text = "Sprache beendet"
                    startListening()  // Continuăm ascultarea dacă este activă
                } else {
                    intentTextView.text = "Zuhören beendet"
                }
            }

            override fun onError(error: Int) {
                intentTextView.text = "Eroare: ${getErrorDescription(error)}"
                // În caz de eroare, încearcă să repornești recunoașterea vocală dacă ascultarea nu a fost oprită
                if (isListening) {
                    startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: "Nicht erkannt"
                intentTextView.text = "Du hast gesagt: $spokenText"

                // Procesare răspunsuri predefinite
                processSpeechResponse(spokenText)

             //         val intent = Intent(this@MainActivity, ChoiceActivity::class.java)
             //           startActivity(intent)

            }


            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Setare comportament pentru butonul de înregistrare
        recordButton.setOnClickListener {
            if(isListening == false){if (hasRecordPermission()) {
                startListening()
                isListening = true
                intentTextView.text = "Ich höre zu"
            } else {
                requestRecordPermission()
            }
        }else{isListening = false
                stopListening()
            }
        }
        // Inițializare TextToSpeech
        textToSpeech = TextToSpeech(this, this)
    }

    private fun startListening() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-De") // Setează limba germana
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Jetzt bitte sprechen...")
        }
        speechRecognizer?.startListening(intent)

    }

    private fun processSpeechResponse(spokenText: String) {
        val response = when {
            spokenText.contains("hallo", ignoreCase = true) -> "Hallo! Magst du mit mir spielen?"

            spokenText.contains("stop", ignoreCase = true) -> {
                // Dacă se spune "stop", oprim ascultarea
                stopListening()
                "Zuhören ist gestoppt" // Răspunsul care va fi spus
            }
            else -> "Bitte wiederholen!"
        }

        // Afișează răspunsul text pe ecran
        intentTextView.text = response

        // Răspunde verbal prin TextToSpeech
        speak(response)
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                intentTextView.text = "Permisiune RECORD_AUDIO refuzată"
                println("Permisiune RECORD_AUDIO refuzată")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
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

    private fun getErrorDescription(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio Fehler"
            SpeechRecognizer.ERROR_NO_MATCH -> "Kein Ereignis"
            SpeechRecognizer.ERROR_NETWORK -> "Netzwerk Fehler"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Zeit überschritten"
            else -> "Eroare necunoscută"
        }
    }
    private fun stopListening() {
        // Oprirea recunoașterii vocale
        speechRecognizer?.stopListening()
        isListening = false
        intentTextView.text = "Zuhören ist beendet."
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale("de", "DE")
        }
    }
}
