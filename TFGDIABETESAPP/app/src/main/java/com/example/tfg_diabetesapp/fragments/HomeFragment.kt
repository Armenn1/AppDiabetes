package com.example.tfg_diabetesapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tfg_diabetesapp.BoloActivity
import com.example.tfg_diabetesapp.LoginActivity
import com.example.tfg_diabetesapp.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables UI globales
    private lateinit var tvGlucosa: TextView
    private lateinit var tvIOB: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Inflar la vista (crear el trozo de pantalla desde el XML)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 2. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 3. REFERENCIAS UI (Fíjate que usamos 'view.findViewById')
        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val cardNewBolo = view.findViewById<MaterialCardView>(R.id.cardNewBolo)

        tvGlucosa = view.findViewById(R.id.tvGlucosaMain)
        tvIOB = view.findViewById(R.id.tvIOB)

        // 4. NAVEGACIÓN A LA CALCULADORA
        cardNewBolo.setOnClickListener {
            startActivity(Intent(requireContext(), BoloActivity::class.java))
        }

        // 5. CERRAR SESIÓN
        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // Borramos el historial de pantallas para que no pueda volver con el botón 'Atrás'
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish() // Cerramos la MainActivity que contiene el Fragment
        }

        return view
    }

    // --- SE EJECUTA AL VOLVER A LA PESTAÑA (Ej: después de calcular un bolo) ---
    override fun onResume() {
        super.onResume()
        refreshDashboardData()
    }

    private fun refreshDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        // Consulta: Descargar solo el último registro
        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastLog = documents.documents[0]

                    val glucosa = lastLog.getDouble("glucosa") ?: 0.0
                    val dosisTotal = lastLog.getDouble("dosisTotal") ?: 0.0
                    val fechaLog = lastLog.getLong("fecha") ?: System.currentTimeMillis()

                    updateGlucoseCard(glucosa)
                    calculateAndShowIOB(dosisTotal, fechaLog)
                } else {
                    tvGlucosa.text = "--"
                    tvIOB.text = "0.0 U"
                }
            }
            .addOnFailureListener {
                // Silencioso si falla la red
            }
    }

    private fun updateGlucoseCard(glucosa: Double) {
        tvGlucosa.text = glucosa.roundToInt().toString()

        val color = when {
            glucosa < 70 -> android.R.color.holo_red_light
            glucosa > 180 -> android.R.color.holo_orange_light
            else -> android.R.color.darker_gray // Un color neutro/verde para estar en rango
        }

        // Aplicamos el color (usamos requireContext() en lugar de 'this')
        tvGlucosa.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun calculateAndShowIOB(dosis: Double, fechaLog: Long) {
        val ahora = System.currentTimeMillis()
        val diferenciaMillis = ahora - fechaLog
        val minutosPasados = TimeUnit.MILLISECONDS.toMinutes(diferenciaMillis)

        // Modelo de IOB lineal (4 horas = 240 mins)
        if (minutosPasados >= 240) {
            tvIOB.text = "0.0 U"
        } else {
            val factorRestante = 1.0 - (minutosPasados.toDouble() / 240.0)
            var iob = dosis * factorRestante
            if (iob < 0) iob = 0.0

            val iobRedondeado = (iob * 10.0).roundToInt() / 10.0
            tvIOB.text = "$iobRedondeado U"
        }
    }
}