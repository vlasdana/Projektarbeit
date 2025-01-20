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


    private var ttsCallback: (() -> Unit)? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var intentTextView: TextView
    private lateinit var languageTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var errorGuideline: Guideline
    private lateinit var recordButton: ToggleButton
    private var isListening = false  // this is for checking if we are listening
    private val handler = Handler(Looper.getMainLooper()) // for pausing

    private val languageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedLanguage = result.data?.getStringExtra("selected_language")
            languageTextView.text = "Selected language: $selectedLanguage"
            //spune mesajul de bun-venit
            speak("Hey,ich bin Lingo, magst du mit mir spielen?")
            // introducem secunde de intarziere sa evitam coliziunea intre tts si speechul nostru
            handler.postDelayed({ startListening() }, 3000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialise the UI
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_first_screen))
        intentTextView = findViewById(R.id.intentView)
        errorTextView = findViewById(R.id.errorView)
        errorGuideline = findViewById(R.id.errorGuideLine)
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
                intentTextView.text = "🔴🟠🟡🟢 Höre zu..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                intentTextView.text = "🧿👀🧿Zuhören beendet"
            }
            override fun onError(error: Int) {
                val errorMessage = getErrorDescription(error)
                intentTextView.text = "Fehler: $errorMessage}"
                // we stop the listening on frequent errors to avoid loops
                if(error == SpeechRecognizer.ERROR_NO_MATCH ) {
                    stopListening()
                    intentTextView.text = "Keine gültige Eingabe erkannt. Bitte nochmal versuchen!"
                } else if(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
                    intentTextView.text = "Sag etwas bitte!"
                    handler.postDelayed({startListening()}, 3000)
                }else if (isListening) {
                    handler.postDelayed({startListening()}, 3000)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: "Nicht erkannt"
                intentTextView.text = "Du hast gesagt: $spokenText"

                // answers processing
                processSpeechResponse(spokenText)

            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // pushing the recordButton will also go to the second screen
        recordButton.setOnClickListener {
            stopListening() // stop listeningg
            stopTTS()        // Oprire imediată a TTS

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
                    override fun onDone(utteranceId: String?) {
                        handler.post{
                            ttsCallback?.invoke()
                            ttsCallback = null
                        }
                    }
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                })
                textToSpeech?.language = Locale("de", "DE")
            }
        })
    }


    private fun startListening() {
        stopListening()
        if(isListening) return

        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE") // Set german language
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Jetzt bitte sprechen...")
        }
        handler.post{
            speechRecognizer?.startListening(intent)
        }
    }

    private fun processSpeechResponse(spokenText: String) {
        val response = when {
            // Dacă utilizatorul spune "nein" sau "nö"
            spokenText.contains("nein", ignoreCase = true) || spokenText.contains("nö", ignoreCase = true) -> {
                stopListening()
                speakAndReset("Ohhh, Schade! Tschüss!")  // Redă mesajul și resetează aplicația
                return
            }

            // Dacă utilizatorul spune "spielen" sau "ja"
            spokenText.contains("spielen", ignoreCase = true)
                    || spokenText.contains("ich möchte spielen", ignoreCase = true)
                    || spokenText.contains("ja", ignoreCase = true) -> {
                stopListening()
                speak("Super! Spielen wir!")
                try {
                    speechRecognizer?.destroy()
                    val intent = Intent(this, ChoiceActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }

            else -> {
                speak("Bitte sag Ja oder Nein.")
                isListening = true
                return
            }
        }

        // Afișează răspunsul pe ecran
        intentTextView.text = response

        // Reluăm ascultarea dacă este necesar
        if (isListening) {
            handler.postDelayed({ startListening() }, 3000)
        }
    }

    private fun speakAndReset(message: String) {
        // Oprirea completă a SpeechRecognizer pentru a evita erorile
        stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        isListening = false  // Asigurăm că ascultarea este dezactivată

        // Ștergem eventualele mesaje de eroare afișate
        runOnUiThread {
            intentTextView.text = ""  // Curățăm textul de pe ecran
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    // Resetăm aplicația la starea inițială
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onError(utteranceId: String?) {}
        })

        // Redăm mesajul vocal
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "reset_message")
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
                intentTextView.text = "Permision RECORD_AUDIO refused"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
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
            else -> "Unbekannte Fehler!"
        }
    }
    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        intentTextView.text = "Zuhören ist beendet."
    }

    private fun stopTTS() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()  // Oprire instantanee a TTS
            textToSpeech?.shutdown()  // Eliberează resursele
            textToSpeech = null
        }
    }

    private fun speak(text: String) {
        coroutineScope.launch {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            delay(100)
            while(textToSpeech?.isSpeaking == true){
                delay(1000)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale("de", "DE")
            textToSpeech?.setSpeechRate(1.5f)
        }
    }
}