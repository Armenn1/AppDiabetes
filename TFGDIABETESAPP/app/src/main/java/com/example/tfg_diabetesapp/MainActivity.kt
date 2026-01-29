package com.example.tfg_diabetesapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView // Importante para las tarjetas
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // --- ---

        // 1. Botón de Salir (arriba a la derecha)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        // 2. Tarjeta grande morada para ir a Calcular Bolo
        val cardNewBolo = findViewById<MaterialCardView>(R.id.cardNewBolo)

        // 3. Tarjeta blanca inferior para ir a Ajustes
        val cardSettings = findViewById<MaterialCardView>(R.id.cardSettings)

        // 4. Textos para mostrar datos (Glucosa e IOB)
        val tvGlucosa = findViewById<TextView>(R.id.tvGlucosaMain)
        val tvIOB = findViewById<TextView>(R.id.tvIOB)

       // Dat0s de ejemplo
        tvGlucosa.text = "115"
        tvIOB.text = "1.2 U"

        // --- NAVEGACIÓN ---

        // Click en la tarjeta "Calcular Nuevo Bolo" -> Abre BoloActivity
        cardNewBolo.setOnClickListener {
            val intent = Intent(this, BoloActivity::class.java)
            startActivity(intent)
        }

        // Click en la tarjeta "Configuración Médica" -> Abre SettingsActivity
        cardSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // --- LOGOUT ---

        // Click en el botón de apagado -> Cierra sesión y vuelve al Login
        btnLogout.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            // Estas flags borran el historial para que no se pueda volver atrás con el botón físico
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}