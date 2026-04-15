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
import com.example.tfg_diabetesapp.BoloLog
import com.example.tfg_diabetesapp.IobCalculator
import com.example.tfg_diabetesapp.LoginActivity
import com.example.tfg_diabetesapp.R
import com.example.tfg_diabetesapp.glucose.GlucoseMeasurement
import com.example.tfg_diabetesapp.glucose.LibreLinkUpRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // DIA de la insulina en horas (se carga desde Firestore, default 4h)
    private var diaHoras: Double = 4.0

    // Umbrales configurables (con defaults médicos estándar)
    private var umbralBajo: Float = 70f
    private var umbralAlto: Float = 180f

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

        // Cargar credenciales y umbrales, y lanzar fetch inmediato al arrancar
        loadUserSettings(fetchAfterLoad = true)

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
        // Recargar ajustes por si el usuario los cambió en Ajustes
        loadUserSettings()
        // Actualizar IOB al volver (ej: después de calcular un bolo)
        refreshIobData()
    }

    private fun loadUserSettings(fetchAfterLoad: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val newEmail = document.getString("libreEmail") ?: ""
                val newPassword = document.getString("librePassword") ?: ""
                diaHoras = document.getDouble("diaHoras") ?: 4.0

                val newUmbralBajo = (document.getDouble("umbralBajo") ?: 70.0).toFloat()
                val newUmbralAlto = (document.getDouble("umbralAlto") ?: 180.0).toFloat()

                val credentialsChanged = newEmail != libreEmail || newPassword != librePassword
                val thresholdsChanged = newUmbralBajo != umbralBajo || newUmbralAlto != umbralAlto

                libreEmail = newEmail
                librePassword = newPassword
                umbralBajo = newUmbralBajo
                umbralAlto = newUmbralAlto

                // Actualizar líneas del gráfico si los umbrales cambiaron
                if (thresholdsChanged) updateChartLimitLines()

                // Si se pidió fetch inmediato o las credenciales cambiaron, actualizar glucosa
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
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(true)
                textColor = Color.GRAY
                textSize = 9f
                labelCount = 6
                granularity = 60f // mínimo 60 minutos entre etiquetas
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val ms = (value * 60_000L).toLong()
                        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.GRAY
                textSize = 9f
                axisMinimum = 40f
                axisMaximum = 300f
            }

            axisRight.isEnabled = false
        }

        // Añadir las líneas de umbral con los valores actuales
        updateChartLimitLines()
    }

    /** Elimina y vuelve a añadir las líneas de umbral en el gráfico con los valores actuales */
    private fun updateChartLimitLines() {
        glucoseChart.axisLeft.removeAllLimitLines()

        val rangeColor = Color.parseColor("#4CAF50")

        val lowLine = LimitLine(umbralBajo, "${umbralBajo.toInt()}").apply {
            lineWidth = 1.5f
            lineColor = rangeColor
            enableDashedLine(10f, 6f, 0f)
            textColor = rangeColor
            textSize = 9f
        }
        val highLine = LimitLine(umbralAlto, "${umbralAlto.toInt()}").apply {
            lineWidth = 1.5f
            lineColor = rangeColor
            enableDashedLine(10f, 6f, 0f)
            textColor = rangeColor
            textSize = 9f
        }

        glucoseChart.axisLeft.addLimitLine(lowLine)
        glucoseChart.axisLeft.addLimitLine(highLine)
        glucoseChart.axisLeft.setDrawLimitLinesBehindData(true)
        glucoseChart.invalidate()
    }

    private fun updateGlucoseChart(measurements: List<GlucoseMeasurement>) {
        if (measurements.isEmpty()) return

        // Intentar parsear el timestamp. LibreLinkUp devuelve "M/d/yyyy h:mm:ss a" en US locale.
        val parsers = listOf(
            SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US),
            SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        )

        fun parseTs(ts: String): Date? {
            for (parser in parsers) {
                try { return parser.parse(ts) } catch (_: Exception) {}
            }
            return null
        }

        // Construir entradas usando minutos desde epoch como X para que el formatter funcione
        val entries = mutableListOf<Entry>()
        measurements.forEachIndexed { index, m ->
            val date = parseTs(m.timestamp)
            val xVal = if (date != null) {
                (date.time / 60_000).toFloat()
            } else {
                index.toFloat() // fallback sin timestamps
            }
            entries.add(Entry(xVal, m.value.toFloat()))
        }

        // Si no se pudieron parsear los timestamps, ocultar etiquetas del eje X
        val timestampsParsed = parseTs(measurements.first().timestamp) != null
        glucoseChart.xAxis.setDrawLabels(timestampsParsed)

        val lineColor = Color.parseColor("#29B6F6")

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
        val diaMs = (diaHoras * 3_600_000).toLong()
        val cutoff = System.currentTimeMillis() - diaMs

        db.collection("users").document(userId)
            .collection("history")
            .whereGreaterThan("fecha", cutoff)
            .get()
            .addOnSuccessListener { documents ->
                val logs = documents.map { doc ->
                    BoloLog(
                        fecha = doc.getLong("fecha") ?: 0L,
                        dosisTotal = doc.getDouble("dosisTotal") ?: 0.0
                    )
                }
                val diaMinutos = (diaHoras * 60).toInt()
                val iob = IobCalculator.calcular(logs, diaMinutos)
                val iobRed = (iob * 10.0).roundToInt() / 10.0
                tvIOB.text = "$iobRed U"
            }
            .addOnFailureListener {
                tvIOB.text = "0.0 U"
            }
    }

    private fun updateGlucoseCard(glucosa: Double, stale: Boolean = false) {
        tvGlucosa.text = glucosa.roundToInt().toString()

        val color = when {
            stale -> android.R.color.darker_gray
            glucosa < umbralBajo -> android.R.color.holo_red_light
            glucosa > umbralAlto -> android.R.color.holo_orange_light
            else -> R.color.green_ok
        }

        tvGlucosa.setTextColor(ContextCompat.getColor(requireContext(), color))
    }
}
