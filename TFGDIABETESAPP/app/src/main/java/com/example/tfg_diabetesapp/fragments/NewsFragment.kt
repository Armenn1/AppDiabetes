package com.example.tfg_diabetesapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tfg_diabetesapp.glucose.LibreLinkUpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tfg_diabetesapp.R

class NewsFragment : Fragment() {

    private lateinit var tvGlucoseValue: TextView
    private lateinit var btnFetchGlucose: Button
    private lateinit var pbGlucoseLoading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        tvGlucoseValue = view.findViewById(R.id.tvGlucoseValue)
        btnFetchGlucose = view.findViewById(R.id.btnFetchGlucose)
        pbGlucoseLoading = view.findViewById(R.id.pbGlucoseLoading)

        btnFetchGlucose.setOnClickListener {
            fetchGlucoseData()
        }

        return view
    }

    private fun fetchGlucoseData() {
        // 1. Preparamos la interfaz (mostramos la ruleta, ocultamos el botón para no hacer spam)
        btnFetchGlucose.isEnabled = false
        pbGlucoseLoading.visibility = View.VISIBLE
        tvGlucoseValue.text = "Conectando..."

        // 2. Lanzamos la corrutina en segundo plano
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // ¡OJO! Pon aquí tu email y contraseña reales de LibreLinkUp
            val email = "rocarmengol0@gmail.com"
            val password = "Matadepera52"

            // Llamamos a nuestro Repositorio
            val glucoseResult = LibreLinkUpRepository.getLatestGlucose(email, password)

            // 3. Volvemos al hilo principal para actualizar la pantalla
            withContext(Dispatchers.Main) {
                pbGlucoseLoading.visibility = View.GONE
                btnFetchGlucose.isEnabled = true

                if (glucoseResult != null) {
                    // ¡Éxito!
                    tvGlucoseValue.text = "$glucoseResult mg/dL"
                    Toast.makeText(requireContext(), "¡Glucosa actualizada!", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallo
                    tvGlucoseValue.text = "Error"
                    Toast.makeText(requireContext(), "Fallo al conectar con Abbott. Mira el Logcat.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}