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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.util.Locale

class NumbersActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var progressBar: ProgressBar
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private lateinit var nrZero: ImageView
    private lateinit var nrOne: ImageView
    private lateinit var nrTwo: ImageView
    private lateinit var nrThree: ImageView
    private lateinit var nrFour: ImageView
    private lateinit var nrFive: ImageView
    private lateinit var nrSix: ImageView
    private lateinit var nrSeven: ImageView
    private lateinit var nrEight: ImageView
    private lateinit var nrNine: ImageView
    private lateinit var nrTen: ImageView

    private val numberList = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
    private var currentNumberIndex = 0
    private var progressPercentage = 0
    private var isGameCompleted = false


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_numbers)

        progressBar = findViewById(R.id.progressBar)
        textToSpeech = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        val playButton = findViewById<Button>(R.id.playButton)

        playButton.setOnClickListener {
            stopTTS()
            stopSpeechRecognizer()

            if (isGameCompleted) {
                // If the game is complete, reset the level
                resetGameAndStart()
                isGameCompleted = false // Reset the game state
            } else {
                // If the game is not complete, start the presentation normally
                coroutineScope.launch {
                    presentNumber()
                }
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            navigateBack()
        }
        nrZero = findViewById(R.id.nrZero)
        nrOne = findViewById(R.id.nrOne)
        nrTwo = findViewById(R.id.nrTwo)
        nrThree = findViewById(R.id.nrThree)
        nrFour = findViewById(R.id.nrFour)
        nrFive = findViewById(R.id.nrFive)
        nrSix = findViewById(R.id.nrSix)
        nrSeven = findViewById(R.id.nrSeven)
        nrEight = findViewById(R.id.nrEight)
        nrNine = findViewById(R.id.nrNine)
        nrTen = findViewById(R.id.nrTen)

        hideAllNumberImages()
    }

    private fun stopSpeechRecognizer() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.GERMAN
            textToSpeech.setSpeechRate(2f)

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {

                    runOnUiThread {
                        if (utteranceId == "intro_message") {
                            startListeningForStartCommand()
                        } else if (utteranceId?.startsWith("number_instruction_") == true) {
                            val number = utteranceId.removePrefix("number_instruction_")
                            startListeningForNumber(number)
                        } else if (utteranceId == "replay_prompt") {
                            stopTTS()
                            startListeningForReplayOrBack()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {}
            })
            textToSpeech.speak(
                "Willkommen in der Welt der Zahlen! Wenn du spielen möchtest, sag 'Play'. Für zurück sag 'Zurück'.",
                TextToSpeech.QUEUE_ADD,
                null,
                "intro_message"
            )
        }
    }

    private fun startInitialMessage() {
        coroutineScope.launch {

            speakInGerman("Ich werde dir eine Zahl sagen, und du musst sie wiederholen.")
            delay(1500)
            presentNumber()
        }
    }

    private suspend fun presentNumber() {
        val number = numberList[currentNumberIndex]
        showNumberImage(number)
        speakInGerman("Das ist die Zahl ${getGermanNumber(number)}.")
        delay(1200)
        speakInEnglish("This is $number. Now repeat after me: $number.", "number_instruction_$number")
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

    private fun startListeningForNumber(expectedNumber: String) {
        stopTTS()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please repeat the number: $expectedNumber")

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
                        if(isGameCompleted){
                            askToReplayOrGoBack()
                        }else{
                            startListeningForNumber(numberList[currentNumberIndex])
                        }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0)?.lowercase(Locale.GERMAN) ?: ""
                val spokenTextEN = matches?.get(0)?.lowercase(Locale.ENGLISH) ?: ""

                when {
                    //  Start game (at the beginning or after resuming)
                    spokenText in listOf("play", "spiel", "spielen", "start", "nochmal", "wiederholen") -> {
                        showToastMessage("Spiel startet!")
                        resetGameAndStart()  //  reset and start game
                    }

                    // Return to the main menu
                    spokenText in listOf("zurück", "zurueck", "back") -> {
                        showToastMessage("Zurück zum Menü!")
                        navigateBack()
                    }

                    //  Check correct number
                    isNumberMatch(spokenTextEN, numberList[currentNumberIndex]) -> {
                        showToastMessage("Gut gemacht!")
                        nextNumber()
                    }

                    //  Feedback for wrong answer
                    else -> {
                        showToastMessage("Das war nicht korrekt. Versuche es nochmal.")
                        coroutineScope.launch {
                            delay(2000)
                            if(!isListening){
                                showToastMessage("Versuche es erneut!")
                                startListeningForNumber(numberList[currentNumberIndex]) // Relaunch listening()
                            }
                        }

                    }
                }
            }

        })
    }

    private fun resetGameAndStart() {
        isGameCompleted = false // Reset game state
        stopTTS()

        currentNumberIndex = 0
        progressPercentage = 0
        progressBar.progress = progressPercentage
        hideAllNumberImages()

        speechRecognizer.cancel()
        speechRecognizer.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        initSpeechRecognizer()

        coroutineScope.launch {
            delay(500)
            startInitialMessage()
        }
    }



    private fun isNumberMatch(spokenText: String, expectedNumber: String): Boolean {
        val numberVariations = mapOf(
            "zero" to listOf("zero", "sero", "ziero"),
            "one" to listOf("one", "wan", "won","uan"),
            "two" to listOf("du", "two", "too", "tu", "thu", "twu", "tzu", "do"),
            "three" to listOf("free","three", "thri", "tree"),
            "four" to listOf("four", "for", "foar", "fo", "vo", "vor"),
            "five" to listOf("five", "faiv", "fiv","aiv"),
            "six" to listOf("six", "siks", "sixx"),
            "seven" to listOf("seven", "sevn", "sewen"),
            "eight" to listOf("aid","eight", "ate", "eit", "age", "echt"),
            "nine" to listOf("nein","nine", "nain", "nin","ain"),
            "ten" to listOf("den","ten", "tenn", "tn","denn","then")
        )
        // Normalizează textul input
        val normalizedText = spokenText.trim().lowercase(Locale.ENGLISH)
        return numberVariations[expectedNumber]?.any { variation ->
            normalizedText.contains(variation)
        } ?: false
    }

    private fun nextNumber() {

        if (isGameCompleted) {
            // Stop TTS and replay game if the Play button was pressed
            stopTTS()
            resetGameAndStart()
            return
        }
        currentNumberIndex++

        progressPercentage = ((currentNumberIndex.toFloat() / numberList.size) * 100).toInt()
        progressBar.progress = progressPercentage

        // Mutăm vulpea
        moveFoxToNextNumber()

        if (currentNumberIndex < numberList.size) {
            coroutineScope.launch {
                delay(1000)
                presentNumber()
            }
        } else {
            isGameCompleted = true // Mark the game as completed
            coroutineScope.launch {
                delay(1500)
                if (!isGameCompleted) return@launch
                speakInGerman("Super! Du hast alle Zahlen richtig wiederholt!")
                delay(1500)
                askToReplayOrGoBack()
            }
        }
    }

    private fun askToReplayOrGoBack() {
        if (!isGameCompleted) return // If the game is not complete, do not display the message

        textToSpeech.speak(
            "Möchtest du dieses Spiel erneut spielen? Sag 'Play' zum Wiederholen oder 'Zurück' zum Menü.",
            TextToSpeech.QUEUE_ADD,
            null,
            "replay_prompt"
        )

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    when (utteranceId) {
                        "replay_prompt" -> {
                            //  Listen for Play or Zurück commands after the resume message
                            if (isGameCompleted) {
                                startListeningForReplayOrBack()
                            }
                        }
                        "intro_message" -> {
                            // At the beginning, listen for start commands
                            startListeningForStartCommand()
                        }
                        else -> {
                            // Handle other messages (such as those for numbers)
                            if (utteranceId?.startsWith("number_instruction_") == true) {
                                val number = utteranceId.removePrefix("number_instruction_")
                                startListeningForNumber(number)
                            }
                        }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sag 'Play' zum Wiederholen oder 'Zurück' zum Menü.")
        }

        isListening = true
        speechRecognizer.startListening(intent)
    }

    private fun hideAllNumberImages() {
        nrZero.visibility = ImageView.INVISIBLE
        nrOne.visibility = ImageView.INVISIBLE
        nrTwo.visibility = ImageView.INVISIBLE
        nrThree.visibility = ImageView.INVISIBLE
        nrFour.visibility = ImageView.INVISIBLE
        nrFive.visibility = ImageView.INVISIBLE
        nrSix.visibility = ImageView.INVISIBLE
        nrSeven.visibility = ImageView.INVISIBLE
        nrEight.visibility = ImageView.INVISIBLE
        nrNine.visibility = ImageView.INVISIBLE
        nrTen.visibility = ImageView.INVISIBLE
    }

    private fun showNumberImage(number: String) {
        when (number.lowercase()) {
            "zero" -> nrZero.visibility = ImageView.VISIBLE
            "one" -> nrOne.visibility = ImageView.VISIBLE
            "two" -> nrTwo.visibility = ImageView.VISIBLE
            "three" -> nrThree.visibility = ImageView.VISIBLE
            "four" -> nrFour.visibility = ImageView.VISIBLE
            "five" -> nrFive.visibility = ImageView.VISIBLE
            "six" -> nrSix.visibility = ImageView.VISIBLE
            "seven" -> nrSeven.visibility = ImageView.VISIBLE
            "eight" -> nrEight.visibility = ImageView.VISIBLE
            "nine" -> nrNine.visibility = ImageView.VISIBLE
            "ten" -> nrTen.visibility = ImageView.VISIBLE
        }
    }

    private fun getGermanNumber(number: String): String {
        return when (number) {
            "zero" -> "Null."
            "one" -> "Eins."
            "two" -> "Zwei."
            "three" -> "Drei."
            "four" -> "Vier."
            "five" -> "Fuenf."
            "six" -> "Sechs."
            "seven" -> "Sieben."
            "eight" -> "Acht."
            "nine" -> "Neun."
            "ten" -> "Zehn."
            else -> number
        }
    }

    private suspend fun speakInGerman(text: String) {
        textToSpeech.language = Locale.GERMAN
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "german_speech")
        while (textToSpeech.isSpeaking) {
            delay(700)
        }
    }

    private fun speakInEnglish(text: String, utteranceId: String) {
        textToSpeech.language = Locale.ENGLISH
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
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

    override fun onDestroy() {
        stopTTS()
        speechRecognizer.cancel()
        speechRecognizer.destroy()
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun moveFoxToNextNumber() {
        // Ascunde vulpea la început
        val fuxiePlay = findViewById<ImageView>(R.id.fuxiePlay)
        fuxiePlay.visibility = ImageView.VISIBLE

        // Poziționăm vulpea în funcție de numărul curent
        when (currentNumberIndex) {
            0 -> {
                // Vulpea lângă numărul 0
                val location = IntArray(2)
                nrZero.getLocationOnScreen(location)
                val x = location[0]
                val y = location[1]

                fuxiePlay.x = x.toFloat() + nrZero.width / 2 - fuxiePlay.width / 2
                fuxiePlay.y = y.toFloat() + nrZero.height
            }
            1 -> {
                // Vulpea lângă numărul 1
                fuxiePlay.x = nrOne.x + nrOne.width / 2
                fuxiePlay.y = nrOne.y + nrOne.height / 2
            }
            2 -> {
                // Vulpea lângă numărul 2
                fuxiePlay.x = nrTwo.x + nrTwo.width / 2
                fuxiePlay.y = nrTwo.y + nrTwo.height / 2
            }
            3 -> {
                // Vulpea lângă numărul 3
                fuxiePlay.x = nrThree.x + nrThree.width / 2
                fuxiePlay.y = nrThree.y + nrThree.height / 2
            }
            4 -> {
                // Vulpea lângă numărul 4
                fuxiePlay.x = nrFour.x + nrFour.width / 2
                fuxiePlay.y = nrFour.y + nrFour.height / 2
            }
            5 -> {
                // Vulpea lângă numărul 5
                fuxiePlay.x = nrFive.x + nrFive.width / 2
                fuxiePlay.y = nrFive.y + nrFive.height / 2
            }
            6 -> {
                // Vulpea lângă numărul 6
                fuxiePlay.x = nrSix.x + nrSix.width / 2
                fuxiePlay.y = nrSix.y + nrSix.height / 2
            }
            7 -> {
                // Vulpea lângă numărul 7
                fuxiePlay.x = nrSeven.x + nrSeven.width / 2
                fuxiePlay.y = nrSeven.y + nrSeven.height / 2
            }
            8 -> {
                // Vulpea lângă numărul 8
                fuxiePlay.x = nrEight.x + nrEight.width / 2
                fuxiePlay.y = nrEight.y + nrEight.height / 2
            }
            9 -> {
                // Vulpea lângă numărul 9
                fuxiePlay.x = nrNine.x + nrNine.width / 2
                fuxiePlay.y = nrNine.y + nrNine.height / 2
            }
            10 -> {
                // Vulpea lângă numărul 10
                fuxiePlay.x = nrTen.x + nrTen.width / 2
                fuxiePlay.y = nrTen.y + nrTen.height / 2
            }
        }
    }
}
