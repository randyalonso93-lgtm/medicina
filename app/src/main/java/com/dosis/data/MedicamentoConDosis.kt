package com.dosis.data

import androidx.room.Embedded

data class MedicamentoConDosis(
    @Embedded val medicamento: Medicamento,
    val horas: String
)