package com.example.voiceadapt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ai.picovoice.picovoice.*
import android.media.MediaPlayer

import android.view.MotionEvent
import android.widget.ImageView


class SecondActivity : AppCompatActivity() {
    /*
    I define the Wake Word text, contextName, contextInformation an fill in my correct
    Picovoice Access_key
     */
   /* private val ACCESS_KEY = "6iO+b3DnXa7KChT8r/3liZIuqurWDJzfOaCI2sie0qO5g59rheOsQA=="
    private var wakeWordName = "Hello World"
    private var contextName = "Ciprian2"
    private var contextInformation = ""*/

    private val ACCESS_KEY = "0kXDK8vwhmMSSIxdO2PqBDDItFYmNzmXlhH/dNbXXIUU8MCazREmCw=="
    private var wakeWordName = "play game" // Replace with your actual wake word name
    private var contextName = "lingoen" // Replace with your actual context name
    private var contextInformation = ""

    // We create a PicoVoiceManager object
    private var picovoiceManager: PicovoiceManager? = null
    private lateinit var intentTextView: TextView
    private val countDownTimer = object : CountDownTimer(2000, 1000) {
        override fun onTick(l: Long) {}

        override fun onFinish() {
            intentTextView.text = "\n    Listening ...\n"
        }
    }
    private val picovoiceWakeWordCallback = PicovoiceWakeWordCallback {
        runOnUiThread {
            countDownTimer.cancel()
            intentTextView.text = "\n    Wake Word Detected 👀🧿   \n"
        }
    }
    private val picovoiceInferenceCallback = PicovoiceInferenceCallback { inference ->
        runOnUiThread {
            intentTextView.text = "\n    {\n"
            intentTextView.append("        \"isUnderstood\" : \"${inference.isUnderstood}\",\n")
            if (inference.isUnderstood) {
                intentTextView.append("        \"intent\" : \"${inference.intent}\",\n")
                val slots = inference.slots
                if (slots.isNotEmpty()) {
                    intentTextView.append("        \"slots\" : {\n")
                    for ((key, value) in slots) {
                        intentTextView.append("            \"$key\" : \"$value\",\n")
                    }
                    intentTextView.append("        }\n")
                }
            }
            intentTextView.append("    }\n")
            countDownTimer.start()
        }
    }
    private lateinit var errorTextView: TextView
    private lateinit var errorGuideline: Guideline
    private lateinit var recordButton: ToggleButton
    private lateinit var cheatSheetButton: Button
    private val picovoiceManagerErrorCallback = PicovoiceManagerErrorCallback { e ->
        runOnUiThread {
            onPicovoiceError(e.message ?: "Unknown error")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_second_screen))
        setContentView(R.layout.second_activity)

        val wakeWordNameTextView: TextView = findViewById(R.id.wakeWordName)
        val contextNameTextView: TextView = findViewById(R.id.contextName)
        intentTextView = findViewById(R.id.intentView)
        errorTextView = findViewById(R.id.errorView)
        errorGuideline = findViewById(R.id.errorGuideLine)
        recordButton = findViewById(R.id.startButton)

        wakeWordName = applicationContext.getString(R.string.pvWakeword)
        wakeWordNameTextView.text = wakeWordName

        contextName = applicationContext.getString(R.string.pvContextName)
        contextNameTextView.text = contextName

        initPicovoice()

