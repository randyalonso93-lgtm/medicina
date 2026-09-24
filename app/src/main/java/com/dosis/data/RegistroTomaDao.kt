package com.dosis.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroTomaDao {

    @Insert
    suspend fun insertar(registro: RegistroToma)

    @Query("SELECT COUNT(*) FROM registros_toma WHERE medicamentoId = :medicamentoId AND fechaHora >= :inicio AND fechaHora < :fin")
    suspend fun contarEnRango(medicamentoId: Int, inicio: Long, fin: Long): Int

    @Query("""
        SELECT * FROM registros_toma 
        WHERE fechaHora >= :inicioDia AND fechaHora < :finDia 
        ORDER BY fechaHora DESC
    """)
    suspend fun obtenerDelDia(inicioDia: Long, finDia: Long): List<RegistroToma>

    @Query("""
        SELECT * FROM registros_toma
        WHERE fechaHora >= :inicio AND fechaHora < :fin
        ORDER BY fechaHora ASC
    """)
    suspend fun obtenerRango(inicio: Long, fin: Long): List<RegistroToma>

    @Query("DELETE FROM registros_toma WHERE medicamentoId = :medicamentoId")
    suspend fun eliminarPorMedicamento(medicamentoId: Int)

    @Query("DELETE FROM registros_toma WHERE fechaHora < :antesDe")
    suspend fun eliminarAntiguos(antesDe: Long)

    @Query("""
        SELECT rg.id, rg.medicamentoId, rg.fechaHora, rg.tomado, rg.tipo, rg.unidades, m.nombre AS nombreMedicamento
        FROM registros_toma rg
        INNER JOIN medicamentos m ON m.id = rg.medicamentoId
        ORDER BY rg.fechaHora DESC
        LIMIT 200
    """)
    fun obtenerTodos(): Flow<List<RegistroTomaConMedicamento>>
}