package com.dosis.data

data class RegistroTomaConMedicamento(
    val id: Int,
    val medicamentoId: Int,
    val fechaHora: Long,
    val tomado: Boolean,
    val tipo: Int,
    val unidades: Int,
    val nombreMedicamento: String
)