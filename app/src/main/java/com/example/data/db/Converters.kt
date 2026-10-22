package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.TileSize
import com.example.data.model.TileType

class Converters {
    @TypeConverter
    fun fromTileSize(size: TileSize): String = size.name

    @TypeConverter
    fun toTileSize(value: String): TileSize = try {
        TileSize.valueOf(value)
    } catch (_: Exception) {
        TileSize.MEDIUM
    }

    @TypeConverter
    fun fromTileType(type: TileType): String = type.name

    @TypeConverter
    fun toTileType(value: String): TileType = try {
        TileType.valueOf(value)
    } catch (_: Exception) {
        TileType.APP
    }
}
