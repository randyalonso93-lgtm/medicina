package com.dosis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DosisDao {

    @Insert
    suspend fun insertar(dosis: Dosis): Long

    @Query("SELECT * FROM dosis WHERE medicamentoId = :medicamentoId ORDER BY hora ASC")
    suspend fun obtenerPorMedicamentoUnaVez(medicamentoId: Int): List<Dosis>

    @Query("SELECT * FROM dosis WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Dosis?

    @Query("""
        SELECT d.* FROM dosis d
        INNER JOIN medicamentos m ON m.id = d.medicamentoId
        WHERE m.activo = 1
        ORDER BY d.hora ASC
    """)
    suspend fun obtenerTodasActivas(): List<Dosis>

    @Query("DELETE FROM dosis WHERE medicamentoId = :medicamentoId")
    suspend fun eliminarPorMedicamento(medicamentoId: Int)
}