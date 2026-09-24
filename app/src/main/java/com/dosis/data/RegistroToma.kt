package com.dosis.data

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
    ],
    indices = [androidx.room.Index("medicamentoId")]
)
data class RegistroToma(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicamentoId: Int,
    val fechaHora: Long = System.currentTimeMillis(),
    val tomado: Boolean = true,   // true = tomado, false = omitido
    val tipo: Int = TOMA,         // TOMA / REABASTECIMIENTO
    val unidades: Int = 1         // +N al reabastecer, -N al tomar
) {
    companion object {
        const val TOMA = 0
        const val REABASTECIMIENTO = 1
    }
}