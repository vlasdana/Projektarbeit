package com.example.voiceadapt


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.util.Locale

class ColorsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var progressBar: ProgressBar
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private lateinit var redBall: ImageView
    private lateinit var greenBall: ImageView
    private lateinit var blueBall: ImageView
    private lateinit var yellowBall: ImageView
    private lateinit var pinkBall: ImageView
    private lateinit var violetBall: ImageView
    private lateinit var orangeBall: ImageView
    private lateinit var brownBall: ImageView
    private lateinit var blackBall: ImageView
    private lateinit var whiteBall: ImageView

    private val colorList = listOf("red", "green", "blue", "yellow","pink","violet","orange","brown","black","white")
    private var currentColorIndex = 0
    private var progressPercentage = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_colors)

        checkAudioPermission()

        progressBar = findViewById(R.id.progressBar)
        textToSpeech = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            coroutineScope.launch {
                presentColor()
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            navigateBack()
        }

        redBall = findViewById(R.id.redBall)
        greenBall = findViewById(R.id.greenBall)
        blueBall = findViewById(R.id.blueBall)
        yellowBall = findViewById(R.id.yellowBall)
        pinkBall = findViewById(R.id.pinkBall)
        violetBall = findViewById(R.id.violetBall)
        orangeBall = findViewById(R.id.orangeBall)
        brownBall = findViewById(R.id.brownBall)
        blackBall = findViewById(R.id.blackBall)
        whiteBall = findViewById(R.id.whiteBall)

        hideAllColorDots()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.GERMAN

            // Setăm listener-ul pentru TTS
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        if (utteranceId == "intro_message") {
                            startListeningForStartCommand()
                        } else if (utteranceId?.startsWith("color_instruction_") == true) {
                            val color = utteranceId.removePrefix("color_instruction_")
                            startListeningForColor(color)
                        }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
            textToSpeech.speak(
                "Willkommen in der Welt der Farben! Wenn du spielen möchtest, sage 'Play'. Für zurück sage 'Zurück'.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "intro_message"
            )
        }
    }

    private fun startInitialMessage() {
        coroutineScope.launch {

            speakInGerman("Ich werde dir eine Farbe sagen, und du musst sie wiederholen.")
            delay(1500)
            presentColor()
        }
    }

    private suspend fun presentColor() {
        val color = colorList[currentColorIndex]
        showColorDot(color)
        speakInGerman("Das ist die Farbe ${getGermanColor(color)}.")
        delay(1400)
        speakInEnglish("This is $color. Now repeat after me: $color.", "color_instruction_$color")

    }
    private fun startListeningForStartCommand() {
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sag 'Play' zum Starten oder 'Zurück' um zurückzugehen.")
        }

        isListening = true
        speechRecognizer.startListening(intent)
    }

    private fun startListeningForColor(expectedColor: String) {
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please repeat the color: $expectedColor")
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
                showToastMessage("Fehler: Bitte versuche es erneut.")
                coroutineScope.launch {
                    delay(1000)
                    if (isListening) startListeningForColor(colorList[currentColorIndex])
                }
            }


            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0)?.lowercase(Locale.GERMAN) ?: ""

                when {
                    //  Start joc (la început sau după reluare)
                    spokenText in listOf("play", "spiel", "spielen", "start", "nochmal", "wiederholen") -> {
                        showToastMessage("Spiel startet!")
                        resetGameAndStart()  //  Reset complet și pornire joc
                    }

                    //  Revenire la meniul principal
                    spokenText in listOf("zurück", "zurueck", "back") -> {
                        showToastMessage("Zurück zum Menü!")
                        navigateBack()
                    }

                    //  Verificare culoare corectă
                    isColorMatch(spokenText, colorList[currentColorIndex]) -> {
                        showToastMessage("Gut gemacht!")
                        nextColor()
                    }

                    //  Feedback pentru răspuns greșit
                    else -> {
                        showToastMessage("Das war nicht korrekt. Versuche es nochmal.")
                        startListeningForColor(colorList[currentColorIndex])
                    }
                }
            }


        })
    }

    private fun resetGameAndStart() {
        currentColorIndex = 0
        progressPercentage = 0
        progressBar.progress = progressPercentage
        hideAllColorDots()

        speechRecognizer.cancel()
        speechRecognizer.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        coroutineScope.launch {
            delay(500)
            startInitialMessage()
        }
    }


    //  Compară pronunția utilizatorului cu variațiile acceptate
    private fun isColorMatch(spokenText: String, expectedColor: String): Boolean {
        val colorVariations = mapOf(
            "red" to listOf("red", "redd", "rad", "wred"),
            "green" to listOf("green", "grin", "gren"),
            "blue" to listOf("blue", "blu", "bluu", "blou"),
            "yellow" to listOf("yellow", "yello", "yelow"),
            "pink" to listOf("pink", "pynk", "ping"),
            "violet" to listOf("violet", "violett", "vyolet","vailet"),
            "orange" to listOf("orange", "oranj", "orenge"),
            "brown" to listOf("brown", "broun", "braun", "breun"),
            "black" to listOf("black", "blak", "bleck"),
            "white" to listOf("white", "whait", "whyte","uait")
        )

        // Verificăm dacă textul rostit este o variație a culorii
        return colorVariations[expectedColor]?.any { variation ->
            spokenText.contains(variation)
        } ?: false
    }


    private fun nextColor() {
        currentColorIndex++

        // Actualizare progres
        progressPercentage = ((currentColorIndex.toFloat() / colorList.size) * 100).toInt()
        progressBar.progress = progressPercentage

        if (currentColorIndex < colorList.size) {
            coroutineScope.launch {
                delay(1000)
                presentColor()
            }
        } else {
            coroutineScope.launch {
                delay(1500)
                speakInGerman("Super! Du hast alle Farben richtig wiederholt!")
                delay(1500)
                askToReplayOrGoBack()
            }
        }
    }



    private fun askToReplayOrGoBack() {
        // Mesaj TTS pentru utilizator
        textToSpeech.speak(
            "Möchtest du dieses Spiel erneut spielen? Sage 'Play' zum Wiederholen oder 'Zurück' zum Menü.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "replay_prompt"
        )

        // După ce mesajul vocal s-a încheiat, începe ascultarea răspunsului
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    if (utteranceId?.startsWith("color_instruction_") == true) {
                        val color = utteranceId.removePrefix("color_instruction_")
                        startListeningForColor(color)  //  Pornește ascultarea
                    } else if (utteranceId == "intro_message") {
                        startListeningForStartCommand()  //  La început, ascultă comenzile Play/Zürück
                    } else if (utteranceId == "replay_prompt") {
                        startListeningForReplayOrBack()  //  După final, ascultă pentru reluare
                    }
                }
            }
            override fun onError(utteranceId: String?) {}
        })
    }

    private fun startListeningForReplayOrBack() {
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sage 'Play' zum Wiederholen oder 'Zurück' zum Menü.")
        }

        isListening = true
        speechRecognizer.startListening(intent)
    }

    private fun hideAllColorDots() {
        redBall.visibility = ImageView.INVISIBLE
        greenBall.visibility = ImageView.INVISIBLE
        blueBall.visibility = ImageView.INVISIBLE
        yellowBall.visibility = ImageView.INVISIBLE
        pinkBall.visibility = ImageView.INVISIBLE
        violetBall.visibility = ImageView.INVISIBLE
        orangeBall.visibility = ImageView.INVISIBLE
        brownBall.visibility = ImageView.INVISIBLE
        blackBall.visibility = ImageView.INVISIBLE
        whiteBall.visibility = ImageView.INVISIBLE
    }

    private fun showColorDot(color: String) {
        when (color.lowercase()) {
            "red" -> redBall.visibility = ImageView.VISIBLE
            "green" -> greenBall.visibility = ImageView.VISIBLE
            "blue" -> blueBall.visibility = ImageView.VISIBLE
            "yellow" -> yellowBall.visibility = ImageView.VISIBLE
            "pink" -> pinkBall.visibility = ImageView.VISIBLE
            "violet" -> violetBall.visibility = ImageView.VISIBLE
            "orange" -> orangeBall.visibility = ImageView.VISIBLE
            "brown" -> brownBall.visibility = ImageView.VISIBLE
            "black" -> blackBall.visibility = ImageView.VISIBLE
            "white" -> whiteBall.visibility = ImageView.VISIBLE
        }
    }

    private fun getGermanColor(color: String): String {
        return when (color) {
            "red" -> "Rot"
            "green" -> "Grün"
            "blue" -> "Blau"
            "yellow" -> "Gelb"
            "pink" -> "Rosa"
            "violet" -> "Lila"
            "orange" -> "Orange"
            "brown" -> "Braun"
            "black" -> "Schwarz"
            "white" -> "Weiß"
            else -> color
        }
    }

    private suspend fun speakInGerman(text: String) {
        textToSpeech.language = Locale.GERMAN
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "german_speech")
        while (textToSpeech.isSpeaking) {
            delay(500)
        }
    }

    private fun speakInEnglish(text: String, utteranceId: String) {
        textToSpeech.language = Locale.ENGLISH
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun navigateBack() {
        val intent = Intent(this, LevelsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun stopTTS() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
    }

    private fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onDestroy() {
        stopTTS()
        speechRecognizer.cancel()
        speechRecognizer.destroy()
        coroutineScope.cancel()
        super.onDestroy()
    }
}
