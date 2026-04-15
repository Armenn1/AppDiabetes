package com.example.tfg_diabetesapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var lista: List<BoloLog>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvTipoBadge: TextView = view.findViewById(R.id.tvTipoBadge)
        val tvGlucosa: TextView = view.findViewById(R.id.tvGlucosaItem)
        val tvUnidadIzq: TextView = view.findViewById(R.id.tvUnidadIzq)
        val tvDosis: TextView = view.findViewById(R.id.tvDosisItem)
        val tvDetalle: TextView = view.findViewById(R.id.tvRacionesItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val log = lista[position]
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        holder.tvFecha.text = sdf.format(Date(log.fecha))

        when (log.tipo) {
            "Insulina" -> bindInsulina(holder, log)
            "Comida"   -> bindComida(holder, log)
            else       -> bindCalculadora(holder, log)
        }
    }

    // ── Calculadora ──────────────────────────────────────────────────────────
    private fun bindCalculadora(h: HistoryViewHolder, log: BoloLog) {
        h.tvTipoBadge.visibility = View.GONE

        // Glucosa coloreada según rango (umbrales estándar, sin acceso al contexto de usuario aquí)
        h.tvGlucosa.visibility = View.VISIBLE
        h.tvUnidadIzq.visibility = View.VISIBLE
        h.tvGlucosa.text = log.glucosa.toInt().toString()
        h.tvUnidadIzq.text = " mg/dL"
        val glucosaColor = when {
            log.glucosa < 70  -> Color.parseColor("#EF5350")
            log.glucosa > 180 -> Color.parseColor("#FF9800")
            else              -> Color.parseColor("#4CAF50")
        }
        h.tvGlucosa.setTextColor(glucosaColor)

        // Dosis en azul primario
        h.tvDosis.visibility = View.VISIBLE
        h.tvDosis.text = "${log.dosisTotal} U"
        h.tvDosis.setTextColor(Color.parseColor("#1976D2"))

        // Desglose
        val detalle = buildString {
            append("${log.raciones} rac")
            if (log.dosisComida > 0) append(" · Comida: ${log.dosisComida}U")
            if (log.dosisCorreccion != 0.0) append(" · Corr: ${log.dosisCorreccion}U")
            if (log.iobDescontado > 0) append(" · IOB: -${log.iobDescontado}U")
        }
        h.tvDetalle.text = detalle
    }

    // ── Insulina manual ──────────────────────────────────────────────────────
    private fun bindInsulina(h: HistoryViewHolder, log: BoloLog) {
        val purple = Color.parseColor("#9C27B0")

        // Badge
        h.tvTipoBadge.visibility = View.VISIBLE
        h.tvTipoBadge.text = "Insulina"
        h.tvTipoBadge.setBackgroundColor(purple)

        // Sin glucosa
        h.tvGlucosa.visibility = View.GONE
        h.tvUnidadIzq.visibility = View.GONE

        // Dosis en morado
        h.tvDosis.visibility = View.VISIBLE
        h.tvDosis.text = "${log.dosisTotal} U"
        h.tvDosis.setTextColor(purple)

        // Nota
        h.tvDetalle.text = if (log.nota.isNotBlank()) log.nota else "Inyección manual"
    }

    // ── Comida sin bolo ──────────────────────────────────────────────────────
    private fun bindComida(h: HistoryViewHolder, log: BoloLog) {
        val green = Color.parseColor("#4CAF50")

        // Badge
        h.tvTipoBadge.visibility = View.VISIBLE
        h.tvTipoBadge.text = "Comida"
        h.tvTipoBadge.setBackgroundColor(green)

        // Raciones como valor principal izquierda
        h.tvGlucosa.visibility = View.VISIBLE
        h.tvUnidadIzq.visibility = View.VISIBLE
        h.tvGlucosa.text = log.raciones.toString()
        h.tvGlucosa.setTextColor(green)
        h.tvUnidadIzq.text = if (log.gramos > 0) " rac (${log.gramos.toInt()}g)" else " rac"

        // Sin dosis
        h.tvDosis.visibility = View.GONE

        // Nota
        h.tvDetalle.text = if (log.nota.isNotBlank()) log.nota else ""
    }

    override fun getItemCount() = lista.size

    /** Actualiza la lista con DiffUtil para evitar parpadeos */
    fun updateLista(nueva: List<BoloLog>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = lista.size
            override fun getNewListSize() = nueva.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                lista[oldPos].fecha == nueva[newPos].fecha
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                lista[oldPos] == nueva[newPos]
        })
        lista = nueva
        diff.dispatchUpdatesTo(this)
    }
}
