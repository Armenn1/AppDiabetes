package com.example.tfg_diabetesapp.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tfg_diabetesapp.R

class MedicalHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subtitleView: TextView
    private val subtitleExtraView: TextView
    private val logoutButton: ImageButton

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.color_primary))
        val px = resources.getDimensionPixelSize(R.dimen.header_padding_horizontal)
        val pt = resources.getDimensionPixelSize(R.dimen.header_padding_top)
        val pb = resources.getDimensionPixelSize(R.dimen.header_padding_bottom)
        setPadding(px, pt, px, pb)

        LayoutInflater.from(context).inflate(R.layout.view_medical_header, this, true)
        titleView = findViewById(R.id.headerTitle)
        subtitleView = findViewById(R.id.headerSubtitle)
        subtitleExtraView = findViewById(R.id.headerSubtitleExtra)
        logoutButton = findViewById(R.id.headerLogout)

        context.theme.obtainStyledAttributes(attrs, R.styleable.MedicalHeaderView, 0, 0).apply {
            try {
                getString(R.styleable.MedicalHeaderView_headerTitle)?.let { titleView.text = it }
                getString(R.styleable.MedicalHeaderView_headerSubtitle)?.let { subtitleView.text = it }
                val showLogout = getBoolean(R.styleable.MedicalHeaderView_headerShowLogout, false)
                logoutButton.visibility = if (showLogout) View.VISIBLE else View.GONE
                val showExtra = getBoolean(R.styleable.MedicalHeaderView_headerShowSubtitleExtra, false)
                subtitleExtraView.visibility = if (showExtra) View.VISIBLE else View.GONE
            } finally {
                recycle()
            }
        }
    }

    fun setTitle(text: CharSequence) { titleView.text = text }

    fun setSubtitle(text: CharSequence) { subtitleView.text = text }

    fun setSubtitleExtra(text: CharSequence?) {
        if (text.isNullOrBlank()) {
            subtitleExtraView.visibility = View.GONE
        } else {
            subtitleExtraView.text = text
            subtitleExtraView.visibility = View.VISIBLE
        }
    }

    fun setOnLogoutClickListener(listener: OnClickListener?) {
        logoutButton.setOnClickListener(listener)
        if (listener != null) logoutButton.visibility = View.VISIBLE
    }
}
