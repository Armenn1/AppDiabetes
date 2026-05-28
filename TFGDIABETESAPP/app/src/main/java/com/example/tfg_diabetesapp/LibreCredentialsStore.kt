package com.example.tfg_diabetesapp

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacén local cifrado para la contraseña de LibreLinkUp.
 *
 * La contraseña nunca debe escribirse en Firestore en texto plano. Se guarda
 * cifrada con AES-256-GCM bajo una MasterKey respaldada por el Android Keystore,
 * vinculada al dispositivo.
 */
object LibreCredentialsStore {
    private const val PREFS_NAME = "libre_secure_prefs"
    private const val KEY_PASSWORD = "libre_password"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getPassword(context: Context): String =
        prefs(context).getString(KEY_PASSWORD, "").orEmpty()

    fun setPassword(context: Context, password: String) {
        prefs(context).edit().putString(KEY_PASSWORD, password).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
