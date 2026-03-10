package com.example.tfg_diabetesapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import android.widget.Toast

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. INICIALIZAMOS FIREBASE PRIMERO
        auth = FirebaseAuth.getInstance()

        // --- EL PASE VIP (INICIO DE SESIÓN AUTOMÁTICO) ---
        if (auth.currentUser != null) {
            // Ya hay un usuario logueado. Lo mandamos directo y matamos esta pantalla.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return // 🚨 IMPORTANTE: Cortamos la ejecución aquí para que no cargue el diseño
        }
        // -------------------------------------------------

        // 2. SI LLEGA HASTA AQUÍ, ES QUE NO HAY SESIÓN. CARGAMOS EL DISEÑO NORMAL.
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // Pequeño escudo: Evitar crasheos si le dan al botón sin escribir nada
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this ,"Error al Iniciar Sesión: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}