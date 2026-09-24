package com.dosis.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Preferencias del modo claro/oscuro de la app. */
object PreferenciasTema {

    private const val PREF_NOMBRE = "dosis_prefs"
    private const val CLAVE_MODO = "modo_noche"

    fun guardar(contexto: Context, modo: Int) {
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putInt(CLAVE_MODO, modo)
            .apply()
        aplicar(contexto)
    }

    fun aplicar(contexto: Context) {
        val modo = contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .getInt(CLAVE_MODO, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(modo)
    }

    fun modoActual(contexto: Context): Int =
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .getInt(CLAVE_MODO, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}