package com.example.medicina.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos WHERE activo = 1 ORDER BY hora ASC")
    fun obtenerTodos(): Flow<List<Medicamento>>

    @Query("SELECT * FROM medicamentos WHERE activo = 1 ORDER BY hora ASC")
    suspend fun obtenerTodosUnaVez(): List<Medicamento>

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Medicamento?

    @Insert
    suspend fun insertar(medicamento: Medicamento): Long

    @Update
    suspend fun actualizar(medicamento: Medicamento)

    @Query("UPDATE medicamentos SET stockActual = stockActual + :cantidad WHERE id = :id")
    suspend fun actualizarStock(id: Int, cantidad: Int)

    @Query("UPDATE medicamentos SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Int)

    @Delete
    suspend fun eliminar(medicamento: Medicamento)

    @Query("SELECT * FROM medicamentos WHERE stockActual <= stockMinimo AND activo = 1")
    suspend fun obtenerStockBajo(): List<Medicamento>

    @Query("SELECT * FROM medicamentos WHERE stockActual <= 0 AND activo = 1")
    suspend fun obtenerSinStock(): List<Medicamento>
}