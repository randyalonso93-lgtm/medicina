package com.dosis.util

import android.content.Context

/** Preferencias sobre permisos ya solicitados al usuario. */
object PreferenciasPermisos {

    private const val PREF_NOMBRE = "dosis_prefs"
    private const val CLAVE_BATERIA_PREGUNTADA = "bateria_optimizacion_preguntada"

    fun bateriaPreguntada(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .getBoolean(CLAVE_BATERIA_PREGUNTADA, false)

    fun marcarBateriaPreguntada(contexto: Context) {
        contexto.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CLAVE_BATERIA_PREGUNTADA, true)
            .apply()
    }
}