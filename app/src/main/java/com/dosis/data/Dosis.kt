package com.dosis.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dosis",
    foreignKeys = [
        ForeignKey(
            entity = Medicamento::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicamentoId")]
)
data class Dosis(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicamentoId: Int,
    val hora: String,                 // Formato "HH:mm", ej: "08:30"
    val frecuencia: Int = 0,          // 0=diaria, 1=días de la semana, 2=cada X días
    val diasSemana: Int = 0,          // Bitmask: LUN=1 .. DOMINGO=64
    val intervaloDias: Int = 0,       // Para frecuencia CADA_X
    val fechaBase: Long = 0           // Primer día programado (epoch día) para CADA_X
) {
    companion object {
        const val DIARIA = 0
        const val SEMANAL = 1
        const val CADA_X = 2
    }
}