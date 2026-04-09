package com.example.tfg_diabetesapp

data class BoloLog(
    val fecha: Long = System.currentTimeMillis(), // Timestamp actual
    val glucosa: Double = 0.0,
    val raciones: Double = 0.0,
    val dosisComida: Double = 0.0,
    val dosisCorreccion: Double = 0.0,
    val dosisTotal: Double = 0.0,
    val iobDescontado: Double = 0.0, // IOB restado al calcular el bolo
    val tipo: String = "Calculadora" // Por si luego añades entradas manuales
)