package com.example.tfg_diabetesapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BoloActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bolo)

        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val glucosaInput = findViewById<EditText>(R.id.inputGlucosa)
        val racionesInput = findViewById<EditText>(R.id.inputRacion)
        val boton = findViewById<Button>(R.id.btnCalcular)
        val resultado = findViewById<TextView>(R.id.resultBolo)
        val historial = findViewById<TextView>(R.id.historial)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val factorHC = prefs.getFloat("factorHC", 1.5f)
        val sensibilidad = prefs.getFloat("sensibilidad", 50f)
        val glucosaObjetivo = prefs.getInt("objetivo", 100)

        btnVolver.setOnClickListener {
            finish()
        }

        boton.setOnClickListener {

            val g = glucosaInput.text.toString().toIntOrNull()
            val r = racionesInput.text.toString().toDoubleOrNull()

            // Validación valores
            if (g == null || r == null || r <= 0) {
                resultado.text = "Introduce valores válidos"
                return@setOnClickListener
            }

            if (g <= 0) {
                resultado.text = "La glucosa no puede ser negativa"
                return@setOnClickListener
            }

            // Avisos médicos (informativos)
            if (g < 70) {
                Toast.makeText(this, "⚠️ Glucosa baja (hipoglucemia)", Toast.LENGTH_LONG).show()
            }

            if (g > 250) {
                Toast.makeText(this, "⚠️ Glucosa alta", Toast.LENGTH_LONG).show()
            }

            // Cálculo del bolo
            val boloComida = r * factorHC
            val correccion = (g - glucosaObjetivo) / sensibilidad
            var boloTotal = boloComida + correccion

            if (boloTotal < 0) boloTotal = 0.0

            // Guardamos ultima insulina y el timestamp
            prefs.edit()
                .putFloat("ultima_insulina", boloTotal.toFloat())
                .putLong("tiempo_insulina", System.currentTimeMillis())
                .apply()

            // Mostrar resultado
            resultado.text = "Bolo recomendado: %.2f U".format(boloTotal)

            historial.append(
                "\nGlucosa: $g | Raciones: $r → %.2f U".format(boloTotal)
            )
        }
    }
}
