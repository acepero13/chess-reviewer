package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.acepero13.android.gamereviewer.data.model.Snippet
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snippet: Snippet): Long

    @Update
    suspend fun update(snippet: Snippet)

    @Delete
    suspend fun delete(snippet: Snippet)

    @Query("SELECT * FROM snippets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets ORDER BY createdAt DESC")
    suspend fun getAll(): List<Snippet>

    @Query("SELECT * FROM snippets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Snippet?

    @Query("DELETE FROM snippets WHERE sourceGameId = :gameId")
    suspend fun deleteBySourceGameId(gameId: Long)
}
