package com.example.voiceadapt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class LevelsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        // Play Button for Farben
        val colorsPlayButton: ImageButton = findViewById(R.id.colorsPlayButton)
        colorsPlayButton.setOnClickListener {
            val intent = Intent(this, ColorsActivity::class.java)
            startActivity(intent)
        }

        // Play Button for Zahlen
        val numbersPlayButton: ImageButton = findViewById(R.id.numbersPlayButton)
        numbersPlayButton.setOnClickListener {
            val intent = Intent(this, NumbersActivity::class.java)
            startActivity(intent)
        }

        // Back-Button finden und Tap-Funktionalität hinzufügen
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Schließt die LevelsActivity und kehrt zur vorherigen zurück
        }
    }
}
