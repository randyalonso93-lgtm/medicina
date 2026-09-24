package com.dosis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicamentos")
data class Medicamento(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val dosis: String,
    val descripcion: String = "",
    val stockActual: Int = 0,
    val stockMinimo: Int = 5,
    val unidadesPorToma: Int = 1,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
)