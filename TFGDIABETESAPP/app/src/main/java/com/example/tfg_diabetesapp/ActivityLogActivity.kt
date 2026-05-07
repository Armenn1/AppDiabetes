package com.example.tfg_diabetesapp

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg_diabetesapp.widgets.MedicalHeaderView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class ActivityLogActivity : AppCompatActivity() {

    private lateinit var chipGroupTipo: ChipGroup
    private lateinit var sliderIntensidad: Slider
    private lateinit var tvIntensidadLabel: TextView
    private lateinit var etDescripcion: EditText
    private lateinit var btnEmpezarAhora: MaterialButton
    private lateinit var btnRegistrarPasada: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var diaHoras: Double = 5.0
    private var peakMin: Int = 75

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_activity_log)

        val header = findViewById<MedicalHeaderView>(R.id.activityHeader)
        chipGroupTipo = findViewById(R.id.chipGroupTipo)
        sliderIntensidad = findViewById(R.id.sliderIntensidad)
        tvIntensidadLabel = findViewById(R.id.tvIntensidadLabel)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnEmpezarAhora = findViewById(R.id.btnEmpezarAhora)
        btnRegistrarPasada = findViewById(R.id.btnRegistrarPasada)

        header.setOnBackClickListener { finish() }

        sliderIntensidad.addOnChangeListener { _, value, _ ->
            tvIntensidadLabel.text = "${value.toInt()} / 10"
        }

        cargarAjustesMedicos()

        btnEmpezarAhora.setOnClickListener {
            val ahora = System.currentTimeMillis()
            guardarActividad(fechaInicio = ahora, fechaFin = null)
        }

        btnRegistrarPasada.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val inicio = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val fin = System.currentTimeMillis()
                    if (inicio > fin) {
                        Toast.makeText(this, "La hora de inicio no puede ser futura", Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }
                    guardarActividad(fechaInicio = inicio, fechaFin = fin)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun cargarAjustesMedicos() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                diaHoras = doc.getDouble("diaHoras") ?: 5.0
                peakMin = (doc.getLong("insulinaPeakMin") ?: 75L).toInt()
            }
    }

    private fun tipoSeleccionado(): String = when (chipGroupTipo.checkedChipId) {
        R.id.chipAerobicoIntenso -> "aerobico_intenso"
        R.id.chipAnaerobico      -> "anaerobico"
        R.id.chipMixto           -> "mixto"
        else                     -> "aerobico_leve"
    }

    private fun efectoResidual(tipo: String): Double = when (tipo) {
        "anaerobico" -> 6.0
        "mixto"      -> 12.0
        else         -> 24.0
    }

    private fun guardarActividad(fechaInicio: Long, fechaFin: Long?) {
        val userId = auth.currentUser?.uid ?: return
        val tipo = tipoSeleccionado()
        val intensidad = sliderIntensidad.value.toInt()
        val descripcion = etDescripcion.text.toString().trim()
        val duracion = if (fechaFin != null) ((fechaFin - fechaInicio) / 60_000).toInt() else 0
        val efecto = efectoResidual(tipo)

        btnEmpezarAhora.isEnabled = false
        btnRegistrarPasada.isEnabled = false

        // Leer glucosa más reciente
        db.collection("users").document(userId)
            .collection("glucoseReadings")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { readings ->
                val glucosaInicial = readings.documents.firstOrNull()?.getLong("valor")?.toInt() ?: 0

                // Leer IOB actual
                val cutoff = System.currentTimeMillis() - (diaHoras * 3_600_000).toLong()
                db.collection("users").document(userId)
                    .collection("history")
                    .whereGreaterThan("fecha", cutoff)
                    .get()
                    .addOnSuccessListener { historyDocs ->
                        val logs = historyDocs.map { d ->
                            BoloLog(
                                fecha = d.getLong("fecha") ?: 0L,
                                dosisTotal = d.getDouble("dosisTotal") ?: 0.0
                            )
                        }
                        val iob = IobCalculator.calcular(logs, (diaHoras * 60).toInt(), peakMin)

                        val log = ActivityLog(
                            fecha = fechaInicio,
                            fechaFin = fechaFin,
                            duracionMinutos = duracion,
                            tipo = tipo,
                            intensidad = intensidad,
                            descripcion = descripcion,
                            glucosaInicial = glucosaInicial,
                            iobAlEmpezar = iob,
                            efectoResidualHoras = efecto
                        )

                        db.collection("users").document(userId)
                            .collection("activityLogs")
                            .add(log)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Actividad registrada ✓", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                                btnEmpezarAhora.isEnabled = true
                                btnRegistrarPasada.isEnabled = true
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al leer historial", Toast.LENGTH_SHORT).show()
                        btnEmpezarAhora.isEnabled = true
                        btnRegistrarPasada.isEnabled = true
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al leer glucosa", Toast.LENGTH_SHORT).show()
                btnEmpezarAhora.isEnabled = true
                btnRegistrarPasada.isEnabled = true
            }
    }
}
