package com.example.tfg_diabetesapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    // Instancias de Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Referencias a la UI
        val etFactorHC = findViewById<EditText>(R.id.etFactorHC)
        val etSensibilidad = findViewById<EditText>(R.id.etSensibilidad)
        val etObjetivo = findViewById<EditText>(R.id.etObjetivo) // <--- Esta ya la tenías

        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // 1. Cargar datos
        loadExistingData(etFactorHC, etSensibilidad, etObjetivo)

        // 2. Acción de Guardar
        btnSave.setOnClickListener {
            val factorText = etFactorHC.text.toString()
            val sensibilidadText = etSensibilidad.text.toString()
            val objetivoText = etObjetivo.text.toString() //

            if (factorText.isNotEmpty() && sensibilidadText.isNotEmpty() && objetivoText.isNotEmpty()) {
                // Pasamos los 3 valores convertidos a Double
                saveToFirestore(
                    factorText.toDouble(),
                    sensibilidadText.toDouble(),
                    objetivoText.toDouble()
                )
            } else {
                Toast.makeText(this, "Por favor rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Botón Volver
        btnBack.setOnClickListener {
            finish()
        }
    }


    private fun saveToFirestore(factor: Double, sensibilidad: Double, objetivo: Double) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Creamos el mapa con los 3 datos
            val medicalData = hashMapOf(
                "factorHC" to factor,
                "sensibilidad" to sensibilidad,
                "target" to objetivo
            )

            db.collection("users").document(userId)
                .set(medicalData)
                .addOnSuccessListener{
                    Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
                    finish() // Cerramos al guardar
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
        }
    }

    // AHORA RECIBE 3 EDITTEXT PARA RELLENAR
    private fun loadExistingData(etFactor: EditText, etSensibilidad: EditText, etObjetivo: EditText) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val factor = document.getDouble("factorHC")
                        val sensi = document.getDouble("sensibilidad")

                        // Intentamos leer 'target'. Si no existe ponemos 100.0 por defecto
                        val target = document.getDouble("target") ?: 100.0

                        if (factor != null) etFactor.setText(factor.toString())
                        if (sensi != null) etSensibilidad.setText(sensi.toString())
                        etObjetivo.setText(target.toString()) // <--- Ponemos el valor en la caja
                    }
                }
        }
    }
}