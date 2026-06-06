package com.acepero13.android.gamereviewer.data.repository

import com.acepero13.android.gamereviewer.data.db.SnippetDao
import com.acepero13.android.gamereviewer.data.model.Snippet
import kotlinx.coroutines.flow.Flow

class SnippetRepository(private val dao: SnippetDao) {

    fun observeAll(): Flow<List<Snippet>> = dao.observeAll()

    suspend fun getAll(): List<Snippet> = dao.getAll()

    suspend fun findById(id: Long): Snippet? = dao.findById(id)

    suspend fun insert(snippet: Snippet): Long = dao.insert(snippet)

    suspend fun update(snippet: Snippet) = dao.update(snippet)

    suspend fun delete(snippet: Snippet) = dao.delete(snippet)

    suspend fun deleteBySourceGameId(gameId: Long) = dao.deleteBySourceGameId(gameId)
}
