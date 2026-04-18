package com.example.tfg_diabetesapp.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.tfg_diabetesapp.PerfilHorario
import com.example.tfg_diabetesapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Vistas globales ──────────────────────────────────────────────────────
    private lateinit var etFactorHC: EditText
    private lateinit var etSensibilidad: EditText
    private lateinit var etObjetivoMin: EditText
    private lateinit var etObjetivoMax: EditText
    private lateinit var etUmbralBajo: EditText
    private lateinit var etUmbralAlto: EditText
    private lateinit var switchAlarmas: MaterialSwitch
    private lateinit var etLibreEmail: EditText
    private lateinit var etLibrePassword: EditText
    private lateinit var actvInsulina: AutoCompleteTextView
    private lateinit var tilDiaPersonalizado: TextInputLayout
    private lateinit var etDiaPersonalizado: EditText
    private lateinit var btnSaveSettings: Button

    // ── Perfil Personal ──────────────────────────────────────────────────────
    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText
    private lateinit var actvSexo: AutoCompleteTextView
    private lateinit var etFechaNacimiento: EditText
    private lateinit var actvTipoDiabetes: AutoCompleteTextView
    private lateinit var etInsulinaBasal: EditText
    private lateinit var etDosisBasal: EditText
    private var fechaNacimientoTimestamp: Long? = null

    // ── Perfiles horarios ────────────────────────────────────────────────────
    private val perfiles = mutableListOf<PerfilHorario>()
    private lateinit var llPerfiles: LinearLayout

    companion object {
        val SEXO_OPTIONS = listOf("Hombre", "Mujer", "Otro")
        val TIPO_DIABETES_OPTIONS = listOf("Tipo 1", "Tipo 2", "LADA")
        val INSULINA_OPTIONS = listOf(
            "Novorapid / Humalog (4 h)",
            "Fiasp / Lyumjev (5 h)",
            "Personalizada"
        )
        fun diaDesdeOpcion(opcion: String, valorPersonalizado: String): Double = when (opcion) {
            INSULINA_OPTIONS[0] -> 4.0
            INSULINA_OPTIONS[1] -> 5.0
            else -> valorPersonalizado.toDoubleOrNull() ?: 4.0
        }
        fun indiceDesdeDia(diaHoras: Double): Int = when (diaHoras) {
            4.0 -> 0
            5.0 -> 1
            else -> 2
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        etFactorHC = view.findViewById(R.id.etFactorHC)
        etSensibilidad = view.findViewById(R.id.etSensibilidad)
        etObjetivoMin = view.findViewById(R.id.etObjetivoMin)
        etObjetivoMax = view.findViewById(R.id.etObjetivoMax)
        etUmbralBajo = view.findViewById(R.id.etUmbralBajo)
        etUmbralAlto = view.findViewById(R.id.etUmbralAlto)
        switchAlarmas = view.findViewById(R.id.switchAlarmas)
        etLibreEmail = view.findViewById(R.id.etLibreEmail)
        etLibrePassword = view.findViewById(R.id.etLibrePassword)
        actvInsulina = view.findViewById(R.id.actvInsulina)
        tilDiaPersonalizado = view.findViewById(R.id.tilDiaPersonalizado)
        etDiaPersonalizado = view.findViewById(R.id.etDiaPersonalizado)
        llPerfiles = view.findViewById(R.id.llPerfiles)

        // Perfil Personal
        etPeso = view.findViewById(R.id.etPeso)
        etAltura = view.findViewById(R.id.etAltura)
        actvSexo = view.findViewById(R.id.actvSexo)
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento)
        actvTipoDiabetes = view.findViewById(R.id.actvTipoDiabetes)
        etInsulinaBasal = view.findViewById(R.id.etInsulinaBasal)
        etDosisBasal = view.findViewById(R.id.etDosisBasal)

        // Dropdowns perfil personal
        actvSexo.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, SEXO_OPTIONS))
        actvTipoDiabetes.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, TIPO_DIABETES_OPTIONS))

        // Date picker para fecha de nacimiento
        etFechaNacimiento.setOnClickListener { showDatePicker() }

        // Dropdown insulina
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, INSULINA_OPTIONS)
        actvInsulina.setAdapter(adapter)
        actvInsulina.setOnItemClickListener { _, _, position, _ ->
            tilDiaPersonalizado.visibility = if (position == 2) View.VISIBLE else View.GONE
        }

        btnSaveSettings = view.findViewById(R.id.btnSaveSettings)
        btnSaveSettings.setOnClickListener { onSaveClick() }
        view.findViewById<Button>(R.id.btnAddPerfil).setOnClickListener { showAddPerfilDialog() }

        loadExistingData()
        return view
    }

    // ── Guardar ajustes globales ─────────────────────────────────────────────

    private fun onSaveClick() {
        val factorText = etFactorHC.text.toString()
        val sensibilidadText = etSensibilidad.text.toString()
        val objetivoMinText = etObjetivoMin.text.toString()
        val objetivoMaxText = etObjetivoMax.text.toString()
        val libreEmailText = etLibreEmail.text.toString().trim()
        val librePasswordText = etLibrePassword.text.toString()
        val insulinaText = actvInsulina.text.toString()
        val umbralBajoText = etUmbralBajo.text.toString()
        val umbralAltoText = etUmbralAlto.text.toString()

        if (factorText.isEmpty() || sensibilidadText.isEmpty() || objetivoMinText.isEmpty() || objetivoMaxText.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor rellena los campos médicos", Toast.LENGTH_SHORT).show()
            return
        }
        if (insulinaText == INSULINA_OPTIONS[2] && etDiaPersonalizado.text.toString().toDoubleOrNull() == null) {
            Toast.makeText(requireContext(), "Introduce las horas de duración de tu insulina", Toast.LENGTH_SHORT).show()
            return
        }
        val objetivoMin = objetivoMinText.toDoubleOrNull() ?: 70.0
        val objetivoMax = objetivoMaxText.toDoubleOrNull() ?: 180.0
        if (objetivoMin >= objetivoMax) {
            Toast.makeText(requireContext(), "El objetivo mínimo debe ser menor que el máximo", Toast.LENGTH_SHORT).show()
            return
        }
        val umbralBajo = umbralBajoText.toDoubleOrNull() ?: 70.0
        val umbralAlto = umbralAltoText.toDoubleOrNull() ?: 180.0
        if (umbralBajo >= umbralAlto) {
            Toast.makeText(requireContext(), "El umbral bajo debe ser menor que el alto", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf<String, Any>(
            "factorHC" to factorText.toDouble(),
            "sensibilidad" to sensibilidadText.toDouble(),
            "objetivoMin" to objetivoMin,
            "objetivoMax" to objetivoMax,
            "libreEmail" to libreEmailText,
            "librePassword" to librePasswordText,
            "diaHoras" to diaDesdeOpcion(insulinaText, etDiaPersonalizado.text.toString()),
            "umbralBajo" to umbralBajo,
            "umbralAlto" to umbralAlto,
            "alarmasActivas" to switchAlarmas.isChecked
        )

        // Perfil personal (solo se guardan los campos que tengan valor)
        etPeso.text.toString().toDoubleOrNull()?.let { data["peso"] = it }
        etAltura.text.toString().toDoubleOrNull()?.let { data["altura"] = it }
        actvSexo.text.toString().takeIf { it.isNotEmpty() }?.let { data["sexo"] = it }
        fechaNacimientoTimestamp?.let { data["fechaNacimiento"] = it }
        actvTipoDiabetes.text.toString().takeIf { it.isNotEmpty() }?.let { data["tipoDiabetes"] = it }
        etInsulinaBasal.text.toString().takeIf { it.isNotEmpty() }?.let { data["insulinaBasal"] = it }
        etDosisBasal.text.toString().toDoubleOrNull()?.let { data["dosisBasal"] = it }

        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ── Cargar datos existentes ──────────────────────────────────────────────

    private fun loadExistingData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                val factor = document.getDouble("factorHC")
                val sensi = document.getDouble("sensibilidad")
                // Leer objetivoMin/objetivoMax; retrocompatible con el antiguo campo "target"
                val legacyTarget = document.getDouble("target")
                val objetivoMin = document.getDouble("objetivoMin") ?: legacyTarget ?: 70.0
                val objetivoMax = document.getDouble("objetivoMax") ?: 180.0
                val libreEmail = document.getString("libreEmail") ?: ""
                val librePassword = document.getString("librePassword") ?: ""
                val diaHoras = document.getDouble("diaHoras") ?: 4.0
                val umbralBajo = document.getDouble("umbralBajo") ?: 70.0
                val umbralAlto = document.getDouble("umbralAlto") ?: 180.0
                val alarmasActivas = document.getBoolean("alarmasActivas") ?: false

                if (factor != null) etFactorHC.setText(factor.toString())
                if (sensi != null) etSensibilidad.setText(sensi.toString())
                etObjetivoMin.setText(objetivoMin.toInt().toString())
                etObjetivoMax.setText(objetivoMax.toInt().toString())
                etUmbralBajo.setText(umbralBajo.toInt().toString())
                etUmbralAlto.setText(umbralAlto.toInt().toString())
                switchAlarmas.isChecked = alarmasActivas
                etLibreEmail.setText(libreEmail)
                etLibrePassword.setText(librePassword)

                // Perfil personal
                document.getDouble("peso")?.let { etPeso.setText(it.toString()) }
                document.getDouble("altura")?.let { etAltura.setText(it.toString()) }
                document.getString("sexo")?.takeIf { it.isNotEmpty() }?.let { actvSexo.setText(it, false) }
                document.getLong("fechaNacimiento")?.let { ts ->
                    fechaNacimientoTimestamp = ts
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    etFechaNacimiento.setText(fmt.format(ts))
                }
                document.getString("tipoDiabetes")?.takeIf { it.isNotEmpty() }?.let { actvTipoDiabetes.setText(it, false) }
                document.getString("insulinaBasal")?.takeIf { it.isNotEmpty() }?.let { etInsulinaBasal.setText(it) }
                document.getDouble("dosisBasal")?.let { etDosisBasal.setText(it.toString()) }

                val indice = indiceDesdeDia(diaHoras)
                actvInsulina.setText(INSULINA_OPTIONS[indice], false)
                if (indice == 2) {
                    tilDiaPersonalizado.visibility = View.VISIBLE
                    etDiaPersonalizado.setText(diaHoras.toString())
                }

                // Cargar perfiles horarios
                @Suppress("UNCHECKED_CAST")
                val rawPerfiles = document.get("perfilesHorarios") as? List<Map<*, *>> ?: emptyList()
                perfiles.clear()
                perfiles.addAll(rawPerfiles.map { PerfilHorario.fromMap(it) })
                renderPerfiles()
            }
    }

    // ── Date picker fecha nacimiento ─────────────────────────────────────────

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        fechaNacimientoTimestamp?.let { cal.timeInMillis = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                fechaNacimientoTimestamp = cal.timeInMillis
                val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etFechaNacimiento.setText(fmt.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ── Render lista de perfiles ─────────────────────────────────────────────

    private fun renderPerfiles() {
        llPerfiles.removeAllViews()
        perfiles.forEachIndexed { index, perfil ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_perfil_horario, llPerfiles, false)

            itemView.findViewById<TextView>(R.id.tvPerfilRango).text = perfil.rangoTexto()
            itemView.findViewById<TextView>(R.id.tvPerfilDetalle).text =
                "Ratio: ${perfil.factorHC} U/rac · Sens: ${perfil.sensibilidad.toInt()} mg/dL"

            // Indicador "activo ahora"
            val tvActivo = itemView.findViewById<TextView>(R.id.tvPerfilActivo)
            if (perfil.esActivo()) tvActivo.visibility = View.VISIBLE

            itemView.findViewById<ImageButton>(R.id.btnDeletePerfil).setOnClickListener {
                perfiles.removeAt(index)
                savePerfilesToFirestore()
                renderPerfiles()
            }

            llPerfiles.addView(itemView)
        }
    }

    // ── Diálogo añadir perfil ────────────────────────────────────────────────

    private fun showAddPerfilDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_perfil, null)

        val etInicio = dialogView.findViewById<EditText>(R.id.etPerfilInicio)
        val etFin = dialogView.findViewById<EditText>(R.id.etPerfilFin)
        val etRatio = dialogView.findViewById<EditText>(R.id.etPerfilRatio)
        val etSensi = dialogView.findViewById<EditText>(R.id.etPerfilSensibilidad)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva franja horaria")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val inicio = etInicio.text.toString().toIntOrNull()
                val fin = etFin.text.toString().toIntOrNull()
                val ratio = etRatio.text.toString().toDoubleOrNull()
                val sensi = etSensi.text.toString().toDoubleOrNull()

                when {
                    inicio == null || fin == null || ratio == null || sensi == null ->
                        Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                    inicio !in 0..23 || fin !in 0..23 ->
                        Toast.makeText(requireContext(), "Las horas deben ser entre 0 y 23", Toast.LENGTH_SHORT).show()
                    inicio == fin ->
                        Toast.makeText(requireContext(), "La hora de inicio y fin no pueden ser iguales", Toast.LENGTH_SHORT).show()
                    else -> {
                        val nuevoPerfil = PerfilHorario(inicio, fin, ratio, sensi)
                        perfiles.add(nuevoPerfil)
                        // Ordenar por hora de inicio para que la lista sea coherente
                        perfiles.sortBy { it.horaInicio }
                        savePerfilesToFirestore()
                        renderPerfiles()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Guardar perfiles en Firestore ────────────────────────────────────────

    private fun savePerfilesToFirestore() {
        val userId = auth.currentUser?.uid ?: return
        val perfilesMap = perfiles.map { PerfilHorario.toMap(it) }
        db.collection("users").document(userId)
            .update("perfilesHorarios", perfilesMap)
            .addOnFailureListener {
                // Si el documento no existe aún, usar set con merge
                db.collection("users").document(userId)
                    .set(mapOf("perfilesHorarios" to perfilesMap), SetOptions.merge())
            }
    }
}
