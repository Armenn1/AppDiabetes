package com.example.tfg_diabetesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val boton = findViewById<Button>(R.id.btnBolo)

        boton.setOnClickListener {
            val intent = Intent(this, BoloActivity::class.java)
            startActivity(intent)
        }

    }
}
