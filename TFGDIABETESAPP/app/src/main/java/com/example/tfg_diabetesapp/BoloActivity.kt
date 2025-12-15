package com.example.tfg_diabetesapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BoloActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bolo)

        val glucosa = findViewById<EditText>(R.id.inputGlucosa)
        val raciones = findViewById<EditText>(R.id.inputRacion)
        val boton = findViewById<Button>(R.id.btnCalcular)
        val resultado = findViewById<TextView>(R.id.resultBolo)

        val btnVolver = findViewById<Button>(R.id.btnVolver)

        btnVolver.setOnClickListener {
            finish()
        }

        boton.setOnClickListener {
            val g = glucosa.text.toString().toIntOrNull()
            val r = raciones.text.toString().toDoubleOrNull()

            if (g == null || r == null) {
                resultado.text = "Introduce valores válidos"
            } else {
                // Ejemplo sencillo de cálculo
                val bolo = (r * 1.5) + ((g - 100) / 50.0)
                resultado.text = "Bolo recomendado: %.2f U".format(bolo)
            }
        }
    }
}
