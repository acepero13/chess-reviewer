package com.acepero13.android.gamereviewer.engine.highlights

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.MoveGenerator

// ── Top-level material value extensions (importable by rules) ────────────────

val PieceType.materialValue: Int get() = when (this) {
    PieceType.PAWN   -> 1
    PieceType.KNIGHT -> 3
    PieceType.BISHOP -> 3
    PieceType.ROOK   -> 5
    PieceType.QUEEN  -> 9
    PieceType.KING   -> 0
    else             -> 0
}

val Piece.materialValue: Int get() = pieceType.materialValue

/**
 * Board-geometry utilities shared across [HighlightRule] implementations.
 * All methods operate on already-constructed [Board] objects.
 */
object BoardAttackHelper {

    // ── Square geometry ──────────────────────────────────────────────────────────

    fun fileOf(sq: Square): Int = sq.ordinal % 8
    fun rankOf(sq: Square): Int = sq.ordinal / 8

    fun squareAt(file: Int, rank: Int): Square? {
        if (file !in 0..7 || rank !in 0..7) return null
        return Square.values().getOrNull(rank * 8 + file)
    }

    fun squareName(sq: Square): String = sq.value().lowercase()

    // ── Piece lookup ─────────────────────────────────────────────────────────────

    fun kingSquare(board: Board, side: Side): Square? {
        val target = if (side == Side.WHITE) Piece.WHITE_KING else Piece.BLACK_KING
        return Square.values().filter { it != Square.NONE }.firstOrNull { board.getPiece(it) == target }
    }

    // ── Attack queries ───────────────────────────────────────────────────────────

    /**
     * All squares that [side]'s pieces can move to [square].
     *
     * generateLegalMoves only covers the side currently to move, so when [side] is the
     * non-to-move side we temporarily rebuild the board with sides swapped so that
     * side's legal moves become visible.
     */
    fun attackersOf(board: Board, square: Square, side: Side): List<Square> =
        runCatching {
            val effectiveBoard = boardForSide(board, side)
            MoveGenerator.generateLegalMoves(effectiveBoard)
                .filter { it.to == square && effectiveBoard.getPiece(it.from) != Piece.NONE && effectiveBoard.getPiece(it.from).pieceSide == side }
                .map { it.from }
                .distinct()
        }.getOrDefault(emptyList())

    fun isAttackedBy(board: Board, square: Square, side: Side): Boolean =
        runCatching {
            val effectiveBoard = boardForSide(board, side)
            MoveGenerator.generateLegalMoves(effectiveBoard)
                .any { it.to == square && effectiveBoard.getPiece(it.from) != Piece.NONE && effectiveBoard.getPiece(it.from).pieceSide == side }
        }.getOrDefault(false)

    /** All squares reachable by the piece on [from]. Uses legal moves. */
    fun attacksFrom(board: Board, from: Square): List<Square> =
        runCatching {
            MoveGenerator.generateLegalMoves(board)
                .filter { it.from == from }
                .map { it.to }
        }.getOrDefault(emptyList())

    // ── Ray geometry ─────────────────────────────────────────────────────────────

    /**
     * Returns squares along the ray from [from] in direction (dFile, dRank),
     * stopping after the first occupied square (inclusive).
     */
    fun raySquares(board: Board, from: Square, dFile: Int, dRank: Int): List<Square> {
        val result = mutableListOf<Square>()
        var file = fileOf(from) + dFile
        var rank = rankOf(from) + dRank
        while (file in 0..7 && rank in 0..7) {
            val sq = squareAt(file, rank) ?: break
            result.add(sq)
            if (board.getPiece(sq) != Piece.NONE) break
            file += dFile
            rank += dRank
        }
        return result
    }

    // ── SAN parsing ──────────────────────────────────────────────────────────────

    /** Parses the destination square from a SAN string; null for castling. */
    fun destinationSquare(san: String): Square? {
        if (san.startsWith("O-O") || san.startsWith("0-0")) return null
        val clean = san.trimEnd('+', '#', '!', '?').trimEnd { it == '=' || it.isUpperCase() }
        if (clean.length < 2) return null
        val dest = clean.takeLast(2)
        return runCatching { Square.valueOf(dest.uppercase()) }.getOrNull()
    }

    /**
     * Returns the square where the moved piece landed after [san] was played,
     * verifying a [side]-coloured piece is actually there in [boardAfter].
     */
    fun movedPieceSquare(san: String, boardAfter: Board, side: Side): Square? {
        val dest = destinationSquare(san) ?: return null
        val piece = boardAfter.getPiece(dest)
        return dest.takeIf { piece != Piece.NONE && piece.pieceSide == side }
    }

    // ── Material counting ────────────────────────────────────────────────────────

    fun totalMaterial(board: Board, side: Side): Int =
        Square.values().filter { it != Square.NONE }.sumOf { sq ->
            val p = board.getPiece(sq)
            if (p != Piece.NONE && p.pieceSide == side && p.pieceType != PieceType.KING)
                p.materialValue else 0
        }

