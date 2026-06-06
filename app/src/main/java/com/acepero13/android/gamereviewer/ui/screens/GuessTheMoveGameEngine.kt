package com.acepero13.android.gamereviewer.ui.screens

import com.acepero13.chess.core.pgn.SolutionTreeBuilder
import com.acepero13.chess.core.ui.components.TreeDisplayItem
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.move.Move

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

internal data class GameSequences(val fens: List<String>, val sans: List<String>)

internal data class MoveRevealData(
    val postFen: String,
    val masterSan: String,
    val masterMove: Move?,
    val userSan: String = "",
    val isExact: Boolean = false,
)

internal class GuessTheMoveGameEngine {

    private val treeBuilder = SolutionTreeBuilder()

    fun buildSequences(uciMoves: List<String>): GameSequences {
        val fens = mutableListOf(START_FEN)
        val sans = mutableListOf<String>()
        var fen  = START_FEN
        for (uci in uciMoves) {
            sans.add(runCatching { ChessUtils.uciToSan(Board().apply { loadFromFen(fen) }, uci) }.getOrDefault(uci))
            fen = treeBuilder.applyUci(fen, uci) ?: break
            fens.add(fen)
        }
        return GameSequences(fens, sans)
    }

    fun computeMoveReveal(preFen: String, userMove: Move, masterUci: String): MoveRevealData {
        val userUci   = buildUci(userMove)
        val userSan   = runCatching { ChessUtils.uciToSan(Board().apply { loadFromFen(preFen) }, userUci) }.getOrDefault(userUci)
        val masterSan = runCatching { ChessUtils.uciToSan(Board().apply { loadFromFen(preFen) }, masterUci) }.getOrDefault(masterUci)
        val postFen   = treeBuilder.applyUci(preFen, masterUci) ?: run {
            Board().apply { loadFromFen(preFen); doMove(userMove) }.fen
        }
        return MoveRevealData(postFen, masterSan, findMove(preFen, masterUci), userSan, userUci == masterUci)
    }

    fun computeSkipReveal(preFen: String, masterUci: String): MoveRevealData {
        val masterSan = runCatching { ChessUtils.uciToSan(Board().apply { loadFromFen(preFen) }, masterUci) }.getOrDefault(masterUci)
        val postFen   = treeBuilder.applyUci(preFen, masterUci) ?: preFen
        return MoveRevealData(postFen, masterSan, findMove(preFen, masterUci))
    }

    fun applyUci(fen: String, uci: String): String? = treeBuilder.applyUci(fen, uci)

    fun resolveLastMove(fens: List<String>, moves: List<String>, index: Int): Move? {
        if (index <= 0) return null
        return findMove(fens.getOrElse(index - 1) { START_FEN }, moves.getOrNull(index - 1) ?: return null)
    }

    fun buildTreeItems(
        upTo: Int, current: Int,
        allSan: List<String>, allFen: List<String>,
        annotations: Map<Int, String>,
    ): List<TreeDisplayItem> {
        val items  = mutableListOf<TreeDisplayItem>()
        var moveNo = 1
        for (idx in 0 until minOf(upTo, allSan.size)) {
            val isWhite = idx % 2 == 0
            items.add(TreeDisplayItem.MoveItem(
                nodeId = (idx + 1).toLong(), san = allSan[idx],
                fen = allFen.getOrElse(idx + 1) { START_FEN }, comment = annotations[idx] ?: "",
                hasAnnotations = (annotations[idx] ?: "").isNotBlank(),
                isCurrentMove = (idx + 1) == current, depth = 0,
                moveNumber = moveNo, isWhiteMove = isWhite, showMoveNumber = isWhite,
            ))
            if (!isWhite) moveNo++
        }
        return items
    }

    private fun findMove(fen: String, uci: String): Move? = runCatching {
        Board().apply { loadFromFen(fen) }.legalMoves().firstOrNull { m ->
            val u = "${m.from.name.lowercase()}${m.to.name.lowercase()}"
            u == uci.take(4) || "${u}${m.promotion.fenSymbol.lowercase()}" == uci
        }
    }.getOrNull()

    private fun buildUci(move: Move): String =
        "${move.from.name.lowercase()}${move.to.name.lowercase()}" +
            if (move.promotion != Piece.NONE) move.promotion.fenSymbol.lowercase() else ""
}
