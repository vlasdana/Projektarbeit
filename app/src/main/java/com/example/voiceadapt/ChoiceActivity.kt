package com.example.voiceadapt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ChoiceActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)

        // Play-Button finden und konfigurieren
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            val intent = Intent(this, LevelsActivity::class.java) // Zur LevelsActivity navigieren
            startActivity(intent)
        }

        // Button finden
        val backButton = findViewById<Button>(R.id.backButton)

        // Tap-Funktionalität
        backButton.setOnClickListener {
            navigateBack()
        }

    }

    private fun navigateBack() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Schließt die aktuelle Activity
    }

}