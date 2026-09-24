package com.dosis.util

import com.dosis.data.Dosis
import java.util.Calendar

/**
 * Lógica de programación de dosis: recurrencia diaria, días de la semana o cada X días.
 */
object ProgramacionDosis {

    const val DIARIA = 0
    const val SEMANAL = 1
    const val CADA_X = 2

    const val LUN = 1 shl 0
    const val MARTES = 1 shl 1
    const val MIERCOLES = 1 shl 2
    const val JUEVES = 1 shl 3
    const val VIERNES = 1 shl 4
    const val SABADO = 1 shl 5
    const val DOMINGO = 1 shl 6

    fun bitDeDia(diaSemana: Int): Int = 1 shl ((diaSemana + 5) % 7)

    fun diaAplicaSegunBitmask(diaSemana: Int, bitmask: Int): Boolean =
        bitmask != 0 && (bitmask and bitDeDia(diaSemana)) != 0

    /** ¿La dosis corresponde programarse el día indicado? */
    fun dosisAplicaHoy(d: Dosis, hoy: Calendar = Calendar.getInstance()): Boolean {
        return when (d.frecuencia) {
            SEMANAL -> diaAplicaSegunBitmask(hoy.get(Calendar.DAY_OF_WEEK), d.diasSemana)
            CADA_X -> {
                if (d.intervaloDias <= 1) return true
                val base = d.fechaBase
                if (base <= 0) return true
                val dias = (inicioDia(hoy.timeInMillis) - inicioDia(base)) / 86_400_000L
                dias % d.intervaloDias == 0L
            }
            else -> true
        }
    }

    /**
     * Próxima ocurrencia >= ahora para una dosis. Nunca devuelve una hora ya pasada.
     */
    fun proximaOcurrencia(
        hora: String,
        frecuencia: Int,
        diasSemana: Int,
        intervaloDias: Int,
        fechaBase: Long
    ): Calendar {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0

        val ahora = Calendar.getInstance()
        val candidato = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when (frecuencia) {
            SEMANAL -> {
                // Buscar dentro de los próximos 7 días el que coincida con la máscara
                for (i in 0..7) {
                    val dia = candidato.get(Calendar.DAY_OF_WEEK)
                    val futura = i == 0 && !candidato.before(ahora)
                    if (diaAplicaSegunBitmask(dia, diasSemana)) {
                        if (futura) return candidato
                    }
                    candidato.add(Calendar.DAY_OF_MONTH, 1)
                    if (candidato.after(ahora)) {
                        // a partir de aquí todos los candidatos son futuros
                        if (diaAplicaSegunBitmask(candidato.get(Calendar.DAY_OF_WEEK), diasSemana)) {
                            return candidato
                        }
                    }
                }
                // Sin días válidos: caer a diaria
                candidato
            }
            CADA_X -> {
                val intervalo = if (intervaloDias > 0) intervaloDias else 1
                val inicioHoy = inicioDia(ahora.timeInMillis)
                val base = if (fechaBase > 0) inicioDia(fechaBase) else inicioHoy

                // Días transcurridos desde la base (los ticks caen en base + k*intervalo).
                val diasDesdeBase = ((inicioHoy - base) / 86_400_000L).coerceAtLeast(0L)
                val resto = diasDesdeBase % intervalo
                val diasHastaTick =
                    if (resto == 0L) 0L else (intervalo - resto).toLong()
                val tickInicio = inicioHoy + diasHastaTick * 86_400_000L

                val candidato = Calendar.getInstance().apply {
                    timeInMillis = tickInicio
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (candidato.before(ahora)) {
                    candidato.add(Calendar.DAY_OF_MONTH, intervalo)
                }
                candidato
            }
            else -> {
                if (candidato.before(ahora)) {
                    candidato.add(Calendar.DAY_OF_MONTH, 1)
                }
                candidato
            }
        }
    }

    fun inicioDia(cal: Calendar): Calendar = Calendar.getInstance().apply {
        timeInMillis = inicioDia(cal.timeInMillis)
    }

    fun inicioDia(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun esMismoDia(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) &&
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR)

    fun esDiaSiguiente(a: Calendar, b: Calendar): Boolean {
        val c = a.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, -1)
        return esMismoDia(c, b)
    }

    val nombresDiasCortos = listOf("L", "M", "X", "J", "V", "S", "D")
}