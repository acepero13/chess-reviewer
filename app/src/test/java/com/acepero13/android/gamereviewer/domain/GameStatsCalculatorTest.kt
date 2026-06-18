package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStatsCalculatorTest {

    private fun game(white: String = "me", black: String = "rival") = ReviewGame(
        id = 1L,
        whitePlayer = white,
        blackPlayer = black,
        result = "1-0",
        date = "2026.01.01",
        event = "Test",
        movesUci = "",
        pgn = "",
        sourceType = "file",
    )

    /** White-perspective eval after each ply; deltas derived from consecutive evals. */
    private fun evals(vararg evalCp: Int): List<GameEvaluation> {
        var prev = 0
        return evalCp.mapIndexed { i, cp ->
            val delta = cp - prev
            prev = cp
            GameEvaluation(gameId = 1L, moveIndex = i + 1, evalCp = cp, evalDelta = delta)
        }
    }

    @Test
    fun `identifies user as black when only black matches username`() {
        val stats = GameStatsCalculator.compute(
            game = game(white = "rival", black = "me"),
            evaluations = evals(20, 10, 30, 5),
            moveTimes = emptyList(),
            username = "me",
            analysisDepth = 12,
        )
        assertTrue(!stats.playerIsWhite)
    }

    @Test
    fun `defaults to white when username matches neither side`() {
        val stats = GameStatsCalculator.compute(
            game = game(white = "a", black = "b"),
            evaluations = evals(20, 10),
            moveTimes = emptyList(),
            username = "nobody",
            analysisDepth = 12,
        )
        assertTrue(stats.playerIsWhite)
    }

    @Test
    fun `counts a white blunder`() {
        // Ply1 (white): eval goes 0 -> +30 (good). Ply3 (white): +30 -> -300 (lost ~330cp, blunder).
        // Ply2/Ply4 are black's moves and must not affect white's counts.
        val list = evals(30, 40, -300, -290)
        val stats = GameStatsCalculator.compute(
            game = game(),
            evaluations = list,
            moveTimes = emptyList(),
            username = "me",
            analysisDepth = 12,
        )
        assertTrue(stats.playerIsWhite)
        assertEquals(1, stats.blunders)
    }

    @Test
    fun `perfect play yields ~100 accuracy and zero acpl`() {
        // Eval never moves against white on white's moves.
        val list = evals(50, 50, 50, 50)
        val stats = GameStatsCalculator.compute(
            game = game(),
            evaluations = list,
            moveTimes = emptyList(),
            username = "me",
            analysisDepth = 12,
        )
        assertEquals(0, stats.acpl)
        assertEquals(0, stats.blunders)
        assertTrue("accuracy should be high", stats.accuracy > 95f)
    }
}
