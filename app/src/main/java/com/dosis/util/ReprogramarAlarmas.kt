package com.dosis.util

import android.content.Context
import com.dosis.data.AppDatabase

/** Re-programa todas las alarmas de dosis y la notificación de resumen diario. */
object ReprogramarAlarmas {

    suspend fun todo(context: Context) {
        val notifHelper = NotificacionHelper(context)
        val dosisDao = AppDatabase.obtenerInstancia(context).dosisDao()
        dosisDao.obtenerTodasActivas().forEach { d ->
            notifHelper.programarAlarma(
                d.id, d.hora, d.frecuencia, d.diasSemana, d.intervaloDias, d.fechaBase
            )
        }
        notifHelper.programarResumenDiario()
    }
}