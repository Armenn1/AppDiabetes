package com.diabetly.app

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
    val gramos: Double = 0.0,         // Gramos de HC (Comida)
    val tendencia: Int = 0,           // flecha tendencia glucosa API LibreLinkUp (1=baja rápido … 3=estable … 5=sube rápido; 0 = desconocida)
    val cobAlCalcular: Double = 0.0,  // Carbs On Board activos (0.0 hasta implementar scanner)
    val tipoComida: String = "",      // "desayuno"|"comida"|"cena"|"snack"|"merienda"
    val perfilHoraInicio: Int? = null,// horaInicio del PerfilHorario activo (null = perfil global)
    val glucosaPost2h: Int? = null,   // se rellenará en fases futuras para drift detection
    val mealScanId: String = "",      // id del mealScan vinculado (scanner)
    val imagenUrl: String = "",       // ruta local (cache) o URL de la foto del plato
    val proteinas: Double = 0.0,      // info nutricional (tipo="Comida")
    val grasas: Double = 0.0,
    val fibra: Double = 0.0,
    val indiceGlucemico: Int = 0
)