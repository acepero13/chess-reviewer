package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/**
 * Detects a discovered attack: moving a piece reveals an attack by a friendly slider
 * (bishop, rook, queen) on a valuable enemy piece.
 */
class DiscoveredAttackRule : HighlightRule {
    override val ruleType = "discovered_attack"

    private val straightDirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val diagDirs     = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -1.0f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()

        // Find where the moved piece came from
        val srcSq = findSourceSquare(context, destSq) ?: return emptyList()

        // For each friendly slider, check if the source square was blocking its ray to a valuable enemy
        for ((sq, piece) in BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)) {
            if (sq == srcSq) continue
            if (piece.pieceType !in listOf(PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN)) continue

            val dirs = when (piece.pieceType) {
                PieceType.ROOK   -> straightDirs
                PieceType.BISHOP -> diagDirs
                PieceType.QUEEN  -> straightDirs + diagDirs
                else             -> continue
            }

            for ((dFile, dRank) in dirs) {
                val rayBefore = BoardAttackHelper.raySquares(context.boardBefore, sq, dFile, dRank)
                if (srcSq !in rayBefore) continue

                val rayAfter = BoardAttackHelper.raySquares(context.boardAfter, sq, dFile, dRank)
                val firstOnRay = rayAfter.firstOrNull { context.boardAfter.getPiece(it) != Piece.NONE } ?: continue
                val target = context.boardAfter.getPiece(firstOnRay)
                if (target == Piece.NONE) continue
                if (target.pieceSide != context.enemyColor) continue
                if (target.materialValue < 3) continue

                val isCheck = target.pieceType == PieceType.KING
                if (!isCheck && BoardAttackHelper.isAttackedBy(context.boardAfter, firstOnRay, context.enemyColor)) continue

                val side       = if (context.isWhiteMove) "White" else "Black"
                val targetName = pieceName(target.pieceType)
                return listOf(
                    GameHighlight(
                        moveIndex      = context.moveIndex,
                        moveNumber     = context.moveNumber,
                        isWhiteMove    = context.isWhiteMove,
                        moveSan        = context.moveSan,
                        fenBefore      = context.fenBefore,
                        phase          = context.phase,
                        ruleType       = ruleType,
                        severity       = if (isCheck) HighlightSeverity.CRITICAL else HighlightSeverity.IMPORTANT,
                        title          = "Discovered attack",
                        description    = "$side's ${context.moveSan} uncovered an attack on the enemy $targetName.",
                        improvementTip = "Discovered attacks are powerful because the opponent must deal with two threats at once."
                    )
                )
            }
        }
        return emptyList()
    }

    private fun findSourceSquare(context: HighlightRuleContext, destSq: Square): Square? {
        val movedPiece = context.boardAfter.getPiece(destSq)
        if (movedPiece == Piece.NONE) return null

        // Collect candidate source squares: same piece type/color in boardBefore, now empty in boardAfter
        val candidates = BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)
            .filter { (sq, p) ->
                sq != destSq &&
                p.pieceType == movedPiece.pieceType &&
                context.boardAfter.getPiece(sq) == Piece.NONE
            }
            .map { it.first }

        if (candidates.size == 1) return candidates.first()

        // Disambiguation via SAN hint
        val san = context.moveSan.trimEnd('+', '#', '?', '!')
        if (san.length >= 4) {
            val hint = san[1]
            if (hint.isLetter()) {
                val hintFile = hint - 'a'
                val match = candidates.firstOrNull { sq -> BoardAttackHelper.fileOf(sq) == hintFile }
                if (match != null) return match
            } else if (hint.isDigit()) {
                val hintRank = hint - '1'
                val match = candidates.firstOrNull { sq -> BoardAttackHelper.rankOf(sq) == hintRank }
                if (match != null) return match
            }
        }

        return candidates.firstOrNull()
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.KING   -> "king"
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        PieceType.PAWN   -> "pawn"
        else             -> "piece"
    }
}
