package com.dosis.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Convierte horas en formato 24h "HH:mm" a formato 12h legible ("8:00 a. m."). */
object FormatoHora {

    fun a12(hora: String): String {
        val partes = hora.split(":")
        if (partes.size != 2) return hora
        val h = partes[0].toIntOrNull() ?: return hora
        val m = partes[1].toIntOrNull() ?: return hora
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formato = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formato.format(cal.time)
    }

    /** Convierte una cadena con varias horas separadas por coma (GROUP_CONCAT). */
    fun lista12(csvHoras: String): String =
        csvHoras.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ") { a12(it) }
}