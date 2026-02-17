package com.example.tfg_diabetesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista mutable para guardar los datos
    private val listaDatos = mutableListOf<BoloLog>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Inflar la vista
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // 2. Referencias UI (usando 'view.')
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        // 3. Configurar RecyclerView (ATENCIÓN: requireContext() en lugar de 'this')
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(listaDatos)
        rvHistory.adapter = adapter

        // 4. Cargar datos desde Firebase
        cargarHistorial(tvEmpty, rvHistory)

        return view
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