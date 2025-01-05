package com.example.voiceadapt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

    class ColorsActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Set layout XML for this activity
            setContentView(R.layout.activity_colors)
            // Afișează un mesaj pentru debug

            // Back-Button finden und Tap-Funktionalität hinzufügen
            val backButton = findViewById<Button>(R.id.backButton)
                backButton.setOnClickListener {
                val intent = Intent(this, LevelsActivity::class.java)
                startActivity(intent)
                finish()
            }

        }

    }
