package com.dosis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dosis.data.MedicamentoRepository
import com.dosis.data.RegistroToma
import com.dosis.util.ProgramacionDosis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class EstadisticasViewModel(private val app: Application) : AndroidViewModel(app) {

    data class Resumen(
        val semanaTomadas: Int = 0,
        val semanaOmitidas: Int = 0,
        val mesTomadas: Int = 0,
        val mesOmitidas: Int = 0,
        val racha: Int = 0
    ) {
        val semanaTotal: Int get() = semanaTomadas + semanaOmitidas
        val mesTotal: Int get() = mesTomadas + mesOmitidas
        val semanaPct: Int get() = if (semanaTotal == 0) 0 else semanaTomadas * 100 / semanaTotal
        val mesPct: Int get() = if (mesTotal == 0) 0 else mesTomadas * 100 / mesTotal
    }

    private val repository = MedicamentoRepository(app)

    private val _resumen = MutableStateFlow(Resumen())
    val resumen: StateFlow<Resumen> = _resumen.asStateFlow()

    fun cargar() {
        viewModelScope.launch(Dispatchers.IO) {
            _resumen.value = calcular()
        }
    }

    private suspend fun calcular(): Resumen {
        val hoy = Calendar.getInstance()
        val inicioHoy = ProgramacionDosis.inicioDia(hoy.timeInMillis)
        val manana = inicioHoy + 86_400_000L
        val MIL_DIA = 86_400_000L

        val regsSemana = repository.obtenerRegistrosRango(inicioHoy - 6 * MIL_DIA, manana)
        val regsMes = repository.obtenerRegistrosRango(inicioHoy - 29 * MIL_DIA, manana)
        val regsRacha = repository.obtenerRegistrosRango(inicioHoy - 400 * MIL_DIA, manana)

        val semanaTomadas = regsSemana.count { it.tipo == RegistroToma.TOMA && it.tomado }
        val semanaOmitidas = regsSemana.count { it.tipo == RegistroToma.TOMA && !it.tomado }
        val mesTomadas = regsMes.count { it.tipo == RegistroToma.TOMA && it.tomado }
        val mesOmitidas = regsMes.count { it.tipo == RegistroToma.TOMA && !it.tomado }

        // Racha: días seguidos (terminando hoy) con al menos una dosis tomada
        val diasTomados = mutableSetOf<Long>()
        for (r in regsRacha) {
            if (r.tipo == RegistroToma.TOMA && r.tomado) {
                diasTomados.add(ProgramacionDosis.inicioDia(r.fechaHora) / MIL_DIA)
            }
        }
        var racha = 0
        var cursor = inicioHoy / MIL_DIA
        while (diasTomados.contains(cursor)) {
            racha++
            cursor--
        }

        return Resumen(
            semanaTomadas, semanaOmitidas,
            mesTomadas, mesOmitidas,
            racha
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EstadisticasViewModel(
                    app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                )
            }
        }
    }
}