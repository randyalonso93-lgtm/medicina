package com.example.medicina.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.medicina.MainActivity
import com.example.medicina.R

class NotificacionHelper(private val contexto: Context) {

    companion object {
        const val CANAL_RECORDATORIO = "canal_recordatorios"
        const val CANAL_STOCK = "canal_stock"
        const val NOTIF_RECORDATORIO = 1001
        const val NOTIF_STOCK_BAJO = 2001
        const val NOTIF_STOCK_VACIO = 2002
    }

    private val notificationManager =
        contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        crearCanales()
    }

    private fun crearCanales() {
        // Canal de recordatorios de medicamento
        val canalRecordatorio = NotificationChannel(
            CANAL_RECORDATORIO,
            contexto.getString(R.string.canal_nombre),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = contexto.getString(R.string.canal_desc)
            enableVibration(true)
            enableLights(true)
        }

        // Canal de alertas de stock
        val canalStock = NotificationChannel(
            CANAL_STOCK,
            "Alertas de inventario",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos cuando queda poco medicamento"
        }

        notificationManager.createNotificationChannel(canalRecordatorio)
        notificationManager.createNotificationChannel(canalStock)
    }

    fun mostrarRecordatorio(medicamentoId: Int, nombre: String, dosis: String) {
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicamento_id", medicamentoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            contexto,
            medicamentoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: marcar como tomado
        val tomadoIntent = Intent(contexto, com.example.medicina.receiver.AlarmaReceiver::class.java).apply {
            action = "TOMADO"
            putExtra("medicamento_id", medicamentoId)
        }
        val tomadoPending = PendingIntent.getBroadcast(
            contexto,
            medicamentoId + 10000,
            tomadoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(contexto, CANAL_RECORDATORIO)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(contexto.getString(R.string.notif_titulo))
            .setContentText(contexto.getString(R.string.notif_texto, nombre, dosis))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contexto.getString(R.string.notif_texto, nombre, dosis))
            )
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Tomado", tomadoPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_RECORDATORIO + medicamentoId, notificacion)
    }

    fun mostrarAlertaStockBajo(nombre: String, cantidad: Int) {
        val notificacion = NotificationCompat.Builder(contexto, CANAL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Stock bajo")
            .setContentText(contexto.getString(R.string.stock_bajo, nombre, cantidad))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_STOCK_BAJO + nombre.hashCode(), notificacion)
    }

    fun mostrarAlertaSinStock(nombre: String) {
        val notificacion = NotificationCompat.Builder(contexto, CANAL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Sin stock")
            .setContentText(contexto.getString(R.string.stock_vacio, nombre))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_STOCK_VACIO + nombre.hashCode(), notificacion)
    }

    /**
     * Programa una alarma exacta para el recordatorio.
     * Se calcula la próxima ocurrencia de la hora indicada.
     */
    fun programarAlarma(medicamentoId: Int, hora: String) {
        val partes = hora.split(":")
        if (partes.size != 2) return

        val horaDeseada = partes[0].toInt()
        val minutoDeseado = partes[1].toInt()

        val calendario = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, horaDeseada)
            set(java.util.Calendar.MINUTE, minutoDeseado)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, programar para mañana
        val ahora = java.util.Calendar.getInstance()
        if (calendario.before(ahora)) {
            calendario.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(contexto, com.example.medicina.receiver.AlarmaReceiver::class.java).apply {
            putExtra("medicamento_id", medicamentoId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            medicamentoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            && alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Fallback para API 26-30
            alarmManager.setAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        }
        android.util.Log.d("MediTrack", "ALARMA PROGRAMADA: id=$medicamentoId, hora=$hora, millis=${calendario.timeInMillis}, ahora=${System.currentTimeMillis()}")


    }

    fun cancelarAlarma(medicamentoId: Int) {
        val intent = Intent(contexto, com.example.medicina.receiver.AlarmaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            medicamentoId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}