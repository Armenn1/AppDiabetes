package com.example.tfg_diabetesapp

data class ActivityLog(
    val id: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val fechaFin: Long? = null,
    val duracionMinutos: Int = 0,
    val tipo: String = "aerobico_leve",
    val intensidad: Int = 5,
    val descripcion: String = "",
    val glucosaInicial: Int = 0,
    val iobAlEmpezar: Double = 0.0,
    val efectoResidualHoras: Double = 24.0
)
