package com.dosis.util

import android.content.Context
import android.net.Uri
import com.dosis.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream

/** Exporta/importa la base de datos local a través de un URI (SAF). */
object BackupHelper {

    private fun archivoDb(contexto: Context) =
        contexto.getDatabasePath("dosis_db")

    suspend fun exportar(contexto: Context, destino: Uri): Boolean = withContext(Dispatchers.IO) {
        val origen = archivoDb(contexto)
        if (!origen.exists()) return@withContext false

        // Fuerza un checkpoint para volcar el WAL al archivo principal antes de copiar.
        runCatching {
            val db = AppDatabase.obtenerInstancia(contexto).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                cursor.moveToFirst()
            }
        }

        val fuente = FileInputStream(origen)
        val salida = contexto.contentResolver.openOutputStream(destino)
            ?: return@withContext fuente.use { false }
        fuente.use { f ->
            salida.use { s ->
                f.copyTo(s)
            }
        }
        true
    }

    /**
     * Restaura una copia. Cierra la DB actual, elimina WAL/SHM previos para evitar que
     * SQLite reaplique una base antigua sobre la copia restaurada, y copia el archivo.
     */
    suspend fun importar(contexto: Context, origen: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            AppDatabase.cerrarInstancia()
            val destino = archivoDb(contexto)
            listOf("dosis_db-wal", "dosis_db-shm", "dosis_db-journal").forEach { nombre ->
                contexto.getDatabasePath(nombre).delete()
            }
            contexto.contentResolver.openInputStream(origen)?.use { entrada ->
                FileOutputStream(destino).use { salida -> entrada.copyTo(salida) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}