package com.example.tfg_diabetesapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BoloActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bolo)

        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val glucosa = findViewById<EditText>(R.id.inputGlucosa)
        val raciones = findViewById<EditText>(R.id.inputRacion)
        val boton = findViewById<Button>(R.id.btnCalcular)
        val resultado = findViewById<TextView>(R.id.resultBolo)
        val historial = findViewById<TextView>(R.id.historial)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val factorHC = prefs.getFloat("factorHC", 1.5f)
        val sensibilidad = prefs.getFloat("sensibilidad", 50f)

        btnVolver.setOnClickListener {
            finish()
        }

        boton.setOnClickListener {
            val g = glucosa.text.toString().toIntOrNull()
            val r = raciones.text.toString().toDoubleOrNull()

            if (g == null || r == null) {
                resultado.text = "Introduce valores válidos"
            } else {
                val bolo = (r * factorHC) + ((g - 100) / sensibilidad)

                resultado.text = "Bolo recomendado: %.2f U".format(bolo)
                historial.append("\nGlucosa: $g | Raciones: $r → %.2f U".format(bolo))
            }
        }
    }
}
