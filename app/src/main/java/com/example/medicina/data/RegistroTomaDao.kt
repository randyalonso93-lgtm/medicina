package com.example.medicina.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroTomaDao {

    @Insert
    suspend fun insertar(registro: RegistroToma)

    @Query("SELECT * FROM registros_toma WHERE medicamentoId = :medicamentoId ORDER BY fechaHora DESC")
    fun obtenerPorMedicamento(medicamentoId: Int): Flow<List<RegistroToma>>

    @Query("""
        SELECT * FROM registros_toma 
        WHERE fechaHora >= :inicioDia AND fechaHora < :finDia 
        ORDER BY fechaHora DESC
    """)
    suspend fun obtenerDelDia(inicioDia: Long, finDia: Long): List<RegistroToma>

    @Query("DELETE FROM registros_toma WHERE medicamentoId = :medicamentoId")
    suspend fun eliminarPorMedicamento(medicamentoId: Int)
}