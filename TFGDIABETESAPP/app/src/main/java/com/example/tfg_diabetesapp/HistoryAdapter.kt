package com.example.tfg_diabetesapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var items: List<HistoryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ── Modelo de items ──────────────────────────────────────────────────────

    sealed class HistoryItem {
        data class Header(
            val fechaTexto: String,
            val tdd: Double,
            val numBolos: Int
        ) : HistoryItem()

        data class Registro(val log: BoloLog) : HistoryItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM   = 1

        /** Convierte lista plana de logs en lista con headers de día */
        fun buildItemList(logs: List<BoloLog>): List<HistoryItem> {
            if (logs.isEmpty()) return emptyList()
            val sdfKey  = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val sdfShow = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()
            val todayKey = sdfKey.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = sdfKey.format(cal.time)

            val result  = mutableListOf<HistoryItem>()
            val porDia  = logs.groupBy { sdfKey.format(Date(it.fecha)) }

            // Firestore ya devuelve ordenado desc, groupBy mantiene el orden
            porDia.forEach { (key, logsDelDia) ->
                val fechaTexto = when (key) {
                    todayKey     -> "Hoy"
                    yesterdayKey -> "Ayer"
                    else         -> sdfShow.format(Date(logsDelDia.first().fecha))
                }
                val tdd = logsDelDia
                    .filter { it.tipo == "Calculadora" || it.tipo == "Insulina" }
                    .sumOf { it.dosisTotal }
                val bolos = logsDelDia.count { it.tipo == "Calculadora" || it.tipo == "Insulina" }
                result.add(HistoryItem.Header(fechaTexto, tdd, bolos))
                logsDelDia.forEach { result.add(HistoryItem.Registro(it)) }
            }
            return result
        }
    }

    // ── ViewHolders ──────────────────────────────────────────────────────────

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvHeaderFecha)
        val tvTdd: TextView   = view.findViewById(R.id.tvHeaderTdd)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha:      TextView  = view.findViewById(R.id.tvFecha)
        val tvBadge:      TextView  = view.findViewById(R.id.tvTipoBadge)
        val tvGlucosa:    TextView  = view.findViewById(R.id.tvGlucosaItem)
        val tvUnidad:     TextView  = view.findViewById(R.id.tvUnidadIzq)
        val tvTendencia:  TextView  = view.findViewById(R.id.tvTendenciaItem)
        val tvTipoComida: TextView  = view.findViewById(R.id.tvTipoComidaItem)
        val tvDosis:      TextView  = view.findViewById(R.id.tvDosisItem)
        val tvDetalle:    TextView  = view.findViewById(R.id.tvRacionesItem)
        val ivFoto:       ImageView = view.findViewById(R.id.ivFotoComida)
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) =
        if (items[position] is HistoryItem.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_history_header, parent, false))
        } else {
            ItemViewHolder(inflater.inflate(R.layout.item_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.Header  -> bindHeader(holder as HeaderViewHolder, item)
            is HistoryItem.Registro -> bindItem(holder as ItemViewHolder, item.log)
        }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): HistoryItem = items[position]

    // ── Bind header ──────────────────────────────────────────────────────────

    private fun bindHeader(h: HeaderViewHolder, header: HistoryItem.Header) {
        h.tvFecha.text = header.fechaTexto
        val tddStr = (Math.round(header.tdd * 10.0) / 10.0).toString()
        h.tvTdd.text = if (header.numBolos > 0) "TDD: ${tddStr} U · ${header.numBolos} bolo${if (header.numBolos != 1) "s" else ""}" else ""
    }

    // ── Bind item ────────────────────────────────────────────────────────────

    private fun bindItem(h: ItemViewHolder, log: BoloLog) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        h.tvFecha.text = sdf.format(Date(log.fecha))

        // Tipo de comida emoji
        if (log.tipoComida.isNotEmpty()) {
            h.tvTipoComida.visibility = View.VISIBLE
            h.tvTipoComida.text = when (log.tipoComida) {
                "desayuno" -> "☀️"
                "comida"   -> "🌤️"
                "merienda" -> "🍎"
                "cena"     -> "🌙"
                else       -> "🍽️"
            }
        } else {
            h.tvTipoComida.visibility = View.GONE
        }

        // Foto (cache local en filesDir/meal_photos)
        val file = if (log.imagenUrl.isNotEmpty()) File(log.imagenUrl) else null
        if (file != null && file.exists()) {
            h.ivFoto.visibility = View.VISIBLE
            h.ivFoto.load(file) {
                crossfade(true)
                size(120, 120)
            }
        } else {
            h.ivFoto.visibility = View.GONE
        }

        when (log.tipo) {
            "Insulina" -> bindInsulina(h, log)
            "Comida"   -> bindComida(h, log)
            else       -> bindCalculadora(h, log)
        }
    }

    private fun bindCalculadora(h: ItemViewHolder, log: BoloLog) {
        h.tvBadge.visibility = View.GONE

        h.tvGlucosa.visibility = View.VISIBLE
        h.tvUnidad.visibility  = View.VISIBLE
        h.tvGlucosa.text = log.glucosa.toInt().toString()
        h.tvUnidad.text  = " mg/dL"
        val glucosaColor = when {
            log.glucosa < 70  -> Color.parseColor("#EF5350")
            log.glucosa > 180 -> Color.parseColor("#FF9800")
            else              -> Color.parseColor("#4CAF50")
        }
        h.tvGlucosa.setTextColor(glucosaColor)

        // Tendencia
        if (log.tendencia > 0) {
            h.tvTendencia.visibility = View.VISIBLE
            h.tvTendencia.text = when (log.tendencia) {
                1 -> "↓↓"; 2 -> "↓"; 3 -> "→"; 4 -> "↑"; 5 -> "↑↑"
                else -> ""
            }
            h.tvTendencia.setTextColor(glucosaColor)
        } else {
            h.tvTendencia.visibility = View.GONE
        }

        h.tvDosis.visibility = View.VISIBLE
        h.tvDosis.text = "${log.dosisTotal} U"
        h.tvDosis.setTextColor(Color.parseColor("#1976D2"))

        val detalle = buildString {
            append("${log.raciones} rac")
            if (log.dosisComida > 0)     append(" · Comida: ${log.dosisComida}U")
            if (log.dosisCorreccion != 0.0) append(" · Corr: ${log.dosisCorreccion}U")
            if (log.iobDescontado > 0)   append(" · IOB: -${log.iobDescontado}U")
        }
        h.tvDetalle.text = detalle
    }

    private fun bindInsulina(h: ItemViewHolder, log: BoloLog) {
        val purple = Color.parseColor("#9C27B0")
        h.tvBadge.visibility = View.VISIBLE
        h.tvBadge.text = "Insulina"
        h.tvBadge.background = roundedBadge(purple)
        h.tvGlucosa.visibility   = View.GONE
        h.tvUnidad.visibility    = View.GONE
        h.tvTendencia.visibility = View.GONE
        h.tvDosis.visibility = View.VISIBLE
        h.tvDosis.text = "${log.dosisTotal} U"
        h.tvDosis.setTextColor(purple)
        h.tvDetalle.text = if (log.nota.isNotBlank()) log.nota else "Inyección manual"
    }

    private fun bindComida(h: ItemViewHolder, log: BoloLog) {
        val green = Color.parseColor("#4CAF50")
        h.tvBadge.visibility = View.VISIBLE
        h.tvBadge.text = "Comida"
        h.tvBadge.background = roundedBadge(green)
        h.tvGlucosa.visibility = View.VISIBLE
        h.tvUnidad.visibility  = View.VISIBLE
        h.tvGlucosa.text = log.raciones.toString()
        h.tvGlucosa.setTextColor(green)
        h.tvUnidad.text = if (log.gramos > 0) " rac (${log.gramos.toInt()}g)" else " rac"
        h.tvTendencia.visibility = View.GONE
        h.tvDosis.visibility = View.GONE
        h.tvDetalle.text = if (log.nota.isNotBlank()) log.nota else ""
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────────

    fun updateLista(nuevosLogs: List<BoloLog>) {
        val nuevosItems = buildItemList(nuevosLogs)
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = nuevosItems.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                val old = items[o]; val new = nuevosItems[n]
                if (old is HistoryItem.Header && new is HistoryItem.Header)
                    return old.fechaTexto == new.fechaTexto
                if (old is HistoryItem.Registro && new is HistoryItem.Registro)
                    return old.log.fecha == new.log.fecha
                return false
            }
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == nuevosItems[n]
        })
        items = nuevosItems
        diff.dispatchUpdatesTo(this)
    }

    private fun roundedBadge(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 40f
        setColor(color)
    }
}
