package com.example.tfg_diabetesapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class QuickBoloWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.quick_bolo_widget)

    // Preparamos la ruta hacia tu BoloActivity
    val intent = Intent(context, BoloActivity::class.java)

    // obligamos a Android a abrirla como una pantalla nueva sí o sí
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

    val pendingIntent = PendingIntent.getActivity(
        context,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Le decimos que reaccione al clic
    views.setOnClickPendingIntent(R.id.btnWidgetBolo, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}