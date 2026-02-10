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

        val etFactorHC = findViewById<EditText>(R.id.etFactorHC)
        val etSensibilidad = findViewById<EditText>(R.id.etSensibilidad)
        val etObjetivo = findViewById<EditText>(R.id.etObjetivo)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // 1. Cargar datos si ya existen (opcional pero recomendado)
        loadExistingData(etFactorHC, etSensibilidad)

        // 2. Acción de Guardar
        btnSave.setOnClickListener {
            val factorText = etFactorHC.text.toString()
            val sensibilidadText = etSensibilidad.text.toString()

            if (factorText.isNotEmpty() && sensibilidadText.isNotEmpty()) {
                saveToFirestore(factorText.toDouble(), sensibilidadText.toDouble())
            } else {
                Toast.makeText(this, "Por favor rellena ambos campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Botón Volver
        btnBack.setOnClickListener {
            finish() // Cierra la actividad y vuelve a la anterior
        }
    }

    private fun saveToFirestore(factor: Double, sensibilidad: Double) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Creamos un mapa con los datos (clave -> valor)
            val medicalData = hashMapOf(
                "factorHC" to factor,
                "sensibilidad" to sensibilidad
            )

            // Guardamos en la colección "users", documento = userId
            db.collection("users").document(userId)
                .set(medicalData)
                .addOnSuccessListener{
                    Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                    finish() // Opcional: volver al dashboard tras guardar
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExistingData(etFactor: EditText, etSensibilidad: EditText) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Recuperamos los datos y los ponemos en los EditText
                        val factor = document.getDouble("factorHC")
                        val sensi = document.getDouble("sensibilidad")

                        etFactor.setText(factor.toString())
                        etSensibilidad.setText(sensi.toString())
                    }
                }
        }
    }
}