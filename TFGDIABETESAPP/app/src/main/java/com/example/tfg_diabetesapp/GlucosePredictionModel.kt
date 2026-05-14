package com.example.tfg_diabetesapp

object GlucosePredictionModel {

    /**
     * Predice la curva de glucosa para los próximos minutosMax minutos
     * usando el estado actual del usuario (glucosa, IOB y COB).
     *
     * Modelo: glucosa(t) = glucosaActual + subidaCarbos(t) - bajadaInsulina(t)
     *
     * - subidaCarbos(t):   los gramos de COB restantes se absorberán linealmente
     *                       en una ventana variable según las grasas (fenómeno pizza).
     *                       Conversión a mg/dL: raciones × factorHC × sensibilidad.
     * - bajadaInsulina(t): el IOB restante actuará linealmente sobre la duración
     *                       DIA. Bajada total: iobActual × sensibilidad mg/dL.
     *
     * @return lista de pares (minutosDesdeAhora, glucosaPredicha mg/dL) cada 15 min
     */
    fun predecir(
        glucosaActual: Double,
        iobActual: Double,
        cobGramos: Double,
        grasasUltimaComida: Double,
        sensibilidad: Double,
        factorHC: Double,
        diaMinutos: Int,
        minutosMax: Int = 180
    ): List<Pair<Int, Double>> {
        val ventanaCob = when {
            grasasUltimaComida > 30 -> 270.0
            grasasUltimaComida > 15 -> 210.0
            else                    -> 180.0
        }

        val raciones = cobGramos / 10.0
        val subidaTotalRestante = raciones * factorHC * sensibilidad
        val bajadaTotalRestante = iobActual * sensibilidad

        val resultado = mutableListOf<Pair<Int, Double>>()
        var t = 0
        while (t <= minutosMax) {
            val pctCarbos   = minOf(1.0, t.toDouble() / ventanaCob)
            val pctInsulina = minOf(1.0, t.toDouble() / diaMinutos)
            val glucosa = glucosaActual +
                          (subidaTotalRestante * pctCarbos) -
                          (bajadaTotalRestante * pctInsulina)
            resultado.add(t to glucosa.coerceIn(40.0, 400.0))
            t += 15
        }
        return resultado
    }
}
