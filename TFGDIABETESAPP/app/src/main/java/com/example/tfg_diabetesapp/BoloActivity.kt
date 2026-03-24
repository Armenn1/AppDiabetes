package com.example.tfg_diabetesapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class BoloActivity : AppCompatActivity() {

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variables médicas (Se rellenan desde Firebase)
    private var myRatio: Double = 0.0
    private var mySensibilidad: Double = 0.0
    // Objetivo personal (Por defecto 100 si falla la carga)
    private var myTarget: Double = 100.0

    private var dataLoaded = false // Semáforo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si no hay cuenta logeada manda a inicio de session (por el widget)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = android.content.Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        setContentView(R.layout.activity_bolo)

        // Referencias UI
        val etGlucosa = findViewById<EditText>(R.id.etGlucosaActual)
        val etRaciones = findViewById<EditText>(R.id.etRaciones)
        // Eliminamos etObjetivo: El usuario ya no tiene que escribirlo a mano

        val btnCalcular = findViewById<MaterialButton>(R.id.btnCalcular)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Referencias Resultado
        val cardResult = findViewById<MaterialCardView>(R.id.cardResult)
        val tvTotal = findViewById<TextView>(R.id.tvTotalDosis)
        val tvDesglose = findViewById<TextView>(R.id.tvDesglose)

        // 1. CARGAR DATOS NADA MÁS ENTRAR
        loadMedicalSettings()

        // 2. LÓGICA DEL BOTÓN CALCULAR
        btnCalcular.setOnClickListener {
            // Seguridad
            if (!dataLoaded) {
                Toast.makeText(this, "Cargando tus ajustes médicos...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sGlucosa = etGlucosa.text.toString()
            val sRaciones = etRaciones.text.toString()

            if (sGlucosa.isNotEmpty() && sRaciones.isNotEmpty()) {
                val glucosa = sGlucosa.toDouble()
                val raciones = sRaciones.toDouble()

                // --- A) SEGURIDAD: DETECTAR HIPOGLUCEMIA ---
                if (glucosa < 70) {
                    cardResult.visibility = View.VISIBLE
                    // Color Rojo Alerta
                    cardResult.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_light))

                    tvTotal.text = "HIPO"
                    tvTotal.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))

                    tvDesglose.text = "¡PELIGRO! Glucosa baja.\nIngiere azúcares rápidos y NO te inyectes."
                    tvDesglose.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))

                    // Guardamos el evento con 0 insulina por seguridad
                    saveLogToFirebase(glucosa, raciones, 0.0, 0.0, 0.0)

                    return@setOnClickListener // STOP
                }

                // --- B) CÁLCULO AVANZADO (Con Objetivo Personalizado) ---

                // 1. Restauramos colores (Blanco)
                cardResult.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
                tvTotal.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.black))
                tvDesglose.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.darker_gray))

                // 2. LA FÓRMULA INTELIGENTE
                val insuComida = raciones * myRatio

                // Corrección: (Glucosa - Objetivo) / Sensibilidad
                // NOTA: Permitimos negativos. Si estás en 90 y tu objetivo es 110, restará insulina.
                val insuCorreccion = (glucosa - myTarget) / mySensibilidad

                // Suma total
                var total = insuComida + insuCorreccion

                // SEGURIDAD FINAL: La insulina nunca puede ser negativa
                if (total < 0) total = 0.0

                // Redondeos (1 decimal)
                val totalRedondeado = (total * 10.0).roundToInt() / 10.0
                val comidaRed = (insuComida * 10.0).roundToInt() / 10.0
                val correccionRed = (insuCorreccion * 10.0).roundToInt() / 10.0

                // 3. Mostrar
                tvTotal.text = "$totalRedondeado U"

                // Explicación dinámica (para que el usuario entienda si le restamos insulina)
                if (correccionRed < 0) {
                    tvDesglose.text = "Comida ($comidaRed) - Resta (${kotlin.math.abs(correccionRed)})"
                } else {
                    tvDesglose.text = "Comida ($comidaRed) + Corrección ($correccionRed)"
                }

                cardResult.visibility = View.VISIBLE

                // --- C) GUARDAR ---
                saveLogToFirebase(glucosa, raciones, comidaRed, correccionRed, totalRedondeado)

            } else {
                Toast.makeText(this, "Rellena glucosa y raciones", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Volver atrás
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadMedicalSettings() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ratio = document.getDouble("factorHC")
                    val sensi = document.getDouble("sensibilidad")
                    // CARGAMOS EL OBJETIVO (Si no existe, usamos 100 por defecto)
                    val target = document.getDouble("target") ?: 100.0

                    if (ratio != null && sensi != null) {
                        myRatio = ratio
                        mySensibilidad = sensi
                        myTarget = target // Guardamos el objetivo
                        dataLoaded = true
                    } else {
                        Toast.makeText(this, "¡Faltan configurar tus Ajustes!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Ve a Ajustes para configurar tus ratios", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveLogToFirebase(glucosa: Double, raciones: Double, comida: Double, correccion: Double, total: Double) {
        val userId = auth.currentUser?.uid ?: return


        val nuevoLog = BoloLog(
            glucosa = glucosa,
            raciones = raciones,
            dosisComida = comida,
            dosisCorreccion = correccion,
            dosisTotal = total,
            fecha = System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("history")
            .add(nuevoLog)
    }
}