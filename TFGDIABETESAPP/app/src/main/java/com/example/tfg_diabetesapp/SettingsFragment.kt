package com.example.tfg_diabetesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    // Instancias de Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflamos el layout para este fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Referencias a la UI (usando 'view.')
        val etFactorHC = view.findViewById<EditText>(R.id.etFactorHC)
        val etSensibilidad = view.findViewById<EditText>(R.id.etSensibilidad)
        val etObjetivo = view.findViewById<EditText>(R.id.etObjetivo)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)

        // 1. Cargar datos
        loadExistingData(etFactorHC, etSensibilidad, etObjetivo)

        // 2. Acción de Guardar
        btnSave.setOnClickListener {
            val factorText = etFactorHC.text.toString()
            val sensibilidadText = etSensibilidad.text.toString()
            val objetivoText = etObjetivo.text.toString()

            if (factorText.isNotEmpty() && sensibilidadText.isNotEmpty() && objetivoText.isNotEmpty()) {
                // Pasamos los 3 valores convertidos a Double
                saveToFirestore(
                    factorText.toDouble(),
                    sensibilidadText.toDouble(),
                    objetivoText.toDouble()
                )
            } else {
                Toast.makeText(requireContext(), "Por favor rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        return view
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
                    Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
                    // Se elimina finish() porque el Fragment no se cierra, se queda esperando interacción
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Usuario no identificado", Toast.LENGTH_SHORT).show()
        }
    }

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
                        etObjetivo.setText(target.toString())
                    }
                }
        }
    }
}