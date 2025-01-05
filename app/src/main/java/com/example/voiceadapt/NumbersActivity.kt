package com.example.voiceadapt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NumbersActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Set layout XML for this activity
            setContentView(R.layout.activity_numbers)

            // Back-Button finden und Tap-Funktionalität hinzufügen
            val backButton = findViewById<Button>(R.id.backButton)
            backButton.setOnClickListener {
                val intent = Intent(this, LevelsActivity::class.java)
                startActivity(intent)
                finish()
            }
    }
}