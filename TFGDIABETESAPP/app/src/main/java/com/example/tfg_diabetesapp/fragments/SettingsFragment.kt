package com.example.tfg_diabetesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg_diabetesapp.R
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
        val etLibreEmail = view.findViewById<EditText>(R.id.etLibreEmail)
        val etLibrePassword = view.findViewById<EditText>(R.id.etLibrePassword)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)

        // 1. Cargar datos
        loadExistingData(etFactorHC, etSensibilidad, etObjetivo, etLibreEmail, etLibrePassword)

        // 2. Acción de Guardar
        btnSave.setOnClickListener {
            val factorText = etFactorHC.text.toString()
            val sensibilidadText = etSensibilidad.text.toString()
            val objetivoText = etObjetivo.text.toString()
            val libreEmailText = etLibreEmail.text.toString().trim()
            val librePasswordText = etLibrePassword.text.toString()

            if (factorText.isNotEmpty() && sensibilidadText.isNotEmpty() && objetivoText.isNotEmpty()) {
                saveToFirestore(
                    factorText.toDouble(),
                    sensibilidadText.toDouble(),
                    objetivoText.toDouble(),
                    libreEmailText,
                    librePasswordText
                )
            } else {
                Toast.makeText(requireContext(), "Por favor rellena los campos médicos", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun saveToFirestore(factor: Double, sensibilidad: Double, objetivo: Double, libreEmail: String, librePassword: String) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val data = hashMapOf(
                "factorHC" to factor,
                "sensibilidad" to sensibilidad,
                "target" to objetivo,
                "libreEmail" to libreEmail,
                "librePassword" to librePassword
            )

            db.collection("users").document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Usuario no identificado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExistingData(etFactor: EditText, etSensibilidad: EditText, etObjetivo: EditText, etLibreEmail: EditText, etLibrePassword: EditText) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val factor = document.getDouble("factorHC")
                        val sensi = document.getDouble("sensibilidad")
                        val target = document.getDouble("target") ?: 100.0
                        val libreEmail = document.getString("libreEmail") ?: ""
                        val librePassword = document.getString("librePassword") ?: ""

                        if (factor != null) etFactor.setText(factor.toString())
                        if (sensi != null) etSensibilidad.setText(sensi.toString())
                        etObjetivo.setText(target.toString())
                        etLibreEmail.setText(libreEmail)
                        etLibrePassword.setText(librePassword)
                    }
                }
        }
    }
}