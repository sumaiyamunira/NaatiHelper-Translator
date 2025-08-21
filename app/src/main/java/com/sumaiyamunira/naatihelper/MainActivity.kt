package com.sumaiyamunira.naatihelper


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var btnSwitchLang: Button
    private lateinit var btnTranslate: Button
    private lateinit var btnClear: Button
    private lateinit var btnSpeak: Button
    private lateinit var inputText: EditText
    private lateinit var outputText: TextView

    private var isBanglaToEnglish = true
    private val SPEECH_REQUEST_CODE = 100
    private val RECORD_AUDIO_REQUEST_CODE = 101

    private val apiKey = "YOUR_API_KEY_HERE"  // 🔒 Replace with your key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSwitchLang = findViewById(R.id.btnSwitchLang)
        btnTranslate = findViewById(R.id.btnTranslate)
        btnClear = findViewById(R.id.btnClear)
        btnSpeak = findViewById(R.id.btnSpeak)
        inputText = findViewById(R.id.inputText)
        outputText = findViewById(R.id.outputText)

        btnSwitchLang.setOnClickListener {
            isBanglaToEnglish = !isBanglaToEnglish
            btnSwitchLang.text = if (isBanglaToEnglish) "Bangla → English" else "English → Bangla"
        }

        btnClear.setOnClickListener {
            inputText.setText("")
            outputText.text = ""
        }

        btnTranslate.setOnClickListener {
            val input = inputText.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prompt = if (isBanglaToEnglish)
                "Translate this Bangla sentence to English:\n$input"
            else
                "Translate this English sentence to Bangla:\n$input"

            translateWithGemini(prompt)

        }

        btnSpeak.setOnClickListener {
            askAudioPermissionAndRecord()
        }
    }

    private fun askAudioPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        } else {
            startSpeechRecognition()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (isBanglaToEnglish) "bn-BD" else "en-US"
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            inputText.setText(spokenText)
        }
    }


    private fun translateWithGemini(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = generationConfig {
                    temperature = 0.7f
                }

                val model = GenerativeModel(
                    modelName = "gemini-1.5-flash-latest",
                    apiKey = apiKey,
                    generationConfig = config
                )

                val response = model.generateContent(prompt)

                runOnUiThread {
                    outputText.text = response.text ?: "No translation found"
                }
            } catch (e: Exception) {
                Log.e("GeminiError", "Translation failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}