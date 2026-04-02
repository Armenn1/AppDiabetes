package com.example.tfg_diabetesapp.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg_diabetesapp.BoloActivity
import com.example.tfg_diabetesapp.LoginActivity
import com.example.tfg_diabetesapp.R
import com.example.tfg_diabetesapp.glucose.GlucoseMeasurement
import com.example.tfg_diabetesapp.glucose.LibreLinkUpRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvGlucosa: TextView
    private lateinit var tvIOB: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var pbGlucoseLoading: ProgressBar
    private lateinit var glucoseChart: LineChart

    // Credenciales LibreLinkUp (se cargan desde Firestore)
    private var libreEmail = ""
    private var librePassword = ""

    // Último valor de glucosa conocido (para mostrar si la API falla)
    private var lastKnownGlucose: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val cardNewBolo = view.findViewById<MaterialCardView>(R.id.cardNewBolo)
        tvGlucosa = view.findViewById(R.id.tvGlucosaMain)
        tvIOB = view.findViewById(R.id.tvIOB)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        pbGlucoseLoading = view.findViewById(R.id.pbGlucoseLoading)
        glucoseChart = view.findViewById(R.id.glucoseChart)
        setupGlucoseChart()

        cardNewBolo.setOnClickListener {
            startActivity(Intent(requireContext(), BoloActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Cargar credenciales y lanzar fetch inmediato al arrancar
        loadLibreCredentials(fetchAfterLoad = true)

        // Loop número: cada 60 segundos
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                fetchGlucoseNumber()
                delay(60_000L)
            }
        }

        // Loop gráfico: al entrar y luego cada 10 minutos
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                fetchGlucoseChart()
                delay(600_000L)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Recargar credenciales LibreLinkUp por si el usuario las cambió en Ajustes
        loadLibreCredentials()
        // Actualizar IOB al volver (ej: después de calcular un bolo)
        refreshIobData()
    }

    private fun loadLibreCredentials(fetchAfterLoad: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val newEmail = document.getString("libreEmail") ?: ""
                val newPassword = document.getString("librePassword") ?: ""
                val credentialsChanged = newEmail != libreEmail || newPassword != librePassword
                libreEmail = newEmail
                librePassword = newPassword
                // Si se pidió fetch inmediato o las credenciales cambiaron, actualizar glucosa ahora
                if ((fetchAfterLoad || credentialsChanged) && libreEmail.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch { fetchGlucoseNumber() }
                    viewLifecycleOwner.lifecycleScope.launch { fetchGlucoseChart() }
                }
            }
    }

    // Actualiza solo el número de glucosa (cada 60s)
    private suspend fun fetchGlucoseNumber() {
        if (libreEmail.isEmpty() || librePassword.isEmpty()) {
            withContext(Dispatchers.Main) {
                tvGlucosa.text = "--"
                tvLastUpdated.text = "Configura tu cuenta LibreLinkUp en Ajustes"
            }
            return
        }

        withContext(Dispatchers.Main) { pbGlucoseLoading.visibility = View.VISIBLE }

        val value = withContext(Dispatchers.IO) {
            LibreLinkUpRepository.getLatestGlucose(libreEmail, librePassword)
        }

        withContext(Dispatchers.Main) {
            pbGlucoseLoading.visibility = View.GONE
            if (value != null) {
                lastKnownGlucose = value
                updateGlucoseCard(value.toDouble(), stale = false)
                val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                tvLastUpdated.text = "Última act. $hora"
            } else if (lastKnownGlucose != null) {
                updateGlucoseCard(lastKnownGlucose!!.toDouble(), stale = true)
                tvLastUpdated.text = "Sin conexión · último dato"
            } else {
                tvGlucosa.text = "--"
                tvLastUpdated.text = "Error de conexión"
            }
        }
    }

    // Actualiza el gráfico (al entrar + cada 10 min)
    private suspend fun fetchGlucoseChart() {
        if (libreEmail.isEmpty() || librePassword.isEmpty()) return

        val result = withContext(Dispatchers.IO) {
            LibreLinkUpRepository.getGlucoseData(libreEmail, librePassword)
        }

        withContext(Dispatchers.Main) {
            if (result != null) updateGlucoseChart(result.graphMeasurements)
        }
    }

    private fun setupGlucoseChart() {
        glucoseChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            legend.isEnabled = false

            xAxis.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.GRAY
                textSize = 9f
                axisMinimum = 40f
                axisMaximum = 300f

                val targetGreen = Color.parseColor("#4CAF50")

                val lowLine = LimitLine(70f, "70").apply {
                    lineWidth = 1.5f
                    lineColor = targetGreen
                    enableDashedLine(10f, 6f, 0f)
                    textColor = targetGreen
                    textSize = 9f
                }
                val highLine = LimitLine(180f, "180").apply {
                    lineWidth = 1.5f
                    lineColor = targetGreen
                    enableDashedLine(10f, 6f, 0f)
                    textColor = targetGreen
                    textSize = 9f
                }
                addLimitLine(lowLine)
                addLimitLine(highLine)
                setDrawLimitLinesBehindData(true)
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateGlucoseChart(measurements: List<GlucoseMeasurement>) {
        if (measurements.isEmpty()) return

        val entries = measurements.mapIndexed { index, m ->
            Entry(index.toFloat(), m.value.toFloat())
        }

        val lineColor = Color.parseColor("#29B6F6") // azul claro

        val dataSet = LineDataSet(entries, "Glucosa").apply {
            color = lineColor
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillColor = lineColor
            fillAlpha = 40
        }

        glucoseChart.data = LineData(dataSet)
        glucoseChart.invalidate()
    }

    private fun refreshIobData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastLog = documents.documents[0]
                    val dosisTotal = lastLog.getDouble("dosisTotal") ?: 0.0
                    val fechaLog = lastLog.getLong("fecha") ?: System.currentTimeMillis()
                    calculateAndShowIOB(dosisTotal, fechaLog)
                } else {
                    tvIOB.text = "0.0 U"
                }
            }
    }

    private fun updateGlucoseCard(glucosa: Double, stale: Boolean = false) {
        tvGlucosa.text = glucosa.roundToInt().toString()

        val color = when {
            stale -> android.R.color.darker_gray
            glucosa < 70 -> android.R.color.holo_red_light
            glucosa > 180 -> android.R.color.holo_orange_light
            else -> R.color.green_ok
        }

        tvGlucosa.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun calculateAndShowIOB(dosis: Double, fechaLog: Long) {
        val ahora = System.currentTimeMillis()
        val minutosPasados = TimeUnit.MILLISECONDS.toMinutes(ahora - fechaLog)

        if (minutosPasados >= 240) {
            tvIOB.text = "0.0 U"
        } else {
            val factorRestante = 1.0 - (minutosPasados.toDouble() / 240.0)
            var iob = dosis * factorRestante
            if (iob < 0) iob = 0.0
            val iobRedondeado = (iob * 10.0).roundToInt() / 10.0
            tvIOB.text = "$iobRedondeado U"
        }
    }
}
