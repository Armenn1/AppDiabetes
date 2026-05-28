package com.diabetly.app.glucose

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.diabetly.app.LibreCredentialsStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GlucoseSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success()

        val db = FirebaseFirestore.getInstance()

        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val email = userDoc.getString("libreEmail").orEmpty()
            val pass = LibreCredentialsStore.getPassword(applicationContext)
            if (email.isEmpty() || pass.isEmpty()) {
                Log.d(TAG, "Sin credenciales LibreLinkUp, abort")
                return Result.success()
            }

            val result = LibreLinkUpRepository.getGlucoseData(email, pass)
            if (result == null || result.graphMeasurements.isEmpty()) {
                Log.w(TAG, "Respuesta vacía de LibreLinkUp")
                return Result.retry()
            }

            saveBatch(db, userId, result.graphMeasurements)
            Log.d(TAG, "Sync OK: ${result.graphMeasurements.size} puntos")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fallo en sync: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun saveBatch(
        db: FirebaseFirestore,
        userId: String,
        measurements: List<GlucoseMeasurement>
    ) {
        val colRef = db.collection("users").document(userId).collection("glucoseReadings")
        val parsers = listOf(
            SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US),
            SimpleDateFormat("M/d/yyyy H:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        )

        val batch = db.batch()
        var count = 0
        for (m in measurements) {
            var fecha: Long? = null
            for (p in parsers) {
                try { fecha = p.parse(m.timestamp)?.time; if (fecha != null) break }
                catch (_: Exception) {}
            }
            if (fecha == null) continue
            val data = mapOf(
                "fecha" to fecha,
                "valor" to m.value,
                "tendencia" to m.trendArrow,
                "fuente" to "LibreLinkUp"
            )
            batch.set(colRef.document(fecha.toString()), data)
            count++
            if (count == 500) break
        }
        if (count > 0) batch.commit().await()
    }

    companion object {
        private const val TAG = "GlucoseSyncWorker"
        private const val WORK_NAME = "glucose_sync_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<GlucoseSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
