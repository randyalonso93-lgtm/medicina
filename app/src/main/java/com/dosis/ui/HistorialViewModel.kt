package com.dosis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dosis.data.Medicamento
import com.dosis.data.MedicamentoRepository
import com.dosis.data.RegistroTomaConMedicamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class HistorialViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = MedicamentoRepository(app)

    private val _filtro = MutableStateFlow<Int?>(null)
    val filtro: StateFlow<Int?> = _filtro.asStateFlow()

    val registros: Flow<List<RegistroTomaConMedicamento>> = combine(
        repository.obtenerHistorialFlow(),
        _filtro
    ) { lista, id ->
        if (id == null) lista else lista.filter { it.medicamentoId == id }
    }

    val medicamentos: Flow<List<Medicamento>> = repository.obtenerMedicamentosSimple()

    fun setFiltro(medicamentoId: Int?) {
        _filtro.value = medicamentoId
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HistorialViewModel(
                    app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                )
            }
        }
    }
}