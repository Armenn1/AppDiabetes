package com.example.tfg_diabetesapp

import kotlin.math.sqrt

data class GlucoseStats(
    val tir: Double,
    val tbr: Double,
    val tbrCritico: Double,
    val tar: Double,
    val tarCritico: Double,
    val media: Double,
    val desviacion: Double,
    val cv: Double,
    val gmi: Double,
    val totalLecturas: Int
)

object GlucoseStatsCalculator {
    fun calcular(lecturas: List<Int>, objetivoMin: Double, objetivoMax: Double): GlucoseStats {
        if (lecturas.isEmpty()) {
            return GlucoseStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
        }

        val total = lecturas.size
        val enRango = lecturas.count { it in objetivoMin.toInt()..objetivoMax.toInt() }
        val bajoRango = lecturas.count { it < objetivoMin }
        val bajoCritico = lecturas.count { it < 54 }
        val sobreRango = lecturas.count { it > objetivoMax }
        val sobreCritico = lecturas.count { it > 250 }

        val media = lecturas.sumOf { it.toDouble() } / total
        val varianza = lecturas.sumOf { (it - media) * (it - media) } / total
        val sd = sqrt(varianza)
        val cv = if (media > 0) (sd / media) * 100.0 else 0.0
        val gmi = 3.31 + (0.02392 * media)

        return GlucoseStats(
            tir = enRango * 100.0 / total,
            tbr = bajoRango * 100.0 / total,
            tbrCritico = bajoCritico * 100.0 / total,
            tar = sobreRango * 100.0 / total,
            tarCritico = sobreCritico * 100.0 / total,
            media = media,
            desviacion = sd,
            cv = cv,
            gmi = gmi,
            totalLecturas = total
        )
    }
}
