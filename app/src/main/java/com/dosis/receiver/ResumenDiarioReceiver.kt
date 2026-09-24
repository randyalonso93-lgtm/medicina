package com.dosis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosis.data.MedicamentoRepository
import com.dosis.util.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Notificación resumen al final del día con lo tomado y omitido. */
class ResumenDiarioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = MedicamentoRepository(context)
                val (tomadas, omitidas) = repo.resumenDeHoy()
                if (tomadas > 0 || omitidas > 0) {
                    NotificacionHelper(context).mostrarResumenDiario(tomadas, omitidas)
                }
            } finally {
                resultado.finish()
            }
        }
    }
}