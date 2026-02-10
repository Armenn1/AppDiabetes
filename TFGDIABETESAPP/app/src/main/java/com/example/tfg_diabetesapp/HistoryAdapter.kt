package com.example.tfg_diabetesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val listaBolos: List<BoloLog>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvGlucosa: TextView = view.findViewById(R.id.tvGlucosaItem)
        val tvDosis: TextView = view.findViewById(R.id.tvDosisItem)
        val tvRaciones: TextView = view.findViewById(R.id.tvRacionesItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val log = listaBolos[position]

        // 1. Formatear la fecha (de milisegundos a texto legible)
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val fechaTexto = sdf.format(Date(log.fecha))

        holder.tvFecha.text = fechaTexto

        // 2. Poner los datos
        // Usamos toInt() en glucosa para quitar decimales (.0) visualmente
        holder.tvGlucosa.text = log.glucosa.toInt().toString()
        holder.tvDosis.text = "${log.dosisTotal} U"
        holder.tvRaciones.text = "Raciones: ${log.raciones} | Comida: ${log.dosisComida} U | Corr: ${log.dosisCorreccion} U"

        // Cambiar color si la glucosa fue alta (>180)

        if (log.glucosa > 180) {
            holder.tvGlucosa.setTextColor(android.graphics.Color.RED)
        }

    }

    override fun getItemCount() = listaBolos.size
}