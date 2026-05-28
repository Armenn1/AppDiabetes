package com.diabetly.app

data class FoodScanResult(
    val id: String = "",
    val fecha: Long = 0L,
    val comida: String = "",
    val descripcion: String = "",
    val carbohidratos: Double = 0.0,
    val fibra: Double = 0.0,
    val proteinas: Double = 0.0,
    val grasas: Double = 0.0,
    val carbohidratosNetos: Double = 0.0,
    val indiceGlucemico: Int = 0,
    val cargaGlucemica: Double = 0.0,
    val raciones: Double = 0.0,
    val confianza: String = "media",
    val glucosaAlEscanear: Int = 0,
    val tendenciaAlEscanear: Int = 0,
    val iobAlEscanear: Double = 0.0,
    val cobAlEscanear: Double = 0.0,
    val dosisCalculada: Double = 0.0,
    val tipoComida: String = "",
    val imagenUrl: String = ""
)