      /*  val greenBallView = findViewById<ImageView>(R.id.greenBallView)
        val mediaPlayerGreen= MediaPlayer.create(this, R.raw.green)
        greenBallView.setOnTouchListener{ _, event ->
            if( event.action == MotionEvent.ACTION_DOWN){
                mediaPlayerGreen?.start()
            }
            true
        }

        val blackBallView = findViewById<ImageView>(R.id.blackBallView)
        val mediaPlayerBlack= MediaPlayer.create(this, R.raw.black)
        blackBallView.setOnTouchListener{ _, event ->
            if( event.action == MotionEvent.ACTION_DOWN){
                mediaPlayerBlack?.start()
            }
            true
        }

        val blueBallView = findViewById<ImageView>(R.id.blueBallView)
        val mediaPlayerBlue= MediaPlayer.create(this, R.raw.blue)
        blueBallView.setOnTouchListener{ _, event ->
            if( event.action == MotionEvent.ACTION_DOWN){
                mediaPlayerBlue?.start()
            }
            true
        }

        val redBallView = findViewById<ImageView>(R.id.redBallView)
        val mediaPlayerRed= MediaPlayer.create(this, R.raw.red)
        redBallView.setOnTouchListener{ _, event ->
            if( event.action == MotionEvent.ACTION_DOWN){
                mediaPlayerRed?.start()
            }
            true
        }

        val stopButton = findViewById<Button>(R.id.stopButton)
        stopButton.setOnTouchListener{ _, event ->
            if(event.action == MotionEvent.ACTION_DOWN){
                if( mediaPlayerGreen.isPlaying){
                    mediaPlayerGreen.stop()
                    mediaPlayerGreen.prepare()
                } else if( mediaPlayerBlack.isPlaying){
                    mediaPlayerBlack.stop()
                    mediaPlayerBlack.prepare()
                } else if( mediaPlayerBlue.isPlaying){
                    mediaPlayerBlue.stop()
                    mediaPlayerBlue.prepare()
                } else if( mediaPlayerRed.isPlaying){
                    mediaPlayerRed.stop()
                    mediaPlayerRed.prepare()
                }
            }
            true
        }
        fun onDestroy() {
            super.onDestroy()
            // Release the media player when the activity is destroyed
            mediaPlayerGreen?.release()
            mediaPlayerBlack?.release()
            mediaPlayerBlue?.release()
            mediaPlayerRed?.release()
        }
*/
        /*
        We define a Fox character and we set onTouchListener so we can move it
         */
        val characterView = findViewById<ImageView>(R.id.foxView)
        characterView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    val newX = motionEvent.rawX - view.width / 2
                    val newY = motionEvent.rawY - view.height / 2
                    view.x = newX
                    view.y = newY
                }
            }
            true
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission(requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (requestCode == 0) {
                recordButton.toggle()
            }
            onPicovoiceError("Microphone permissions denied")
        } else {
            if (requestCode == 0) {
                process(null)
            } else if (requestCode == 1) {
                // what we want to do here ? as there is no cheatsheet anymore
            }
        }
    }

    private fun initPicovoice() {
        val porcupineModel: String
        val rhinoModel: String

        porcupineModel = "porcupine_params.pv"
        rhinoModel = "rhino_params.pv"

        try {
            picovoiceManager = PicovoiceManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath("${wakeWordName.replace(" ", "-")}.ppn")
                .setPorcupineSensitivity(0.75f)
                .setPorcupineModelPath("models/$porcupineModel")
                .setWakeWordCallback(picovoiceWakeWordCallback)
                .setContextPath("contexts/$contextName.rhn")
                .setRhinoSensitivity(0.25f)
                .setInferenceCallback(picovoiceInferenceCallback)
                .setProcessErrorCallback(picovoiceManagerErrorCallback)
                .build(applicationContext)
            contextInformation = picovoiceManager?.contextInformation ?: ""
            Log.i("PicovoiceManager", contextInformation)
        } catch (e: PicovoiceInvalidArgumentException) {
            onPicovoiceError(e.message ?: "Unknown error")
        } catch (e: PicovoiceActivationException) {
            onPicovoiceError("AccessKey activation error")
        } catch (e: PicovoiceActivationLimitException) {
            onPicovoiceError("AccessKey reached its device limit")
        } catch (e: PicovoiceActivationRefusedException) {
            onPicovoiceError("AccessKey refused")
        } catch (e: PicovoiceActivationThrottledException) {
            onPicovoiceError("AccessKey has been throttled")
        } catch (e: PicovoiceException) {
            onPicovoiceError("Failed to initialize Picovoice ${e.message}")
        }
    }

    fun process(view: View?) {
        val recordButton: ToggleButton = findViewById(R.id.startButton)

        try {
            if (recordButton.isChecked) {
                if (hasRecordPermission()) {
                    picovoiceManager?.start()
                    intentTextView.text = "\n    Listening ...\n"
                } else {
                    requestRecordPermission(0)
                }
            } else {
                countDownTimer.cancel()
                picovoiceManager?.stop()
                intentTextView.text = ""
            }
        } catch (e: PicovoiceException) {
            Log.e("PicovoiceManager", e.message ?: "Unknown error")
        }
    }

    fun showContextCheatSheet(view: View) {
        val builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = findViewById(androidx.constraintlayout.widget.R.id.view_transition)
        val dialogView = LayoutInflater.from(view.context)
            .inflate(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, viewGroup, false)
        builder.setView(dialogView)

        val contextField: TextView = dialogView.findViewById(R.id.contextName)
        contextField.text = contextInformation

        val dialog = builder.create()
        dialog.show()
    }

    private fun onPicovoiceError(errorMessage: String) {
        recordButton.isChecked = false
        recordButton.isEnabled = false
        recordButton.setBackground(ContextCompat.getDrawable(applicationContext, R.drawable.button_background))

        cheatSheetButton.isEnabled = false

        errorTextView.text = errorMessage
        errorTextView.visibility = View.VISIBLE

        val intentParam = intentTextView.layoutParams as ConstraintLayout.LayoutParams
        intentParam.bottomToTop = errorGuideline.id
        intentTextView.requestLayout()
        intentTextView.text = ""
    }

}