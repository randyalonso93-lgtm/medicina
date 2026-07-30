package com.example.medicina.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicina.data.AppDatabase
import com.example.medicina.data.RegistroToma
import com.example.medicina.util.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicamentoId = intent.getIntExtra("medicamento_id", -1)
        if (medicamentoId == -1) return

        // Acción "Tomado" desde la notificación
        if (intent.action == "TOMADO") {
            marcarComoTomado(context, medicamentoId)
            return
        }

        // Acción normal: mostrar recordatorio
        val dao = AppDatabase.obtenerInstancia(context).medicamentoDao()
        val notifHelper = NotificacionHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            val medicamento = dao.obtenerPorId(medicamentoId) ?: return@launch

            if (medicamento.activo) {
                notifHelper.mostrarRecordatorio(
                    medicamentoId,
                    medicamento.nombre,
                    medicamento.dosis
                )

                // Re-programar para el día siguiente
                notifHelper.programarAlarma(medicamentoId, medicamento.hora)

                // Verificar stock
                if (medicamento.stockActual <= 0) {
                    notifHelper.mostrarAlertaSinStock(medicamento.nombre)
                } else if (medicamento.stockActual <= medicamento.stockMinimo) {
                    notifHelper.mostrarAlertaStockBajo(medicamento.nombre, medicamento.stockActual)
                }
            }
        }
    }

    private fun marcarComoTomado(context: Context, medicamentoId: Int) {
        val db = AppDatabase.obtenerInstancia(context)
        val notifHelper = NotificacionHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            db.registroTomaDao().insertar(
                RegistroToma(
                    medicamentoId = medicamentoId,
                    tomado = true
                )
            )
            db.medicamentoDao().actualizarStock(medicamentoId, -1)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(NotificacionHelper.NOTIF_RECORDATORIO + medicamentoId)

            val med = db.medicamentoDao().obtenerPorId(medicamentoId)
            if (med != null) {
                if (med.stockActual <= 0) {
                    notifHelper.mostrarAlertaSinStock(med.nombre)
                } else if (med.stockActual <= med.stockMinimo) {
                    notifHelper.mostrarAlertaStockBajo(med.nombre, med.stockActual)
                }
            }
        }
    }
}