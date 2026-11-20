package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT id FROM favorites WHERE isSeries = 0")
    fun getFavoriteMovieIds(): Flow<List<String>>

    @Query("SELECT id FROM favorites WHERE isSeries = 1")
    fun getFavoriteSeriesIds(): Flow<List<String>>

    @Query("SELECT * FROM favorites WHERE isSeries = 1")
    suspend fun getSeriesFavoritesSync(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: String)
}
