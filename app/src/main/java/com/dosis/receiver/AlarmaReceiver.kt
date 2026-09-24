package com.dosis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosis.data.AppDatabase
import com.dosis.data.Medicamento
import com.dosis.data.MedicamentoRepository
import com.dosis.util.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                procesar(context, intent)
            } catch (_: Exception) {
                // No dejes morir al proceso por un error de reprogramación
            } finally {
                resultado.finish()
            }
        }
    }

    private suspend fun procesar(context: Context, intent: Intent) {
        val dosisId = intent.getIntExtra("dosis_id", -1)
        val medicamentoId = intent.getIntExtra("medicamento_id", -1)
        val accion = intent.action
        val notifHelper = NotificacionHelper(context)
        val repository = MedicamentoRepository(context)

        when (accion) {
            NotificacionHelper.ACCION_TOMADO -> {
                if (medicamentoId != -1) marcarComoTomado(context, medicamentoId, dosisId)
                return
            }
            NotificacionHelper.ACCION_OMITIDO -> {
                notifHelper.cancelarSnooze(dosisId)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(NotificacionHelper.NOTIF_RECORDATORIO + dosisId)
                if (medicamentoId != -1) repository.marcarOmitidaAhora(medicamentoId)
                return
            }
            NotificacionHelper.ACCION_APLAZAR -> {
                notifHelper.programarSnooze(dosisId)
                return
            }
            NotificacionHelper.ACCION_SNOOZE -> {
                // Recordatorio aplazado: mostrar de nuevo sin reprogramar lo diario
                val db = AppDatabase.obtenerInstancia(context)
                val dosis = db.dosisDao().obtenerPorId(dosisId) ?: return
                val med = db.medicamentoDao().obtenerPorId(dosis.medicamentoId) ?: return
                if (!med.activo) return
                notifHelper.mostrarRecordatorio(dosis.id, med.id, med.nombre, med.dosis)
                return
            }
        }

        // Acción normal: recordatorio de una dosis
        if (dosisId == -1) return

        val db = AppDatabase.obtenerInstancia(context)
        val dao = db.medicamentoDao()
        val dosisDao = db.dosisDao()

        val dosis = dosisDao.obtenerPorId(dosisId) ?: return
        val medicamento = dao.obtenerPorId(dosis.medicamentoId) ?: return

        if (!medicamento.activo) return

        // Marcar como omitidas las dosis pasadas de hoy (excepto la que acaba de sonar)
        repository.marcarOmitidasMedicamento(medicamento, excluirDosisId = dosis.id)

        notifHelper.mostrarRecordatorio(
            dosis.id,
            medicamento.id,
            medicamento.nombre,
            medicamento.dosis
        )

        // Re-programar la próxima ocurrencia según la frecuencia de la dosis
        notifHelper.programarAlarma(
            dosis.id,
            dosis.hora,
            dosis.frecuencia,
            dosis.diasSemana,
            dosis.intervaloDias,
            dosis.fechaBase
        )
    }

    private suspend fun marcarComoTomado(context: Context, medicamentoId: Int, dosisId: Int) {
        val db = AppDatabase.obtenerInstancia(context)
        val repository = MedicamentoRepository(context)

        // Cancelar la posible alarma de posponer de ESA dosis
        NotificacionHelper(context).cancelarSnooze(dosisId)

        val med: Medicamento? = db.medicamentoDao().obtenerPorId(medicamentoId)
        med?.let { repository.tomarDosis(it) }

        // Ocultar la notificación de ESA dosis (id único por dosis)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (dosisId != -1) {
            notificationManager.cancel(NotificacionHelper.NOTIF_RECORDATORIO + dosisId)
        } else {
            notificationManager.cancelAll()
        }
    }
}