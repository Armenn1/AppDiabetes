package com.example.tfg_diabetesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg_diabetesapp.BoloLog
import com.example.tfg_diabetesapp.HistoryAdapter
import com.example.tfg_diabetesapp.R
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista completa cargada de Firestore (sin filtrar)
    private var todosLosRegistros = listOf<BoloLog>()
    private lateinit var adapter: HistoryAdapter

    private lateinit var tvEmpty: TextView
    private lateinit var rvHistory: RecyclerView

    // Filtro activo: null = Todo
    private var filtroActivo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        rvHistory = view.findViewById(R.id.rvHistory)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(emptyList())
        rvHistory.adapter = adapter

        // Chips de filtro
        view.findViewById<ChipGroup>(R.id.chipGroupFiltro).setOnCheckedStateChangeListener { _, ids ->
            filtroActivo = when (ids.firstOrNull()) {
                R.id.chipCalculadora -> "Calculadora"
                R.id.chipInsulina    -> "Insulina"
                R.id.chipComida      -> "Comida"
                else                 -> null
            }
            aplicarFiltro()
        }

        // FAB
        view.findViewById<FloatingActionButton>(R.id.fabAddRegistro).setOnClickListener {
            mostrarMenuRegistro()
        }

        cargarHistorial()
        return view
    }

    // ── Carga desde Firestore ────────────────────────────────────────────────

    private fun cargarHistorial() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("history")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { resultado ->
                todosLosRegistros = resultado.documents.mapNotNull { doc ->
                    try { doc.toObject(BoloLog::class.java) } catch (_: Exception) { null }
                }
                aplicarFiltro()
            }
    }

    // ── Filtrado ─────────────────────────────────────────────────────────────

    private fun aplicarFiltro() {
        val filtrados = if (filtroActivo == null) {
            todosLosRegistros
        } else {
            todosLosRegistros.filter { it.tipo == filtroActivo }
        }

        if (filtrados.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }

        adapter.updateLista(filtrados)
    }

    // ── FAB → selección de tipo ──────────────────────────────────────────────

    private fun mostrarMenuRegistro() {
        val opciones = arrayOf("Registrar insulina manual", "Apuntar comida")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Qué quieres registrar?")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoInsulina()
                    1 -> mostrarDialogoComida()
                }
            }
            .show()
    }

    // ── Diálogo insulina manual ──────────────────────────────────────────────

    private fun mostrarDialogoInsulina() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_log_insulina, null)
        val etDosis = dialogView.findViewById<EditText>(R.id.etInsulina)
        val etNota = dialogView.findViewById<EditText>(R.id.etNotaInsulina)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Insulina manual")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val dosis = etDosis.text.toString().toDoubleOrNull()
                if (dosis == null || dosis <= 0) {
                    Toast.makeText(requireContext(), "Introduce las unidades inyectadas", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val log = BoloLog(
                    tipo = "Insulina",
                    dosisTotal = dosis,
                    nota = etNota.text.toString().trim(),
                    fecha = System.currentTimeMillis()
                )
                guardarRegistro(log)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Diálogo comida sin bolo ──────────────────────────────────────────────

    private fun mostrarDialogoComida() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_log_comida, null)
        val etRaciones = dialogView.findViewById<EditText>(R.id.etRacionesComida)
        val etGramos = dialogView.findViewById<EditText>(R.id.etGramosComida)
        val etNota = dialogView.findViewById<EditText>(R.id.etNotaComida)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Apuntar comida")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val raciones = etRaciones.text.toString().toDoubleOrNull()
                if (raciones == null || raciones <= 0) {
                    Toast.makeText(requireContext(), "Introduce las raciones de HC", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val log = BoloLog(
                    tipo = "Comida",
                    raciones = raciones,
                    gramos = etGramos.text.toString().toDoubleOrNull() ?: 0.0,
                    nota = etNota.text.toString().trim(),
                    fecha = System.currentTimeMillis()
                )
                guardarRegistro(log)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Guardar en Firestore y refrescar ─────────────────────────────────────

    private fun guardarRegistro(log: BoloLog) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("history")
            .add(log)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Registro guardado", Toast.LENGTH_SHORT).show()
                cargarHistorial()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
            }
    }
}
