package com.example.medicina.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicina.data.AppDatabase
import com.example.medicina.util.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-programa todas las alarmas cuando el dispositivo se reinicia,
 * porque el AlarmManager pierde las alarmas al apagarse.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val notifHelper = NotificacionHelper(context)
            val dao = AppDatabase.obtenerInstancia(context).medicamentoDao()

            CoroutineScope(Dispatchers.IO).launch {
                val medicamentos = dao.obtenerTodosUnaVez()
                for (med in medicamentos) {
                    if (med.activo) {
                        notifHelper.programarAlarma(med.id, med.hora)
                    }
                }
            }
        }
    }
}