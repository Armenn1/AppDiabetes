package com.example.tfg_diabetesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tfg_diabetesapp.R
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etFactorHC: EditText
    private lateinit var etSensibilidad: EditText
    private lateinit var etObjetivo: EditText
    private lateinit var etUmbralBajo: EditText
    private lateinit var etUmbralAlto: EditText
    private lateinit var switchAlarmas: MaterialSwitch
    private lateinit var etLibreEmail: EditText
    private lateinit var etLibrePassword: EditText
    private lateinit var actvInsulina: AutoCompleteTextView
    private lateinit var tilDiaPersonalizado: TextInputLayout
    private lateinit var etDiaPersonalizado: EditText

    companion object {
        val INSULINA_OPTIONS = listOf(
            "Novorapid / Humalog (4 h)",
            "Fiasp / Lyumjev (5 h)",
            "Personalizada"
        )
        // Mapa de opción → diaHoras
        fun diaDesdeOpcion(opcion: String, valorPersonalizado: String): Double = when (opcion) {
            INSULINA_OPTIONS[0] -> 4.0
            INSULINA_OPTIONS[1] -> 5.0
            else -> valorPersonalizado.toDoubleOrNull() ?: 4.0
        }
        // Mapa inverso: diaHoras → índice de opción (0, 1 ó 2)
        fun indiceDesdeDia(diaHoras: Double): Int = when (diaHoras) {
            4.0 -> 0
            5.0 -> 1
            else -> 2
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        etFactorHC = view.findViewById(R.id.etFactorHC)
        etSensibilidad = view.findViewById(R.id.etSensibilidad)
        etObjetivo = view.findViewById(R.id.etObjetivo)
        etUmbralBajo = view.findViewById(R.id.etUmbralBajo)
        etUmbralAlto = view.findViewById(R.id.etUmbralAlto)
        switchAlarmas = view.findViewById(R.id.switchAlarmas)
        etLibreEmail = view.findViewById(R.id.etLibreEmail)
        etLibrePassword = view.findViewById(R.id.etLibrePassword)
        actvInsulina = view.findViewById(R.id.actvInsulina)
        tilDiaPersonalizado = view.findViewById(R.id.tilDiaPersonalizado)
        etDiaPersonalizado = view.findViewById(R.id.etDiaPersonalizado)

        // Configurar dropdown de tipo de insulina
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, INSULINA_OPTIONS)
        actvInsulina.setAdapter(adapter)
        actvInsulina.setOnItemClickListener { _, _, position, _ ->
            tilDiaPersonalizado.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)

        loadExistingData()

        btnSave.setOnClickListener {
            val factorText = etFactorHC.text.toString()
            val sensibilidadText = etSensibilidad.text.toString()
            val objetivoText = etObjetivo.text.toString()
            val libreEmailText = etLibreEmail.text.toString().trim()
            val librePasswordText = etLibrePassword.text.toString()
            val insulinaText = actvInsulina.text.toString()

            val umbralBajoText = etUmbralBajo.text.toString()
            val umbralAltoText = etUmbralAlto.text.toString()

            if (factorText.isNotEmpty() && sensibilidadText.isNotEmpty() && objetivoText.isNotEmpty()) {
                // Validar DIA personalizada si está seleccionada
                if (insulinaText == INSULINA_OPTIONS[2] && etDiaPersonalizado.text.toString().toDoubleOrNull() == null) {
                    Toast.makeText(requireContext(), "Introduce las horas de duración de tu insulina", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val umbralBajo = umbralBajoText.toDoubleOrNull() ?: 70.0
                val umbralAlto = umbralAltoText.toDoubleOrNull() ?: 180.0
                if (umbralBajo >= umbralAlto) {
                    Toast.makeText(requireContext(), "El umbral bajo debe ser menor que el alto", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val diaHoras = diaDesdeOpcion(insulinaText, etDiaPersonalizado.text.toString())
                saveToFirestore(
                    factorText.toDouble(),
                    sensibilidadText.toDouble(),
                    objetivoText.toDouble(),
                    libreEmailText,
                    librePasswordText,
                    diaHoras,
                    umbralBajo,
                    umbralAlto,
                    switchAlarmas.isChecked
                )
            } else {
                Toast.makeText(requireContext(), "Por favor rellena los campos médicos", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun saveToFirestore(
        factor: Double,
        sensibilidad: Double,
        objetivo: Double,
        libreEmail: String,
        librePassword: String,
        diaHoras: Double,
        umbralBajo: Double,
        umbralAlto: Double,
        alarmasActivas: Boolean
    ) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "factorHC" to factor,
            "sensibilidad" to sensibilidad,
            "target" to objetivo,
            "libreEmail" to libreEmail,
            "librePassword" to librePassword,
            "diaHoras" to diaHoras,
            "umbralBajo" to umbralBajo,
            "umbralAlto" to umbralAlto,
            "alarmasActivas" to alarmasActivas
        )

        db.collection("users").document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadExistingData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val factor = document.getDouble("factorHC")
                    val sensi = document.getDouble("sensibilidad")
                    val target = document.getDouble("target") ?: 100.0
                    val libreEmail = document.getString("libreEmail") ?: ""
                    val librePassword = document.getString("librePassword") ?: ""
                    val diaHoras = document.getDouble("diaHoras") ?: 4.0
                    val umbralBajo = document.getDouble("umbralBajo") ?: 70.0
                    val umbralAlto = document.getDouble("umbralAlto") ?: 180.0
                    val alarmasActivas = document.getBoolean("alarmasActivas") ?: false

                    if (factor != null) etFactorHC.setText(factor.toString())
                    if (sensi != null) etSensibilidad.setText(sensi.toString())
                    etObjetivo.setText(target.toString())
                    etUmbralBajo.setText(umbralBajo.toInt().toString())
                    etUmbralAlto.setText(umbralAlto.toInt().toString())
                    switchAlarmas.isChecked = alarmasActivas
                    etLibreEmail.setText(libreEmail)
                    etLibrePassword.setText(librePassword)

                    // Pre-seleccionar el tipo de insulina
                    val indice = indiceDesdeDia(diaHoras)
                    actvInsulina.setText(INSULINA_OPTIONS[indice], false)
                    if (indice == 2) {
                        tilDiaPersonalizado.visibility = View.VISIBLE
                        etDiaPersonalizado.setText(diaHoras.toString())
                    }
                }
            }
    }
}
