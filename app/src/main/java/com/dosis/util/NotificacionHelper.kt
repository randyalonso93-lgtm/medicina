package com.dosis.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dosis.MainActivity
import com.dosis.R
import com.dosis.receiver.AlarmaReceiver
import com.dosis.receiver.ResumenDiarioReceiver
import java.util.Calendar

class NotificacionHelper(private val contexto: Context) {

    companion object {
        const val CANAL_RECORDATORIO = "canal_recordatorios"
        const val CANAL_STOCK = "canal_stock"
        const val CANAL_RESUMEN = "canal_resumen"
        const val NOTIF_RECORDATORIO = 1001
        const val NOTIF_STOCK_BAJO = 2001
        const val NOTIF_STOCK_VACIO = 2002
        const val NOTIF_RESUMEN = 3001

        const val ACCION_TOMADO = "TOMADO"
        const val ACCION_OMITIDO = "OMITIDO"
        const val ACCION_APLAZAR = "APLAZAR"
        const val ACCION_SNOOZE = "SNOOZE"

        const val REQUEST_TOMADO = 10000
        const val REQUEST_APLAZAR = 20000
        const val REQUEST_OMITIDO = 30000
        const val REQUEST_SNOOZE = 50000
        const val REQUEST_RESUMEN = 7777

        const val MINUTOS_SNOOZE = 10
        const val HORA_RESUMEN = 21
        const val MINUTO_RESUMEN = 0
    }

    private val notificationManager =
        contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        crearCanales()
    }

    private fun crearCanales() {
        val canalRecordatorio = NotificationChannel(
            CANAL_RECORDATORIO,
            contexto.getString(R.string.canal_nombre),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = contexto.getString(R.string.canal_desc)
            enableVibration(true)
            enableLights(true)
        }

        val canalStock = NotificationChannel(
            CANAL_STOCK,
            contexto.getString(R.string.canal_stock_nombre),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = contexto.getString(R.string.canal_stock_desc)
        }

        val canalResumen = NotificationChannel(
            CANAL_RESUMEN,
            contexto.getString(R.string.canal_resumen_nombre),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = contexto.getString(R.string.canal_resumen_desc)
        }

        notificationManager.createNotificationChannel(canalRecordatorio)
        notificationManager.createNotificationChannel(canalStock)
        notificationManager.createNotificationChannel(canalResumen)
    }

    fun mostrarRecordatorio(dosisId: Int, medicamentoId: Int, nombre: String, dosis: String) {
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            contexto,
            dosisId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accion = { accionConst: String, requestCode: Int, etiqueta: String ->
            val i = Intent(contexto, AlarmaReceiver::class.java).apply {
                action = accionConst
                putExtra("medicamento_id", medicamentoId)
                putExtra("dosis_id", dosisId)
            }
            PendingIntent.getBroadcast(
                contexto,
                requestCode + dosisId,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val tomadoPending = accion(ACCION_TOMADO, REQUEST_TOMADO, contexto.getString(R.string.tomado))
        val aplazarPending = accion(ACCION_APLAZAR, REQUEST_APLAZAR, contexto.getString(R.string.posponer))
        val omitidoPending = accion(ACCION_OMITIDO, REQUEST_OMITIDO, contexto.getString(R.string.omitido))

        val notificacion = NotificationCompat.Builder(contexto, CANAL_RECORDATORIO)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(contexto.getString(R.string.notif_titulo))
            .setContentText(contexto.getString(R.string.notif_texto, nombre, dosis))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contexto.getString(R.string.notif_texto, nombre, dosis))
            )
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_input_add, contexto.getString(R.string.tomado), tomadoPending)
            .addAction(android.R.drawable.ic_menu_revert, contexto.getString(R.string.omitido), omitidoPending)
            .addAction(android.R.drawable.ic_lock_idle_alarm, contexto.getString(R.string.posponer), aplazarPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_RECORDATORIO + dosisId, notificacion)
    }

    fun mostrarAlertaStockBajo(nombre: String, cantidad: Int, medicamentoId: Int, diasRestantes: Int) {
        val texto = if (diasRestantes > 0) {
            contexto.getString(R.string.stock_bajo_dias, nombre, cantidad, diasRestantes)
        } else {
            contexto.getString(R.string.stock_bajo, nombre, cantidad)
        }
        val notificacion = NotificationCompat.Builder(contexto, CANAL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(contexto.getString(R.string.notif_stock_bajo))
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_STOCK_BAJO + medicamentoId, notificacion)
    }

    fun mostrarAlertaSinStock(nombre: String, medicamentoId: Int) {
        val notificacion = NotificationCompat.Builder(contexto, CANAL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(contexto.getString(R.string.notif_sin_stock))
            .setContentText(contexto.getString(R.string.stock_vacio, nombre))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_STOCK_VACIO + medicamentoId, notificacion)
    }

    fun mostrarResumenDiario(tomadas: Int, omitidas: Int) {
        val texto = contexto.getString(R.string.resumen_diario_texto, tomadas, omitidas)
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            contexto,
            REQUEST_RESUMEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL_RESUMEN)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(contexto.getString(R.string.resumen_diario_titulo))
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIF_RESUMEN, notificacion)
    }

    /**
     * Programa la alarma de una dosis calculando la próxima ocurrencia según su frecuencia.
     */
    fun programarAlarma(
        dosisId: Int,
        hora: String,
        frecuencia: Int = ProgramacionDosis.DIARIA,
        diasSemana: Int = 0,
        intervaloDias: Int = 0,
        fechaBase: Long = 0
    ) {
        val calendario = ProgramacionDosis.proximaOcurrencia(
            hora, frecuencia, diasSemana, intervaloDias, fechaBase
        )
        val intent = Intent(contexto, AlarmaReceiver::class.java).apply {
            putExtra("dosis_id", dosisId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            dosisId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            && alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        }
        android.util.Log.d(
            "Dosis", "ALARMA PROGRAMADA: dosis=$dosisId, hora=$hora, " +
                "frec=$frecuencia, millis=${calendario.timeInMillis}, ahora=${System.currentTimeMillis()}"
        )
    }

    /** Programa una alarma única para re-disparar el recordatorio dentro de N minutos. */
    fun programarSnooze(dosisId: Int) {
        val calendario = Calendar.getInstance().apply {
            add(Calendar.MINUTE, MINUTOS_SNOOZE)
        }
        val intent = Intent(contexto, AlarmaReceiver::class.java).apply {
            action = ACCION_SNOOZE
            putExtra("dosis_id", dosisId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            REQUEST_SNOOZE + dosisId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            && alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelarSnooze(dosisId: Int) {
        val intent = Intent(contexto, AlarmaReceiver::class.java).apply {
            action = ACCION_SNOOZE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            REQUEST_SNOOZE + dosisId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun cancelarAlarma(dosisId: Int) {
        val intent = Intent(contexto, AlarmaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            dosisId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Programa la notificación diaria de resumen (inexacta, sin permisos especiales). */
    fun programarResumenDiario() {
        val intent = Intent(contexto, ResumenDiarioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            REQUEST_RESUMEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val primer = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HORA_RESUMEN)
            set(Calendar.MINUTE, MINUTO_RESUMEN)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (primer.before(Calendar.getInstance())) {
            primer.add(Calendar.DAY_OF_MONTH, 1)
        }
        val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            primer.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}