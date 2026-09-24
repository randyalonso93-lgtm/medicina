package com.dosis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Medicamento::class, RegistroToma::class, Dosis::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun registroTomaDao(): RegistroTomaDao
    abstract fun dosisDao(): DosisDao

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        /**
         * v1 -> v2: las horas pasan de `medicamentos.hora` (una sola) a la tabla `dosis`.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_tmp_hora` (`medicamentoId` INTEGER NOT NULL, `hora` TEXT NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `_tmp_hora` (`medicamentoId`, `hora`) SELECT `id`, `hora` FROM `medicamentos`"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `_tmp_regs` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `medicamentoId` INTEGER NOT NULL,
                        `fechaHora` INTEGER NOT NULL,
                        `tomado` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `_tmp_regs` (`id`, `medicamentoId`, `fechaHora`, `tomado`) SELECT `id`, `medicamentoId`, `fechaHora`, `tomado` FROM `registros_toma`"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medicamentos_nuevo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `dosis` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `stockActual` INTEGER NOT NULL, `stockMinimo` INTEGER NOT NULL, `activo` INTEGER NOT NULL, `fechaCreacion` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `medicamentos_nuevo` (`id`, `nombre`, `dosis`, `descripcion`, `stockActual`, `stockMinimo`, `activo`, `fechaCreacion`) SELECT `id`, `nombre`, `dosis`, `descripcion`, `stockActual`, `stockMinimo`, `activo`, `fechaCreacion` FROM `medicamentos`"
                )

                db.execSQL("DROP TABLE `registros_toma`")
                db.execSQL("DROP TABLE `medicamentos`")

                db.execSQL("ALTER TABLE `medicamentos_nuevo` RENAME TO `medicamentos`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dosis` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicamentoId` INTEGER NOT NULL,
                        `hora` TEXT NOT NULL,
                        FOREIGN KEY(`medicamentoId`) REFERENCES `medicamentos`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_dosis_medicamentoId` ON `dosis` (`medicamentoId`)"
                )
                db.execSQL("INSERT INTO `dosis` (`medicamentoId`, `hora`) SELECT `medicamentoId`, `hora` FROM `_tmp_hora`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `registros_toma` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicamentoId` INTEGER NOT NULL,
                        `fechaHora` INTEGER NOT NULL,
                        `tomado` INTEGER NOT NULL,
                        FOREIGN KEY(`medicamentoId`) REFERENCES `medicamentos`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `registros_toma` (`id`, `medicamentoId`, `fechaHora`, `tomado`) SELECT `id`, `medicamentoId`, `fechaHora`, `tomado` FROM `_tmp_regs`"
                )

                db.execSQL("DROP TABLE `_tmp_hora`")
                db.execSQL("DROP TABLE `_tmp_regs`")
            }
        }

        /**
         * v2 -> v3: índice sobre `registros_toma.medicamentoId`.
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_registros_toma_medicamentoId` ON `registros_toma` (`medicamentoId`)"
                )
            }
        }

        /**
         * v3 -> v4: nuevas columnas — unidades por toma (medicamentos),
         * frecuencia/días/intervalo por dosis y tipo/unidades de evento en el historial.
         */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `medicamentos` ADD COLUMN `unidadesPorToma` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL("ALTER TABLE `dosis` ADD COLUMN `frecuencia` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `dosis` ADD COLUMN `diasSemana` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `dosis` ADD COLUMN `intervaloDias` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `dosis` ADD COLUMN `fechaBase` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `registros_toma` ADD COLUMN `tipo` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `registros_toma` ADD COLUMN `unidades` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun obtenerInstancia(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dosis_db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
                    .build()
                    .also { INSTANCIA = it }
            }
        }

        /** Cierra y descarta la instancia (usado al restaurar una copia). */
        fun cerrarInstancia() {
            synchronized(this) {
                INSTANCIA?.close()
                INSTANCIA = null
            }
        }
    }
}