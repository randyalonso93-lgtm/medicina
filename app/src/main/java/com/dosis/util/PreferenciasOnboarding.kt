package com.dosis.util

import android.content.Context

/** Preferencias del onboarding de primer inicio. */
object PreferenciasOnboarding {

    private const val PREF_NOMBRE = "dosis_prefs"
    private const val CLAVE_VISTO = "onboarding_visto"

    fun visto(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .getBoolean(CLAVE_VISTO, false)

    fun marcarVisto(contexto: Context) {
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CLAVE_VISTO, true)
            .apply()
    }
}