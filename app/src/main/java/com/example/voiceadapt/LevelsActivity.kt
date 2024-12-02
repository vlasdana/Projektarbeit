package com.example.voiceadapt

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LevelsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_levels)

        // Back-Button finden und Tap-Funktionalität hinzufügen
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Schließt die LevelsActivity und kehrt zur vorherigen zurück
        }
    }
}
