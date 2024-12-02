package com.example.voiceadapt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class LanguageActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val deutschOption: LinearLayout = findViewById(R.id.deutsch_option)

        deutschOption.setOnClickListener{
            val resultIntent = Intent()
            resultIntent.putExtra("selected_language", "Deutsch")
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

}
