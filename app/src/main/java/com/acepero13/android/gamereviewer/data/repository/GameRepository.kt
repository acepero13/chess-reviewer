package com.acepero13.android.gamereviewer.data.repository

import com.acepero13.android.gamereviewer.data.db.ReviewGameDao
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: ReviewGameDao) {

    fun observeAll(): Flow<List<ReviewGame>> = dao.observeAll()

    fun countAll(): Flow<Int> = dao.countAll()

    suspend fun findById(id: Long): ReviewGame? = dao.findById(id)

    suspend fun insert(game: ReviewGame): Long = dao.insert(game)

    suspend fun insertAll(games: List<ReviewGame>): List<Long> = dao.insertAll(games)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun isDuplicate(sourceType: String, sourceId: String): Boolean =
        dao.findBySourceId(sourceType, sourceId) != null
}
