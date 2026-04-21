package com.example.tfg_diabetesapp

import kotlin.math.exp

/**
 * IOB (Insulin On Board) con modelo bi-exponencial estilo Loop/OpenAPS.
 *
 * El modelo lineal (1 − t/DIA) sobrestima IOB al inicio y lo subestima al final,
 * provocando stacking de bolos e hipos tardías. Esta curva respeta la
 * farmacocinética real de los análogos rápidos subcutáneos:
 *   · Actividad ~0 en t=0
 *   · Pico a τ minutos (típicamente 55–75)
 *   · Cola residual hasta DIA (típicamente 300–360 min)
 *
 * Referencias: Dragan Maksimovic / Pete Schwartz (Loop), OpenAPS docs.
 *
 * @param logs         Lista de bolos activos (fecha + dosisTotal).
 * @param diaMinutos   DIA en minutos (Duration of Insulin Action). Default 300 (5 h).
 * @param tauMinutos   Tiempo al pico de actividad. Default 75 (análogos rápidos);
 *                     usar 55 para ultra-rápidas (Fiasp, Lyumjev).
 * @return IOB total en unidades de insulina aún activas.
 */
object IobCalculator {

    fun calcular(
        logs: List<BoloLog>,
        diaMinutos: Int,
        tauMinutos: Int = 75
    ): Double {
        val ahora = System.currentTimeMillis()
        val dia = diaMinutos.toDouble()
        val tau = tauMinutos.toDouble()
        return logs.sumOf { log ->
            val dosis = maxOf(0.0, log.dosisTotal)
            val t = (ahora - log.fecha) / 60_000.0
            if (t < 0 || t >= dia) 0.0
            else dosis * fraccionRestante(t, dia, tau)
        }
    }

    /**
     * Fracción de la dosis aún activa en el tiempo t (0.0–1.0).
     * Curva bi-exponencial cerrada de Loop (una sola inyección, subcutánea).
     */
    private fun fraccionRestante(t: Double, dia: Double, tau: Double): Double {
        // Guardarraíl: τ debe ser < DIA/2 para que la curva tenga pico real.
        val tauSafe = tau.coerceAtMost(dia / 2.0 - 1.0)

        val tp = tauSafe * (1.0 - tauSafe / dia) / (1.0 - 2.0 * tauSafe / dia)
        val a  = 2.0 * tp / (dia * dia)
        val s  = 1.0 / (1.0 - a + (1.0 + a) * exp(-dia / tp))

        val iob = 1.0 - s * (1.0 - a) * (
            ((t * t / (tauSafe * dia * (1.0 - a))) - t / dia - 1.0) * exp(-t / tp) + 1.0
        )
        return iob.coerceIn(0.0, 1.0)
    }
}
