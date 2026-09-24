package com.dosis.data

import android.content.Context
import com.dosis.R
import com.dosis.util.NotificacionHelper
import com.dosis.util.ProgramacionDosis
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MedicamentoRepository(private val contexto: Context) {

    private val db = AppDatabase.obtenerInstancia(contexto)
    private val medDao = db.medicamentoDao()
    private val dosisDao = db.dosisDao()
    private val registroDao = db.registroTomaDao()
    private val notifHelper = NotificacionHelper(contexto)

    fun obtenerMedicamentos(consulta: String): Flow<List<MedicamentoConDosis>> =
        medDao.obtenerTodosConDosis(consulta)

    fun obtenerMedicamentosSimple(): Flow<List<Medicamento>> = medDao.obtenerTodos()

    /**
     * Guarda (inserta o actualiza) un medicamento reemplazando sus dosis.
     * Las alarmas viejas se cancelan y se programan las nuevas.
     */
    suspend fun guardarMedicamento(medicamento: Medicamento, dosis: List<Dosis>): String {
        val esNuevo = medicamento.id == 0
        return if (esNuevo) {
            val id = medDao.insertar(medicamento).toInt()
            programarDosis(id, dosis)
            contexto.getString(R.string.msj_guardado)
        } else {
            val anteriores = dosisDao.obtenerPorMedicamentoUnaVez(medicamento.id)
            anteriores.forEach { notifHelper.cancelarAlarma(it.id) }
            medDao.actualizar(medicamento)
            dosisDao.eliminarPorMedicamento(medicamento.id)
            programarDosis(medicamento.id, dosis)
            contexto.getString(R.string.msj_actualizado)
        }
    }

    private suspend fun programarDosis(medicamentoId: Int, dosis: List<Dosis>) {
        val hoy = ProgramacionDosis.inicioDia(Calendar.getInstance()).timeInMillis
        dosis
            .distinctBy { it.hora }
            .sortedBy { it.hora }
            .forEach { d ->
                val base = if (d.frecuencia == Dosis.CADA_X && d.fechaBase <= 0) hoy else d.fechaBase
                val id = dosisDao.insertar(
                    d.copy(medicamentoId = medicamentoId, fechaBase = base)
                ).toInt()
                notifHelper.programarAlarma(id, d.hora, d.frecuencia, d.diasSemana, d.intervaloDias, base)
            }
    }

    suspend fun tomarDosis(medicamento: Medicamento): String {
        val unidades = if (medicamento.unidadesPorToma > 0) medicamento.unidadesPorToma else 1

        if (medicamento.stockActual <= 0) {
            return contexto.getString(R.string.msj_sin_stock_tomar, medicamento.nombre)
        }
        if (medicamento.stockActual < unidades) {
            return contexto.getString(
                R.string.msj_stock_insuficiente,
                medicamento.nombre,
                medicamento.stockActual,
                unidades
            )
        }

        registroDao.insertar(
            RegistroToma(
                medicamentoId = medicamento.id,
                tomado = true,
                tipo = RegistroToma.TOMA,
                unidades = -unidades
            )
        )
        medDao.actualizarStock(medicamento.id, -unidades)

        val actualizado = medDao.obtenerPorId(medicamento.id)
        if (actualizado != null) {
            if (actualizado.stockActual <= 0) {
                notifHelper.mostrarAlertaSinStock(actualizado.nombre, actualizado.id)
            } else if (actualizado.stockActual <= actualizado.stockMinimo) {
                notifHelper.mostrarAlertaStockBajo(
                    actualizado.nombre,
                    actualizado.stockActual,
                    actualizado.id,
                    calcularDiasRestantes(actualizado)
                )
            }
        }

        return contexto.getString(R.string.msj_dosis_registrada, medicamento.nombre)
    }

    /** Ajusta el stock con un delta (positivo reabastece, negativo descuenta). */
    suspend fun ajustarStock(medicamento: Medicamento, delta: Int): String {
        if (delta == 0) return contexto.getString(R.string.msj_sin_cambios, medicamento.nombre)
        medDao.actualizarStock(medicamento.id, delta)
        if (delta > 0) {
            registroDao.insertar(
                RegistroToma(
                    medicamentoId = medicamento.id,
                    tomado = true,
                    tipo = RegistroToma.REABASTECIMIENTO,
                    unidades = delta
                )
            )
            return contexto.getString(R.string.msj_reabastecido, delta, medicamento.nombre)
        }
        return contexto.getString(R.string.msj_reducido, -delta, medicamento.nombre)
    }

    suspend fun eliminarMedicamento(medicamento: Medicamento) {
        val dosis = dosisDao.obtenerPorMedicamentoUnaVez(medicamento.id)
        dosis.forEach { notifHelper.cancelarAlarma(it.id) }
        medDao.desactivar(medicamento.id)
        dosisDao.eliminarPorMedicamento(medicamento.id)
        registroDao.eliminarPorMedicamento(medicamento.id)
    }

    suspend fun reprogramarTodasLasAlarmas() {
        val dosis = dosisDao.obtenerTodasActivas()
        dosis.forEach {
            notifHelper.programarAlarma(it.id, it.hora, it.frecuencia, it.diasSemana, it.intervaloDias, it.fechaBase)
        }
    }

    /** Dosis activas con su medicamento, para calcular el próximo recordatorio. */
    suspend fun obtenerHorariosParaProximo(): List<Pair<Medicamento, Dosis>> {
        val medicamentos = medDao.obtenerTodosUnaVez().associateBy { it.id }
        return dosisDao.obtenerTodasActivas()
            .mapNotNull { dosis -> medicamentos[dosis.medicamentoId]?.let { it to dosis } }
    }

    suspend fun obtenerDosisPara(medicamentoId: Int): List<Dosis> =
        dosisDao.obtenerPorMedicamentoUnaVez(medicamentoId)

    suspend fun obtenerMedicamento(id: Int): Medicamento? = medDao.obtenerPorId(id)

    fun obtenerHistorialFlow(): Flow<List<RegistroTomaConMedicamento>> = registroDao.obtenerTodos()

    suspend fun obtenerRegistrosRango(inicio: Long, fin: Long): List<RegistroToma> =
        registroDao.obtenerRango(inicio, fin)

    suspend fun limpiarHistorial(antesDe: Long) {
        registroDao.eliminarAntiguos(antesDe)
    }

    /** Marca como omitida la dosis actual (acción "Omitir" desde la notificación). */
    suspend fun marcarOmitidaAhora(medicamentoId: Int) {
        if (medDao.obtenerPorId(medicamentoId) == null) return
        registroDao.insertar(
            RegistroToma(
                medicamentoId = medicamentoId,
                tomado = false,
                tipo = RegistroToma.TOMA,
                unidades = 0
            )
        )
    }

    /**
     * Marca como "omitido" cada dosis pasada de hoy que no tenga registro y que corresponda
     * programarse hoy. Se excluye la dosis que acaba de disparar la alarma (excluirDosisId).
     */
    suspend fun marcarOmitidasMedicamento(
        medicamento: Medicamento,
        excluirDosisId: Int? = null
    ) {
        val ahora = Calendar.getInstance()
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        dosisDao.obtenerPorMedicamentoUnaVez(medicamento.id).forEach { dosis ->
            if (dosis.id == excluirDosisId) return@forEach
            if (!ProgramacionDosis.dosisAplicaHoy(dosis, hoy)) return@forEach

            val partes = dosis.hora.split(":")
            val h = partes.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val m = partes.getOrNull(1)?.toIntOrNull() ?: return@forEach
            val horaDosis = hoy.clone() as Calendar
            horaDosis.set(Calendar.HOUR_OF_DAY, h)
            horaDosis.set(Calendar.MINUTE, m)

            // Solo dosis cuya hora ya pasó y sin registro en una ventana generosa
            if (horaDosis.before(ahora)) {
                val inicio = horaDosis.timeInMillis
                val fin = inicio + 90 * 60_000L
                if (registroDao.contarEnRango(medicamento.id, inicio, fin) == 0) {
                    registroDao.insertar(
                        RegistroToma(
                            medicamentoId = medicamento.id,
                            fechaHora = inicio,
                            tomado = false,
                            tipo = RegistroToma.TOMA,
                            unidades = 0
                        )
                    )
                }
            }
        }
    }

    /** Días estimados de stock: stock / dosis al día. -1 si no hay dosis. */
    suspend fun calcularDiasRestantes(medicamento: Medicamento): Int {
        val dosis = dosisDao.obtenerPorMedicamentoUnaVez(medicamento.id)
        if (dosis.isEmpty()) return -1
        val dosisPorDia = dosis.sumOf { d -> dosisPromedioDiarias(d) }
        if (dosisPorDia <= 0.0) return -1
        return (medicamento.stockActual / dosisPorDia).toInt()
    }

    /** Promedio de veces que una dosis se toma al día según su frecuencia. */
    private fun dosisPromedioDiarias(d: Dosis): Double = when (d.frecuencia) {
        Dosis.SEMANAL ->
            if (d.diasSemana == 0) 0.0 else Integer.bitCount(d.diasSemana) / 7.0
        Dosis.CADA_X ->
            if (d.intervaloDias > 1) 1.0 / d.intervaloDias else 1.0
        else -> 1.0
    }

    /** Resumen del día: (tomadas, omitidas) para la notificación de fin de día. */
    suspend fun resumenDeHoy(): Pair<Int, Int> {
        val hoy = ProgramacionDosis.inicioDia(Calendar.getInstance()).timeInMillis
        val manana = hoy + 86_400_000L
        val registros = registroDao.obtenerDelDia(hoy, manana)
            .filter { it.tipo == RegistroToma.TOMA }
        return Pair(
            registros.count { it.tomado },
            registros.count { !it.tomado }
        )
    }
}