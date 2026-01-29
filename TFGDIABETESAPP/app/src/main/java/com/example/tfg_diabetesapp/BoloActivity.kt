package com.example.tfg_diabetesapp //

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class BoloActivity : AppCompatActivity() {

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variables médicas (Se rellenarán desde Firebase)
    private var myRatio: Double = 0.0
    private var mySensibilidad: Double = 0.0
    private var dataLoaded = false // Semáforo para saber si ya tenemos los datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bolo)

        // Referencias UI
        val etGlucosa = findViewById<EditText>(R.id.etGlucosaActual)
        val etRaciones = findViewById<EditText>(R.id.etRaciones)
        val btnCalcular = findViewById<MaterialCardView>(R.id.btnCalcular)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Referencias Resultado
        val cardResult = findViewById<MaterialCardView>(R.id.cardResult)
        val tvTotal = findViewById<TextView>(R.id.tvTotalDosis)
        val tvDesglose = findViewById<TextView>(R.id.tvDesglose)

        // 1. CARGAR DATOS NADA MÁS ENTRAR
        loadMedicalSettings()

        // 2. LÓGICA DEL BOTÓN CALCULAR

        btnCalcular.setOnClickListener {
            // Seguridad: ¿Tenemos los ajustes cargados?
            if (!dataLoaded) {
                Toast.makeText(this, "Cargando ajustes...", Toast.LENGTH_SHORT).show()
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

                    // Poner tarjeta en ROJO (Alerta)
                    // Usamos ContextCompat para que no de errores de versión
                    cardResult.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_light))

                    tvTotal.text = "HIPO"
                    tvTotal.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))

                    tvDesglose.text = "¡PELIGRO! Glucosa muy baja.\nIngiere azúcares rápidos y NO te inyectes."
                    tvDesglose.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))

                    return@setOnClickListener // ¡PARAMOS AQUÍ! No guardamos ni calculamos.
                }

                // --- B) CÁLCULO NORMAL (Si no hay peligro) ---

                // 1. Restauramos colores normales (Blanco y texto oscuro)
                cardResult.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
                tvTotal.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.black))
                tvDesglose.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.darker_gray))

                // 2. La Fórmula
                val insuComida = raciones * myRatio
                var insuCorreccion = (glucosa - 100) / mySensibilidad

                // Si la corrección es negativa (ej: glucosa 90), la ponemos a 0 para no restar insulina de la comida
                if (insuCorreccion < 0) insuCorreccion = 0.0

                val total = insuComida + insuCorreccion

                // Redondeos bonitos
                val totalRedondeado = (total * 10.0).roundToInt() / 10.0
                val comidaRed = (insuComida * 10.0).roundToInt() / 10.0
                val correccionRed = (insuCorreccion * 10.0).roundToInt() / 10.0

                // 3. Mostrar en pantalla
                tvTotal.text = "$totalRedondeado U"
                tvDesglose.text = "Comida ($comidaRed) + Corrección ($correccionRed)"
                cardResult.visibility = View.VISIBLE

                // --- C) GUARDAR EN EL HISTORIAL ---
                // Aquí llamamos a la función que vamos a crear en el paso 2
                saveLogToFirebase(glucosa, raciones, comidaRed, correccionRed, totalRedondeado)

            } else {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Volver atrás
        btnBack.setOnClickListener {
            finish()
        }
    }

    // Función auxiliar para bajar los datos de Firestore
    private fun loadMedicalSettings() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ratio = document.getDouble("factorHC")
                    val sensi = document.getDouble("sensibilidad")

                    if (ratio != null && sensi != null) {
                        myRatio = ratio
                        mySensibilidad = sensi
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
    // Esta función recibe los datos y usa tu Data Class "BoloLog" para subirlos a Firebase
    private fun saveLogToFirebase(glucosa: Double, raciones: Double, comida: Double, correccion: Double, total: Double) {
        val userId = auth.currentUser?.uid ?: return

        // Creamos el objeto usando la clase que creaste antes
        val nuevoLog = BoloLog(
            // La fecha se pone sola porque lo definiste así en el data class (System.currentTimeMillis())
            glucosa = glucosa,
            raciones = raciones,
            dosisComida = comida,
            dosisCorreccion = correccion,
            dosisTotal = total
        )

        // Subimos a Firebase: users -> [ID] -> history -> [Nuevo Documento]
        db.collection("users").document(userId)
            .collection("history") // Nueva sub-colección automática
            .add(nuevoLog)
            .addOnSuccessListener {
                // Confirmación visual discreta
                // Opcional: Toast.makeText(this, "Guardado en historial", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar historial", Toast.LENGTH_SHORT).show()
            }
    }
}