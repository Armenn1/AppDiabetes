package com.example.tfg_diabetesapp.glucose

import com.google.gson.annotations.SerializedName

// 1. Lo que ENVIAMOS al servidor para hacer Login
data class LoginRequest(
    val email: String,
    val password: String
)

// 2. La RESPUESTA gigante que nos devuelve el servidor al hacer Login con éxito
data class LoginResponse(
    val status: Int,
    val data: LoginData?
)

data class LoginData(
    val authTicket: AuthTicket,
    val user: LibreUser
)

data class AuthTicket(
    val token: String, // 🔑.
    val expires: Long,
    val duration: Long
)

data class LibreUser(
    val id: String // ID interno de Abbott para el usuario
)

// --- A partir de aquí son las clases para pedir la glucosa ---

// 3. La RESPUESTA que nos da el servidor cuando pedimos las conexiones (los sensores vinculados a esa cuenta)
data class ConnectionsResponse(
    val status: Int,
    val data: List<PatientConnection>?
)

data class PatientConnection(
    val patientId: String // El ID real del paciente que lleva el sensor puesto
)

// 4. La RESPUESTA FINAL con el dato de glucosa
data class GlucoseResponse(
    val status: Int,
    val data: GraphData?
)

data class GraphData(
    val connection: ConnectionDetails?,
    val graphData: List<GlucoseMeasurement>?
)

// Resultado combinado: glucosa actual + histórico del día
data class GlucoseResult(
    val currentValue: Int,
    val graphMeasurements: List<GlucoseMeasurement>
)

data class ConnectionDetails(
    val glucoseMeasurement: GlucoseMeasurement?
)

data class GlucoseMeasurement(
    @SerializedName("Value") val value: Int,             // El valor numérico real (ej: 110)
    @SerializedName("TrendArrow") val trendArrow: Int,   // La flecha de tendencia (1=baja rápido, 3=estable, 5=sube rápido...)
    @SerializedName("Timestamp") val timestamp: String   // La fecha y hora de la lectura
)