package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.domain.extractMoveAnnotations
import com.acepero13.android.gamereviewer.domain.extractUciMovesFromFullPgn
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.pgn.PgnImporter
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board

private const val TAG = "GameSequenceBuilder"

internal class GameSequenceBuilder(
    private val session: GameSession,
    private val annotationDao: PositionAnnotationDao,
) {

    suspend fun buildMoveLists(game: ReviewGame) {
        val rawUci = if (game.movesUci.isNotBlank()) game.movesUci
                     else extractUciMovesFromFullPgn(game.pgn)
        session.uciMoves = rawUci.split(' ').filter { it.isNotBlank() }
        buildFenAndSanSequence()
        session.pgnAnnotations = PgnImporter().parseGame(game.pgn)
            ?.let { extractMoveAnnotations(it.movesPgn) } ?: emptyMap()
    }

    private fun buildFenAndSanSequence() {
        val fens  = mutableListOf(session.startFen)
        val sans  = mutableListOf<String>()
        val board = Board().apply { loadFromFen(session.startFen) }
        for ((idx, uci) in session.uciMoves.withIndex()) {
            sans.add(runCatching { ChessUtils.uciToSan(board, uci) }.getOrDefault(uci))
            val move = UciMoveUtils.uciToMove(board, uci) ?: run {
                Log.e(TAG, "buildFenAndSanSequence: null move '$uci' at $idx"); break
            }
            if (!board.doMove(move)) { Log.e(TAG, "doMove failed '$uci' at $idx"); break }
            fens.add(board.fen)
        }
        session.fenSequence = fens
        session.sanMoves    = sans
    }

    suspend fun prewarmAnnotationCache() {
        for (fen in session.fenSequence) {
            if (!session.annotationCache.containsKey(fen)) {
                session.annotationCache[fen] = annotationDao.getByFen(fen)
            }
        }
    }
}
