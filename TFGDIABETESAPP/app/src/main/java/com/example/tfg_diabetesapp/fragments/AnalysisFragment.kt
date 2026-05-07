package com.example.tfg_diabetesapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tfg_diabetesapp.GlucoseStats
import com.example.tfg_diabetesapp.GlucoseStatsCalculator
import com.example.tfg_diabetesapp.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AnalysisFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var chipGroupPeriodo: ChipGroup
    private lateinit var pieChart: PieChart
    private lateinit var tvTotalLecturas: TextView
    private lateinit var tvTirBig: TextView
    private lateinit var progressTir: ProgressBar
    private lateinit var tvTbr: TextView
    private lateinit var tvTar: TextView
    private lateinit var tvMedia: TextView
    private lateinit var tvSd: TextView
    private lateinit var tvCv: TextView
    private lateinit var tvGmi: TextView
    private lateinit var tvGmiVsObjetivo: TextView

    private var rangoMin: Double = 70.0
    private var rangoMax: Double = 180.0
    private var objetivoHbA1c: Double? = null

    private var diasSeleccionados: Int = 7

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        chipGroupPeriodo = view.findViewById(R.id.chipGroupPeriodo)
        pieChart = view.findViewById(R.id.pieChartTir)
        tvTotalLecturas = view.findViewById(R.id.tvTotalLecturas)
        tvTirBig = view.findViewById(R.id.tvTirBig)
        progressTir = view.findViewById(R.id.progressTir)
        tvTbr = view.findViewById(R.id.tvTbr)
        tvTar = view.findViewById(R.id.tvTar)
        tvMedia = view.findViewById(R.id.tvMedia)
        tvSd = view.findViewById(R.id.tvSd)
        tvCv = view.findViewById(R.id.tvCv)
        tvGmi = view.findViewById(R.id.tvGmi)
        tvGmiVsObjetivo = view.findViewById(R.id.tvGmiVsObjetivo)

        setupPieChart()

        chipGroupPeriodo.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            diasSeleccionados = when (id) {
                R.id.chip7d -> 7
                R.id.chip14d -> 14
                R.id.chip30d -> 30
                else -> 7
            }
            cargarLecturas()
        }

        loadProfileAndCompute()
        return view
    }

    private fun loadProfileAndCompute() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                rangoMin = doc.getDouble("umbralBajo") ?: 70.0
                rangoMax = doc.getDouble("umbralAlto") ?: 180.0
                objetivoHbA1c = doc.getDouble("objetivoHbA1c")
                cargarLecturas()
            }
            .addOnFailureListener { cargarLecturas() }
    }

    private fun cargarLecturas() {
        val userId = auth.currentUser?.uid ?: return
        val cutoff = System.currentTimeMillis() - (diasSeleccionados * 24L * 60L * 60L * 1000L)
        db.collection("users").document(userId)
            .collection("glucoseReadings")
            .whereGreaterThan("fecha", cutoff)
            .get()
            .addOnSuccessListener { docs ->
                val valores = docs.mapNotNull { it.getLong("valor")?.toInt() }
                val stats = GlucoseStatsCalculator.calcular(valores, rangoMin, rangoMax)
                renderStats(stats)
            }
            .addOnFailureListener {
                renderStats(GlucoseStatsCalculator.calcular(emptyList(), rangoMin, rangoMax))
            }
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            isRotationEnabled = false
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 65f
            transparentCircleRadius = 68f
            setCenterTextSize(20f)
            setCenterTextColor(Color.parseColor("#1A1C1E"))
        }
    }

    private fun renderStats(stats: GlucoseStats) {
        tvTotalLecturas.text = "${stats.totalLecturas} lecturas"
        tvTirBig.text = "%.0f%%".format(stats.tir)
        progressTir.progress = stats.tir.toInt()
        tvTbr.text = "%.0f%%".format(stats.tbr)
        tvTar.text = "%.0f%%".format(stats.tar)
        tvMedia.text = "%.0f".format(stats.media)
        tvSd.text = "%.0f".format(stats.desviacion)
        val cvText = "%.1f%%".format(stats.cv)
        tvCv.text = if (stats.cv > 36.0) "$cvText ⚠️" else cvText
        tvGmi.text = "%.1f%%".format(stats.gmi)

        tvGmiVsObjetivo.text = if (objetivoHbA1c != null) {
            "Tu HbA1c estimada: %.1f%% · Objetivo: %.1f%%".format(stats.gmi, objetivoHbA1c!!)
        } else {
            "Tu HbA1c estimada: %.1f%%".format(stats.gmi)
        }

        renderPie(stats)
    }

    private fun renderPie(stats: GlucoseStats) {
        if (stats.totalLecturas == 0) {
            pieChart.clear()
            pieChart.centerText = "0%"
            pieChart.invalidate()
            return
        }
        val entries = listOf(
            PieEntry(stats.tir.toFloat(), "TIR"),
            PieEntry(stats.tbr.toFloat(), "TBR"),
            PieEntry(stats.tar.toFloat(), "TAR")
        )
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#EF5350"),
                Color.parseColor("#FF9800")
            )
            setDrawValues(false)
            sliceSpace = 2f
        }
        pieChart.data = PieData(dataSet)
        pieChart.centerText = "%.0f%%".format(stats.tir)
        pieChart.invalidate()
    }
}
