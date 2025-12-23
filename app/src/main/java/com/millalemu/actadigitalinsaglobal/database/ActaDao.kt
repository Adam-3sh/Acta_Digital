package com.millalemu.actadigitalinsaglobal.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActaDao {
    // CORREGIDO: Ahora busca en "actas"
    @Query("SELECT * FROM actas ORDER BY id DESC")
    suspend fun getAll(): List<ActaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(acta: ActaEntity)

    @Delete
    suspend fun delete(acta: ActaEntity)
}