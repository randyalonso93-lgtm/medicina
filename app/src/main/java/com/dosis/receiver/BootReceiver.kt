package com.dosis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosis.util.ReprogramarAlarmas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-programa todas las alarmas cuando el dispositivo se reinicia,
 * porque el AlarmManager pierde las alarmas al apagarse.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

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