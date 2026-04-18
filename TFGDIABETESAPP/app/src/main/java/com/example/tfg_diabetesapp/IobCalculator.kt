package com.example.tfg_diabetesapp

object IobCalculator {
    /**
     * Calcula el IOB (Insulin On Board) total sumando el IOB restante
     * de cada bolo registrado dentro del período DIA.
     *
     * @param logs     Lista de registros de bolo activos (dentro del período DIA)
     * @param diaMinutos  Duración de acción de la insulina en minutos (ej: 240 para 4h)
     */
    fun calcular(logs: List<BoloLog>, diaMinutos: Int): Double {
        val ahora = System.currentTimeMillis()
        return logs.sumOf { log ->
            val dosis = maxOf(0.0, log.dosisTotal)
            val minutosDesde = (ahora - log.fecha) / 60_000.0
            val factorRestante = 1.0 - (minutosDesde / diaMinutos)
            if (factorRestante > 0) dosis * factorRestante else 0.0
        }
    }
}

