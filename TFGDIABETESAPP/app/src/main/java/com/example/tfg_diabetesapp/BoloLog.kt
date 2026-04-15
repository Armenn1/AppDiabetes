package com.example.tfg_diabetesapp

data class BoloLog(
    val fecha: Long = System.currentTimeMillis(),
    val glucosa: Double = 0.0,
    val raciones: Double = 0.0,
    val dosisComida: Double = 0.0,
    val dosisCorreccion: Double = 0.0,
    val dosisTotal: Double = 0.0,
    val iobDescontado: Double = 0.0,
    val tipo: String = "Calculadora", // "Calculadora" | "Insulina" | "Comida"
    val nota: String = "",            // Nota libre (Insulina / Comida)
    val gramos: Double = 0.0          // Gramos de HC (Comida)
)