package com.millalemu.actadigitalinsaglobal.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Aquí registramos la Entity (versión 1)
@Database(entities = [ActaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Conectamos el DAO
    abstract fun actaDao(): ActaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "acta_database" // Nombre del archivo físico de la BD en el celular
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}