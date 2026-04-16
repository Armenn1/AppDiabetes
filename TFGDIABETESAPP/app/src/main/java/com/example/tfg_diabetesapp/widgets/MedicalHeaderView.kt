package com.example.tfg_diabetesapp.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.tfg_diabetesapp.R

class MedicalHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val backButton: ImageButton
    private val titleView: TextView
    private val subtitleView: TextView
    private val subtitleExtraView: TextView
    private val logoutButton: ImageButton

    private val basePaddingTop: Int
    private val basePaddingBottom: Int
    private val basePaddingHorizontal: Int

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.color_primary))
        basePaddingHorizontal = resources.getDimensionPixelSize(R.dimen.header_padding_horizontal)
        basePaddingTop = resources.getDimensionPixelSize(R.dimen.header_padding_top)
        basePaddingBottom = resources.getDimensionPixelSize(R.dimen.header_padding_bottom)
        setPadding(basePaddingHorizontal, basePaddingTop, basePaddingHorizontal, basePaddingBottom)

        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = basePaddingTop + statusBarTop)
            insets
        }

        LayoutInflater.from(context).inflate(R.layout.view_medical_header, this, true)
        backButton = findViewById(R.id.headerBack)
        titleView = findViewById(R.id.headerTitle)
        subtitleView = findViewById(R.id.headerSubtitle)
        subtitleExtraView = findViewById(R.id.headerSubtitleExtra)
        logoutButton = findViewById(R.id.headerLogout)

        context.theme.obtainStyledAttributes(attrs, R.styleable.MedicalHeaderView, 0, 0).apply {
            try {
                getString(R.styleable.MedicalHeaderView_headerTitle)?.let { titleView.text = it }
                getString(R.styleable.MedicalHeaderView_headerSubtitle)?.let { subtitleView.text = it }
                backButton.visibility = if (getBoolean(R.styleable.MedicalHeaderView_headerShowBack, false)) View.VISIBLE else View.GONE
                logoutButton.visibility = if (getBoolean(R.styleable.MedicalHeaderView_headerShowLogout, false)) View.VISIBLE else View.GONE
                subtitleExtraView.visibility = if (getBoolean(R.styleable.MedicalHeaderView_headerShowSubtitleExtra, false)) View.VISIBLE else View.GONE
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

    fun setOnBackClickListener(listener: OnClickListener?) {
        backButton.setOnClickListener(listener)
        if (listener != null) backButton.visibility = View.VISIBLE
    }
}
