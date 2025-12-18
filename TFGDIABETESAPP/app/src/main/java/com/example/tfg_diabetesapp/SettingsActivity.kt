package com.example.tfg_diabetesapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val inputFactor = findViewById<EditText>(R.id.inputFactorHC)
        val inputSensibilidad = findViewById<EditText>(R.id.inputSensibilidad)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Cargar valores guardados si existen
        inputFactor.setText(prefs.getFloat("factorHC", 1.5f).toString())
        inputSensibilidad.setText(prefs.getFloat("sensibilidad", 50f).toString())

        btnGuardar.setOnClickListener {
            val factor = inputFactor.text.toString().toFloatOrNull()
            val sensibilidad = inputSensibilidad.text.toString().toFloatOrNull()

            if (factor != null && sensibilidad != null) {
                prefs.edit()
                    .putFloat("factorHC", factor)
                    .putFloat("sensibilidad", sensibilidad)
                    .apply()

                finish()
            }
        }
    }
}
