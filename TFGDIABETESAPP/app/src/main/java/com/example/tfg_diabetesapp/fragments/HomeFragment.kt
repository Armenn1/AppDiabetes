package com.example.tfg_diabetesapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg_diabetesapp.BoloActivity
import com.example.tfg_diabetesapp.LoginActivity
import com.example.tfg_diabetesapp.R
import com.example.tfg_diabetesapp.glucose.LibreLinkUpRepository
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvGlucosa: TextView
    private lateinit var tvIOB: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var pbGlucoseLoading: ProgressBar

    // Credenciales LibreLinkUp (se cargan desde Firestore)
    private var libreEmail = ""
    private var librePassword = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        val cardNewBolo = view.findViewById<MaterialCardView>(R.id.cardNewBolo)
        tvGlucosa = view.findViewById(R.id.tvGlucosaMain)
        tvIOB = view.findViewById(R.id.tvIOB)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        pbGlucoseLoading = view.findViewById(R.id.pbGlucoseLoading)

        cardNewBolo.setOnClickListener {
            startActivity(Intent(requireContext(), BoloActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Cargar credenciales y lanzar fetch inmediato al arrancar
        loadLibreCredentials(fetchAfterLoad = true)

        // Bucle de auto-actualización: fetch glucosa cada 60 segundos
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                fetchGlucoseFromLibre()
                delay(60_000L)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Recargar credenciales LibreLinkUp por si el usuario las cambió en Ajustes
        loadLibreCredentials()
        // Actualizar IOB al volver (ej: después de calcular un bolo)
        refreshIobData()
    }

    private fun loadLibreCredentials(fetchAfterLoad: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val newEmail = document.getString("libreEmail") ?: ""
                val newPassword = document.getString("librePassword") ?: ""
                val credentialsChanged = newEmail != libreEmail || newPassword != librePassword
                libreEmail = newEmail
                librePassword = newPassword
                // Si se pidió fetch inmediato o las credenciales cambiaron, actualizar glucosa ahora
                if ((fetchAfterLoad || credentialsChanged) && libreEmail.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        fetchGlucoseFromLibre()
                    }
                }
            }
    }

    private suspend fun fetchGlucoseFromLibre() {
        if (libreEmail.isEmpty() || librePassword.isEmpty()) {
            withContext(Dispatchers.Main) {
                tvGlucosa.text = "--"
                tvLastUpdated.text = "Configura tu cuenta LibreLinkUp en Ajustes"
            }
            return
        }

        withContext(Dispatchers.Main) {
            pbGlucoseLoading.visibility = View.VISIBLE
        }

        val glucoseResult = withContext(Dispatchers.IO) {
            LibreLinkUpRepository.getLatestGlucose(libreEmail, librePassword)
        }

        withContext(Dispatchers.Main) {
            pbGlucoseLoading.visibility = View.GONE
            if (glucoseResult != null) {
                updateGlucoseCard(glucoseResult.toDouble())
                val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                tvLastUpdated.text = "Última act. $hora"
            } else {
                tvGlucosa.text = "--"
                tvLastUpdated.text = "Error de conexión"
            }
        }
    }

    private fun refreshIobData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastLog = documents.documents[0]
                    val dosisTotal = lastLog.getDouble("dosisTotal") ?: 0.0
                    val fechaLog = lastLog.getLong("fecha") ?: System.currentTimeMillis()
                    calculateAndShowIOB(dosisTotal, fechaLog)
                } else {
                    tvIOB.text = "0.0 U"
                }
            }
    }

    private fun updateGlucoseCard(glucosa: Double) {
        tvGlucosa.text = glucosa.roundToInt().toString()

        val color = when {
            glucosa < 70 -> android.R.color.holo_red_light
            glucosa > 180 -> android.R.color.holo_orange_light
            else -> android.R.color.darker_gray
        }

        tvGlucosa.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun calculateAndShowIOB(dosis: Double, fechaLog: Long) {
        val ahora = System.currentTimeMillis()
        val minutosPasados = TimeUnit.MILLISECONDS.toMinutes(ahora - fechaLog)

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
