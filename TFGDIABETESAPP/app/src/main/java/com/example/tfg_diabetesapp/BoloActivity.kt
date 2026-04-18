package com.example.tfg_diabetesapp

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg_diabetesapp.widgets.MedicalHeaderView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class BoloActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // AdMob
    private var interstitialAd: InterstitialAd? = null

    // Ajustes médicos globales (fallback cuando no hay perfil activo)
    private var myRatio: Double = 0.0
    private var mySensibilidad: Double = 0.0
    private var myObjetivoMin: Double = 70.0
    private var myObjetivoMax: Double = 180.0
    private var myDia: Double = 4.0

    // Perfiles horarios del usuario
    private var perfiles: List<PerfilHorario> = emptyList()

    // IOB activo al abrir la calculadora
    private var currentIob: Double = 0.0

    private var dataLoaded = false

    // ── Ratio y sensibilidad efectivos (perfil activo o globales) ────────────
    private val effectiveRatio get() = PerfilHorario.getActivo(perfiles)?.factorHC ?: myRatio
    private val effectiveSensibilidad get() = PerfilHorario.getActivo(perfiles)?.sensibilidad ?: mySensibilidad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(android.content.Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_bolo)

        MobileAds.initialize(this)
        loadInterstitialAd()

        val etGlucosa = findViewById<EditText>(R.id.etGlucosaActual)
        val etRaciones = findViewById<EditText>(R.id.etRaciones)
        val btnCalcular = findViewById<MaterialButton>(R.id.btnCalcular)
        val boloHeader = findViewById<MedicalHeaderView>(R.id.boloHeader)
        val cardResult = findViewById<MaterialCardView>(R.id.cardResult)
        val tvTotal = findViewById<TextView>(R.id.tvTotalDosis)
        val tvDesglose = findViewById<TextView>(R.id.tvDesglose)

        boloHeader.setOnBackClickListener { finish() }

        loadMedicalSettings(boloHeader)

        btnCalcular.setOnClickListener {
            if (!dataLoaded) {
                Toast.makeText(this, "Cargando tus ajustes médicos...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sGlucosa = etGlucosa.text.toString()
            val sRaciones = etRaciones.text.toString()

            if (sGlucosa.isEmpty() || sRaciones.isEmpty()) {
                Toast.makeText(this, "Rellena glucosa y raciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val glucosa = sGlucosa.toDouble()
            val raciones = sRaciones.toDouble()

            // A) Hipoglucemia
            if (glucosa < 70) {
                cardResult.visibility = View.VISIBLE
                cardResult.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(this, android.R.color.holo_red_light)
                )
                tvTotal.text = "HIPO"
                tvTotal.setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
                )
                tvDesglose.text = "¡PELIGRO! Glucosa baja.\nIngiere azúcares rápidos y NO te inyectes."
                tvDesglose.setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
                )
                saveLogToFirebase(glucosa, raciones, 0.0, 0.0, 0.0)
                return@setOnClickListener
            }

            // B) Cálculo con perfil activo (o fallback global)
            cardResult.setCardBackgroundColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
            )
            tvTotal.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.black)
            )
            tvDesglose.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, android.R.color.darker_gray)
            )

            val insuComida = raciones * effectiveRatio
            val correctionTarget = (myObjetivoMin + myObjetivoMax) / 2.0
            val insuCorreccion = (glucosa - correctionTarget) / effectiveSensibilidad
            var total = insuComida + insuCorreccion - currentIob
            if (total < 0) total = 0.0

            val totalRed = (total * 10.0).roundToInt() / 10.0
            val comidaRed = (insuComida * 10.0).roundToInt() / 10.0
            val correccionRed = (insuCorreccion * 10.0).roundToInt() / 10.0
            val iobRed = (currentIob * 10.0).roundToInt() / 10.0

            tvTotal.text = "$totalRed U"

            val desglose = buildString {
                if (correccionRed < 0) {
                    append("Comida ($comidaRed) - Resta (${kotlin.math.abs(correccionRed)})")
                } else {
                    append("Comida ($comidaRed) + Corrección ($correccionRed)")
                }
                if (iobRed > 0) append(" - IOB ($iobRed)")
            }
            tvDesglose.text = desglose
            cardResult.visibility = View.VISIBLE

            saveLogToFirebase(glucosa, raciones, comidaRed, correccionRed, totalRed, iobRed)
            showInterstitialAd()
        }

    }

    // ── Carga de ajustes + perfiles ──────────────────────────────────────────

    private fun loadMedicalSettings(header: MedicalHeaderView) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Ve a Ajustes para configurar tus ratios", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val ratio = document.getDouble("factorHC")
                val sensi = document.getDouble("sensibilidad")
                // Retrocompatible con el antiguo campo "target"
                val legacyTarget = document.getDouble("target")
                val objetivoMin = document.getDouble("objetivoMin") ?: legacyTarget ?: 70.0
                val objetivoMax = document.getDouble("objetivoMax") ?: 180.0
                val dia = document.getDouble("diaHoras") ?: 4.0

                if (ratio == null || sensi == null) {
                    Toast.makeText(this, "¡Faltan configurar tus Ajustes!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                myRatio = ratio
                mySensibilidad = sensi
                myObjetivoMin = objetivoMin
                myObjetivoMax = objetivoMax
                myDia = dia

                @Suppress("UNCHECKED_CAST")
                val rawPerfiles = document.get("perfilesHorarios") as? List<Map<*, *>> ?: emptyList()
                perfiles = rawPerfiles.map { PerfilHorario.fromMap(it) }

                val activo = PerfilHorario.getActivo(perfiles)
                header.setSubtitleExtra(activo?.let { "Perfil activo: ${it.rangoTexto()}" })

                loadCurrentIob()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCurrentIob() {
        val userId = auth.currentUser?.uid ?: run { dataLoaded = true; return }
        val cutoff = System.currentTimeMillis() - (myDia * 3_600_000).toLong()

        db.collection("users").document(userId)
            .collection("history")
            .whereGreaterThan("fecha", cutoff)
            .get()
            .addOnSuccessListener { documents ->
                val logs = documents.map { doc ->
                    BoloLog(
                        fecha = doc.getLong("fecha") ?: 0L,
                        dosisTotal = doc.getDouble("dosisTotal") ?: 0.0
                    )
                }
                currentIob = IobCalculator.calcular(logs, (myDia * 60).toInt())
                dataLoaded = true
            }
            .addOnFailureListener {
                currentIob = 0.0
                dataLoaded = true
            }
    }

    // ── AdMob ────────────────────────────────────────────────────────────────

    private fun loadInterstitialAd() {
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712", // TODO: reemplaza con tu ad unit ID real
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            }
        )
    }

    private fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                }
            }
            ad.show(this)
        }
    }

    // ── Guardar en Firestore ─────────────────────────────────────────────────

    private fun saveLogToFirebase(
        glucosa: Double,
        raciones: Double,
        comida: Double,
        correccion: Double,
        total: Double,
        iobDescontado: Double = 0.0
    ) {
        val userId = auth.currentUser?.uid ?: return
        val nuevoLog = BoloLog(
            glucosa = glucosa,
            raciones = raciones,
            dosisComida = comida,
            dosisCorreccion = correccion,
            dosisTotal = total,
            iobDescontado = iobDescontado,
            fecha = System.currentTimeMillis()
        )
        db.collection("users").document(userId).collection("history").add(nuevoLog)
    }
}
