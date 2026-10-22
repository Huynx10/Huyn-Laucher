package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TileDao {
    @Query("SELECT * FROM tiles ORDER BY orderIndex ASC")
    fun getAllTilesFlow(): Flow<List<TileEntity>>

    @Query("SELECT * FROM tiles ORDER BY orderIndex ASC")
    suspend fun getAllTiles(): List<TileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tile: TileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiles: List<TileEntity>)

    @Update
    suspend fun update(tile: TileEntity)

    @Delete
    suspend fun delete(tile: TileEntity)

    @Query("DELETE FROM tiles WHERE id = :tileId")
    suspend fun deleteById(tileId: String)

    @Query("DELETE FROM tiles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tiles")
    suspend fun getCount(): Int
}
