package com.dosis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosis.util.ReprogramarAlarmas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-programa todas las alarmas cuando cambia la hora, la zona horaria o la fecha,
 * porque las alarmas se programan con milisegundos absolutos y quedarían desfasadas.
 */
class CambiosSistemaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> Unit
            else -> return
        }

        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReprogramarAlarmas.todo(context)
            } finally {
                resultado.finish()
            }
        }
    }
}