package com.example.tfg_diabetesapp.glucose

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest

object LibreLinkUpRepository {

    private const val BASE_URL = "https://api-eu.libreview.io/"

    // ¡NUEVO! El truco de magia: Encriptamos el ID a SHA-256 como exige Abbott
    private fun hashSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val api: LibreLinkUpApi by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibreLinkUpApi::class.java)
    }

    suspend fun getLatestGlucose(email: String, pass: String): Int? {
        try {
            Log.d("LibreAPI", "Iniciando asalto: Conectando con Abbott...")

            val loginReq = LoginRequest(email, pass)
            val loginRes = api.login(request = loginReq)

            if (!loginRes.isSuccessful || loginRes.body()?.data == null) {
                Log.e("LibreAPI", "Error Login: Código ${loginRes.code()}")
                return null
            }

            val rawToken = loginRes.body()!!.data!!.authTicket.token
            val bearerToken = "Bearer $rawToken"

            // Extraemos tu ID y lo encriptamos
            val rawAccountId = loginRes.body()!!.data!!.user.id
            val hashedAccountId = hashSHA256(rawAccountId)

            Log.d("LibreAPI", "Login OK. Token: Obtenido | Hashed ID: $hashedAccountId")

            // Le pasamos a Abbott el ID encriptado
            val connRes = api.getConnections(token = bearerToken, accountId = hashedAccountId)

            if (!connRes.isSuccessful) {
                Log.e("LibreAPI", "Abbott ha bloqueado el paso 2 con error: ${connRes.code()}")
                return null
            }
            if (connRes.body()?.data.isNullOrEmpty()) {
                Log.e("LibreAPI", "La lista de pacientes ha llegado vacía (0 sensores)")
                return null
            }

            val patientId = connRes.body()!!.data!![0].patientId
            Log.d("LibreAPI", "Paciente localizado. ID: $patientId")

            // Usamos el ID encriptado también para pedir la gráfica
            val glucoseRes = api.getGlucoseGraph(
                token = bearerToken,
                accountId = hashedAccountId,
                patientId = patientId
            )

            if (!glucoseRes.isSuccessful || glucoseRes.body()?.data?.connection?.glucoseMeasurement == null) {
                Log.e("LibreAPI", "Error al extraer la gráfica. Código: ${glucoseRes.code()}")
                return null
            }

            val finalGlucose = glucoseRes.body()!!.data!!.connection!!.glucoseMeasurement!!.value
            Log.d("LibreAPI", "¡K.O! Glucosa actual obtenida: $finalGlucose mg/dL")

            return finalGlucose

        } catch (e: Exception) {
            Log.e("LibreAPI", "Fallo catastrófico: ${e.message}")
            return null
        }
    }
}