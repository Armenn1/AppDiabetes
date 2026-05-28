package com.diabetly.app

object CarbsCalculator {
    /**
     * Calcula el COB (Carbs On Board) sumando los carbohidratos activos
     * de cada mealScan reciente, con ventana de absorción variable según grasa.
     */
    fun calcular(scans: List<FoodScanResult>): Double {
        val ahora = System.currentTimeMillis()
        return scans.sumOf { scan ->
            val absorcionMinutos = when {
                scan.grasas > 30 -> 270.0   // fenómeno pizza: >30g grasa → 4.5h
                scan.grasas > 15 -> 210.0   // grasa moderada → 3.5h
                else             -> 180.0   // normal → 3h
            }
            val minutos = (ahora - scan.fecha) / 60_000.0
            val factor = 1.0 - (minutos / absorcionMinutos)
            if (factor > 0) scan.carbohidratosNetos * factor else 0.0
        }
    }
}
