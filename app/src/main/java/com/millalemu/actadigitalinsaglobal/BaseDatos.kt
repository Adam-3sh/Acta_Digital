package com.millalemu.actadigitalinsaglobal

import android.content.Context
import androidx.room.*

@Entity(tableName = "actas")
data class ActaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // Encabezado
    val folio: String,
    val registro: String,

    // Checkboxes
    val tipoServicio: String,

    // Datos Maquinaria (Columna Izq)
    val tipoUnidad: String,
    val patente: String,
    val mandante: String,
    // RUT Mandante ELIMINADO
    val obsLogisticas: String,

    // Datos Maquinaria (Columna Der)
    val marca: String,
    val modelo: String,
    val horometro: String,
    val pinFabricante: String,

    // Datos Instalación
    val lugarInst: String,
    val fechaInst: String,
    val nSistema: String,
    val nPrecinto: String,
    val obsTecnicas: String,
    val capCilindro: String,

    // Firmas
    val nombreTecnico: String,
    val runTecnico: String,
    val firmaTecnicoB64: String? = null,

    val nombreSupervisor: String,
    val runSupervisor: String,
    val firmaSupervisorB64: String? = null,

    val nombreJefe: String,
    val runJefe: String,
    val firmaJefeB64: String? = null,

    val nombreCliente: String,
    val runCliente: String,
    val firmaClienteB64: String? = null,

    // Footer
    val obsEntrega: String
)

@Dao
interface ActaDao {
    @Query("SELECT * FROM actas ORDER BY id DESC")
    suspend fun obtenerTodas(): List<ActaEntity>

    @Insert
    suspend fun insertar(acta: ActaEntity)

    @Delete
    suspend fun borrar(acta: ActaEntity)
}

@Database(entities = [ActaEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun actaDao(): ActaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "actas_db_v2"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}