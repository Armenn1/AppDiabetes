package com.example.tfg_diabetesapp  //

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables UI globales
    private lateinit var tvGlucosa: TextView
    private lateinit var tvIOB: TextView
    private lateinit var tvEstado: TextView // El texto que dice "En Rango"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- REFERENCIAS UI ---
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        val cardNewBolo = findViewById<MaterialCardView>(R.id.cardNewBolo)
        val cardSettings = findViewById<MaterialCardView>(R.id.cardSettings)
        val cardHistory = findViewById<MaterialCardView>(R.id.cardHistory)

        tvGlucosa = findViewById(R.id.tvGlucosaMain)
        tvIOB = findViewById(R.id.tvIOB)
        // Busca el TextView pequeño debajo de la glucosa (si no tiene ID, ponle uno en el XML: tvEstadoGlucosa)
        // Por ahora lo simulamos si no tienes ID, o lo dejamos pendiente.

        // --- NAVEGACIÓN ---
        cardNewBolo.setOnClickListener {
            startActivity(Intent(this, BoloActivity::class.java))
        }

        cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        cardHistory.setOnClickListener {
            // Abrimos la pantalla de Historial
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- MAGIA: ESTO SE EJECUTA CADA VEZ QUE VUELVES A ESTA PANTALLA ---
    override fun onResume() {
        super.onResume()
        refreshDashboardData()
    }

    private fun refreshDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        // Consulta: Dame el historial, ordenado por fecha (descendente), solo 1 (el último)
        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Tenemos datos reales
                    val lastLog = documents.documents[0]

                    val glucosa = lastLog.getDouble("glucosa") ?: 0.0
                    val dosisTotal = lastLog.getDouble("dosisTotal") ?: 0.0
                    val fechaLog = lastLog.getLong("fecha") ?: System.currentTimeMillis()

                    // 1. ACTUALIZAR TARJETA GLUCOSA
                    updateGlucoseCard(glucosa)

                    // 2. CALCULAR Y MOSTRAR IOB (Insulina Activa)
                    calculateAndShowIOB(dosisTotal, fechaLog)

                } else {
                    // Usuario nuevo sin historial
                    tvGlucosa.text = "--"
                    tvIOB.text = "0.0 U"
                }
            }
            .addOnFailureListener {
                // Si falla (ej: sin internet), no hacemos nada o mostramos error discreto
            }
    }

    private fun updateGlucoseCard(glucosa: Double) {
        // Redondeamos para quitar decimales feos en la pantalla principal
        tvGlucosa.text = glucosa.roundToInt().toString()

        // Lógica de colores semafóricos (Extra de profesionalidad)
        val color = when {
            glucosa < 70 -> android.R.color.holo_red_light // Hipo
            glucosa > 180 -> android.R.color.holo_orange_light // Hiper
            else -> R.color.green_ok // Tendrías que definir este color o usar uno por defecto
        }

        //  cambiar el color del texto según el valor:
        // tvGlucosa.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun calculateAndShowIOB(dosis: Double, fechaLog: Long) {
        val ahora = System.currentTimeMillis()
        val diferenciaMillis = ahora - fechaLog

        // Pasamos a minutos
        val minutosPasados = TimeUnit.MILLISECONDS.toMinutes(diferenciaMillis)

        // MODELO SIMPLIFICADO DE IOB (Lineal 4 horas)
        // La insulina dura 4 horas (240 minutos).
        // Fórmula: Dosis * (1 - (minutosPasados / 240))

        if (minutosPasados >= 240) {
            // Han pasado más de 4 horas, ya no queda insulina activa
            tvIOB.text = "0.0 U"
        } else {
            val factorRestante = 1.0 - (minutosPasados.toDouble() / 240.0)
            var iob = dosis * factorRestante

            if (iob < 0) iob = 0.0 // Por seguridad

            // Redondeo a 1 decimal
            val iobRedondeado = (iob * 10.0).roundToInt() / 10.0
            tvIOB.text = "$iobRedondeado U"
        }
    }
}