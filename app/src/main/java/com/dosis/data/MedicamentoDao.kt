package com.dosis.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos WHERE activo = 1 ORDER BY nombre ASC")
    fun obtenerTodos(): Flow<List<Medicamento>>

    @Query("SELECT * FROM medicamentos WHERE activo = 1 ORDER BY nombre ASC")
    suspend fun obtenerTodosUnaVez(): List<Medicamento>

    @Query("""
        SELECT m.*, COALESCE((
            SELECT GROUP_CONCAT(hora, ', ') FROM dosis WHERE medicamentoId = m.id
        ), '') AS horas
        FROM medicamentos m
        WHERE m.activo = 1
            AND (:consulta = '' OR m.nombre LIKE '%' || :consulta || '%')
        ORDER BY (
            SELECT hora FROM dosis WHERE medicamentoId = m.id ORDER BY hora LIMIT 1
        ) ASC
    """)
    fun obtenerTodosConDosis(consulta: String): Flow<List<MedicamentoConDosis>>

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
}