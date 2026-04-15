package com.example.tfg_diabetesapp

import java.util.Calendar

data class PerfilHorario(
    val horaInicio: Int = 0,       // hora del día, 0-23
    val horaFin: Int = 6,          // hora del día, 0-23 (exclusive: [inicio, fin))
    val factorHC: Double = 0.0,    // U por ración
    val sensibilidad: Double = 0.0 // mg/dL por U
) {
    /** "08:00 - 14:00" */
    fun rangoTexto(): String = "%02d:00 - %02d:00".format(horaInicio, horaFin)

    /** True si la hora actual cae dentro del rango de este perfil */
    fun esActivo(): Boolean {
        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (horaInicio < horaFin) {
            horaActual >= horaInicio && horaActual < horaFin
        } else {
            // Perfil nocturno (ej: 22 → 06)
            horaActual >= horaInicio || horaActual < horaFin
        }
    }

    companion object {
        /** Devuelve el perfil activo de la lista, o null si ninguno aplica */
        fun getActivo(perfiles: List<PerfilHorario>): PerfilHorario? =
            perfiles.firstOrNull { it.esActivo() }

        /** Convierte un map de Firestore en PerfilHorario */
        fun fromMap(map: Map<*, *>): PerfilHorario = PerfilHorario(
            horaInicio = (map["horaInicio"] as? Long)?.toInt() ?: 0,
            horaFin = (map["horaFin"] as? Long)?.toInt() ?: 6,
            factorHC = (map["factorHC"] as? Double) ?: 0.0,
            sensibilidad = (map["sensibilidad"] as? Double) ?: 0.0
        )

        /** Convierte un PerfilHorario en map para Firestore */
        fun toMap(p: PerfilHorario): Map<String, Any> = mapOf(
            "horaInicio" to p.horaInicio,
            "horaFin" to p.horaFin,
            "factorHC" to p.factorHC,
            "sensibilidad" to p.sensibilidad
        )
    }
}
