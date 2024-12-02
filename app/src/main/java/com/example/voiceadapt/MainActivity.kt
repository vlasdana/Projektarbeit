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
import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private val ACCESS_KEY = "0kXDK8vwhmMSSIxdO2PqBDDItFYmNzmXlhH/dNbXXIUU8MCazREmCw=="
    private var wakeWordName = "play game" // Replace with your actual wake word name
    private var contextName = "lingoen" // Replace with your actual context name
    private var contextInformation = ""

    private var picovoiceManager: PicovoiceManager? = null
    private lateinit var intentTextView: TextView
    private lateinit var languageTextView: TextView

    private val languageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedLanguage = result.data?.getStringExtra("selected_language")
            languageTextView.text = "Selected language: $selectedLanguage"
        }
    }
    private val countDownTimer = object : CountDownTimer(2000, 1000) {
        override fun onTick(l: Long) {}

        override fun onFinish() {
            intentTextView.text = "\n    Listening ...\n"
        }
    }
    private val picovoiceWakeWordCallback = PicovoiceWakeWordCallback {
        runOnUiThread {
            countDownTimer.cancel()
            intentTextView.text = "\n    Wake Word Detected 👀🧿...\n"
        }
    }
    private val picovoiceInferenceCallback = PicovoiceInferenceCallback { inference ->
        runOnUiThread {
            intentTextView.text = "\n    {\n"
            intentTextView.append("        \"isUnderstood\" : \"${inference.isUnderstood}\",\n")
            if (inference.isUnderstood) {
                intentTextView.append("        \"intent\" : \"${inference.intent}\",\n")

                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)

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
    private val picovoiceManagerErrorCallback = PicovoiceManagerErrorCallback { e ->
        runOnUiThread {
            onPicovoiceError(e.message ?: "Unknown error")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_first_screen))
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

        val btnLanguage = findViewById<Button>(R.id.btn_mothertongue)
        languageTextView = findViewById(R.id.language_text_view)

        btnLanguage.setOnClickListener{
            val intent = Intent(this, LanguageActivity::class.java)
            languageResultLauncher.launch(intent)
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
                // what you want to do
            }
        }
    }
    fun process(view: View?) {
        val recordButton: ToggleButton = findViewById(R.id.startButton)

        try {
            if (recordButton.isChecked) {
                val intent=Intent(this,ChoiceActivity::class.java)
                startActivity(intent)
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

    private fun initPicovoice() {
        val porcupineModel: String

        porcupineModel = "porcupine_params.pv"

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



    private fun onPicovoiceError(errorMessage: String) {
        recordButton.isChecked = false
        recordButton.isEnabled = true
       // recordButton.setBackground(ContextCompat.getDrawable(applicationContext, R.drawable.button_background))

        errorTextView.text = errorMessage
        errorTextView.visibility = View.VISIBLE

        val intentParam = intentTextView.layoutParams as ConstraintLayout.LayoutParams
        intentParam.bottomToTop = errorGuideline.id
        intentTextView.requestLayout()
        intentTextView.text = ""
    }

}