    fun countPieceType(board: Board, side: Side, type: PieceType): Int =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side && p.pieceType == type
        }

    // ── Board iteration helpers ──────────────────────────────────────────────────

    /** All non-empty squares for the given side. */
    fun piecesOf(board: Board, side: Side): List<Pair<Square, Piece>> =
        Square.values().filter { it != Square.NONE }.mapNotNull { sq ->
            val p = board.getPiece(sq)
            if (p != Piece.NONE && p.pieceSide == side) sq to p else null
        }

    /** All occupied squares on the board. */
    fun allPieces(board: Board): List<Pair<Square, Piece>> =
        Square.values().filter { it != Square.NONE }.mapNotNull { sq ->
            val p = board.getPiece(sq)
            if (p != Piece.NONE) sq to p else null
        }

    // ── Hanging piece detection ──────────────────────────────────────────────────

    /**
     * Returns true only when a profitable capture exists — the cheapest available
     * attacker can take [piece] on [sq] and come out ahead after any recapture.
     *
     * Naively counting attackers vs. defenders is wrong because [attackersOf] finds
     * pieces via legal moves to [sq]. Friendly pieces can never legally move to a
     * square occupied by another friendly piece, so the defender count is always 0.
     * Instead, we simulate the cheapest capture (which removes [piece] from [sq])
     * and re-count defenders on the now-exposed square.
     */
    fun isGenuinelyHanging(board: Board, sq: Square, piece: Piece): Boolean {
        val pieceSide    = piece.pieceSide
        val attackerSide = if (pieceSide == Side.WHITE) Side.BLACK else Side.WHITE
        // Exclude kings: their materialValue is 0 (same as the NONE/else branch), so minByOrNull
        // would always pick a king as "cheapest" even over a pawn. Kings also can't legally
        // capture defended squares, so recaptorsAfterCapture would return -1 and falsely flag
        // the piece as hanging.
        val nonKingAttackers = attackersOf(board, sq, attackerSide)
            .filter { sq2 ->
                val p = board.getPiece(sq2)
                p != Piece.NONE && p.pieceSide == attackerSide && p.pieceType != PieceType.KING
            }
        if (nonKingAttackers.isEmpty()) return false

        val cheapestAttackerSq = nonKingAttackers.minByOrNull { board.getPiece(it).materialValue }
            ?: return false
        val attackerValue = board.getPiece(cheapestAttackerSq).materialValue
        val captureGain   = piece.materialValue

        val recaptors = recaptorsAfterCapture(board, sq, cheapestAttackerSq, pieceSide)
        if (recaptors < 0) return false   // attacker can't legally make the capture

        return if (recaptors == 0) true else captureGain > attackerValue
    }

    /**
     * Clones the board, applies the capture [attackerSq] → [targetSq], then counts
     * how many [defenderSide] pieces can pseudo-legally move to [targetSq] (potential recaptors).
     *
     * Pseudo-legal moves are used intentionally: we want to count pinned pieces as
     * defenders so that the pre-move checklist only highlights *obviously* undefended
     * pieces. A piece defended by a pinned piece is a subtle tactical motif beyond the
     * scope of a quick board-scan habit prompt.
     *
     * Returns -1 on any error so callers conservatively treat the capture as illegal (not hanging).
     */
    private fun recaptorsAfterCapture(
        board: Board,
        targetSq: Square,
        attackerSq: Square,
        defenderSide: Side,
    ): Int = runCatching {
        val attackerSide = if (defenderSide == Side.WHITE) Side.BLACK else Side.WHITE
        val fen = board.fen
        val cloneFen = if (board.sideToMove != attackerSide) {
            val parts = fen.split(" ")
            if (parts.size >= 2)
                parts.toMutableList().apply { set(1, if (parts[1] == "w") "b" else "w") }.joinToString(" ")
            else fen
        } else fen
        val clone = Board().apply { loadFromFen(cloneFen) }
        val captureMove = MoveGenerator.generateLegalMoves(clone)
            .firstOrNull { it.from == attackerSq && it.to == targetSq } ?: return -1
        clone.doMove(captureMove)
        // After the capture clone.sideToMove == defenderSide, so generatePseudoLegalMoves
        // produces the defender's moves. Pseudo-legal (not strictly legal) so pinned pieces
        // are counted as recaptors — see doc above.
        MoveGenerator.generatePseudoLegalMoves(clone)
            .count { it.to == targetSq && clone.getPiece(it.from).pieceSide == defenderSide }
    }.getOrDefault(-1)

    // ── Internal helpers ─────────────────────────────────────────────────────────

    /**
     * Returns [board] as-is when [side] already has the move, otherwise rebuilds
     * it from a FEN with the side-to-move token swapped so that [side]'s legal
     * moves become visible to [MoveGenerator.generateLegalMoves].
     *
     * Note: flipping the side can leave the king in an "impossible" check from the
     * engine's perspective; [MoveGenerator.generateLegalMoves] handles this gracefully
     * (it filters moves that leave the king in check), so the result is a conservative
     * but correct set of [side]'s attack squares.
     */
    private fun boardForSide(board: Board, side: Side): Board {
        if (board.sideToMove == side) return board
        val fen = board.fen
        val parts = fen.split(" ")
        if (parts.size < 2) return board
        val flipped = parts.toMutableList().apply { set(1, if (parts[1] == "w") "b" else "w") }
            .joinToString(" ")
        return runCatching { Board().apply { loadFromFen(flipped) } }.getOrElse { board }
    }
}
