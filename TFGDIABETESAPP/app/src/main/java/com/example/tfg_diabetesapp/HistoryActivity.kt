package com.example.tfg_diabetesapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista mutable para guardar los datos
    private val listaDatos = mutableListOf<BoloLog>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnBack = findViewById<ImageButton>(R.id.btnBackHist)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        // Configurar RecyclerView
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(listaDatos)
        rvHistory.adapter = adapter

        // Botón volver
        btnBack.setOnClickListener { finish() }

        // Cargar datos
        cargarHistorial(tvEmpty, rvHistory)
    }

    private fun cargarHistorial(tvEmpty: TextView, rv: RecyclerView) {
        val userId = auth.currentUser?.uid ?: return

        // Consulta: Colección 'history', ordenada por fecha (más nuevo arriba)
        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { resultado ->
                listaDatos.clear() // Limpiar lista anterior

                if (resultado.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE

                    for (documento in resultado) {
                        // Convierte el JSON de Firebase a tu objeto BoloLog automáticamente
                        val log = documento.toObject(BoloLog::class.java)
                        listaDatos.add(log)
                    }
                    // Avisar al adaptador de que hay cambios
                    adapter.notifyDataSetChanged()
                }
            }
    }
}