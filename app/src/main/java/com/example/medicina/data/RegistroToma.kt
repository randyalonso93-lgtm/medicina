package com.example.medicina.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_toma",
    foreignKeys = [
        ForeignKey(
            entity = Medicamento::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RegistroToma(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicamentoId: Int,
    val fechaHora: Long = System.currentTimeMillis(),
    val tomado: Boolean = true   // true = tomado, false = omitido
)