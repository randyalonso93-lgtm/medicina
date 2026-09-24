package com.dosis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dosis.data.Dosis
import com.dosis.data.Medicamento
import com.dosis.data.MedicamentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel de la pantalla de alta/edición de medicamento. */
class FormMedicamentoViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = MedicamentoRepository(app)

    private val _medicamento = MutableStateFlow<Medicamento?>(null)
    val medicamento: StateFlow<Medicamento?> = _medicamento.asStateFlow()

    private val _dosis = MutableStateFlow<List<Dosis>>(emptyList())
    val dosis: StateFlow<List<Dosis>> = _dosis.asStateFlow()

    fun cargar(medicamentoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _medicamento.value = repository.obtenerMedicamento(medicamentoId)
            _dosis.value = repository.obtenerDosisPara(medicamentoId)
        }
    }

    suspend fun guardar(medicamento: Medicamento, dosis: List<Dosis>): String =
        repository.guardarMedicamento(medicamento, dosis)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                FormMedicamentoViewModel(
                    app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                )
            }
        }
    }
}