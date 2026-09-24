package com.dosis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dosis.R
import com.dosis.data.Dosis
import com.dosis.data.Medicamento
import com.dosis.data.MedicamentoConDosis
import com.dosis.data.MedicamentoRepository
import com.dosis.util.ProgramacionDosis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MedicamentoViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = MedicamentoRepository(app)

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    val medicamentos: Flow<List<MedicamentoConDosis>> = combine(
        repository.obtenerMedicamentos(""),
        _busqueda
    ) { lista, q ->
        if (q.isBlank()) lista
        else lista.filter { it.medicamento.nombre.contains(q, ignoreCase = true) }
    }

    private val _proximoRecordatorio = MutableStateFlow(app.getString(R.string.sin_proximo))
    val proximoRecordatorio: StateFlow<String> = _proximoRecordatorio.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun iniciar() {
        viewModelScope.launch(Dispatchers.IO) {
            actualizarProximo()
            repository.reprogramarTodasLasAlarmas()
        }
    }

    fun setBusqueda(consulta: String) {
        _busqueda.value = consulta
    }

    fun guardar(medicamento: Medicamento, dosis: List<Dosis>) {
        viewModelScope.launch(Dispatchers.IO) {
            _mensajes.tryEmit(repository.guardarMedicamento(medicamento, dosis))
            actualizarProximo()
        }
    }

    fun tomarDosis(medicamento: Medicamento) {
        viewModelScope.launch(Dispatchers.IO) {
            _mensajes.tryEmit(repository.tomarDosis(medicamento))
            actualizarProximo()
        }
    }

    fun ajustarStock(medicamento: Medicamento, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _mensajes.tryEmit(repository.ajustarStock(medicamento, delta))
        }
    }

    fun eliminar(medicamento: Medicamento) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarMedicamento(medicamento)
            _mensajes.tryEmit(app.getString(R.string.msj_eliminado))
            actualizarProximo()
        }
    }

    fun reprogramarTodasLasAlarmas() {
        viewModelScope.launch(Dispatchers.IO) { repository.reprogramarTodasLasAlarmas() }
    }

    fun refrescarProximo() {
        viewModelScope.launch(Dispatchers.IO) { actualizarProximo() }
    }

    suspend fun obtenerDosisPara(medicamentoId: Int): List<Dosis> =
        repository.obtenerDosisPara(medicamentoId)

    suspend fun calcularDiasRestantes(medicamento: Medicamento): Int =
        repository.calcularDiasRestantes(medicamento)

    fun limpiarHistorialAntiguo() {
        viewModelScope.launch(Dispatchers.IO) {
            val antes = System.currentTimeMillis() - 90L * 86_400_000L
            repository.limpiarHistorial(antes)
        }
    }

    private suspend fun actualizarProximo() {
        val ahora = Calendar.getInstance()
        val formato = SimpleDateFormat("h:mm a", Locale.getDefault())

        val candidatos = repository.obtenerHorariosParaProximo()
        val proximo = candidatos
            .mapNotNull { (med, d) ->
                val occ = ProgramacionDosis.proximaOcurrencia(
                    d.hora, d.frecuencia, d.diasSemana, d.intervaloDias, d.fechaBase
                )
                Triple(med, d, occ)
            }
            .minByOrNull { it.third.timeInMillis }
            ?.let { it.first to it.third }

        val texto = if (proximo != null) {
            val (med, occ) = proximo
            val horaFmt = formato.format(occ.time)
            when {
                ProgramacionDosis.esMismoDia(occ, ahora) ->
                    app.getString(R.string.proximo, "$horaFmt — ${med.nombre}")
                ProgramacionDosis.esDiaSiguiente(occ, ahora) ->
                    app.getString(R.string.manana_formato, horaFmt, med.nombre)
                else -> {
                    val fecha =
                        SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(occ.time)
                    app.getString(R.string.proximo_fecha, "$fecha $horaFmt", med.nombre)
                }
            }
        } else {
            app.getString(R.string.sin_proximo)
        }
        _proximoRecordatorio.value = texto
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MedicamentoViewModel(
                    app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                )
            }
        }
    }
}