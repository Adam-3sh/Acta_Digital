package com.millalemu.actadigitalinsaglobal.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actas")
data class ActaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "fecha") val fecha: String,
    @ColumnInfo(name = "firma_path") val firmaPath: String? = null // Campo nuevo para la firma
)