package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
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
            pawnStructure = "",
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
            pawnStructure = "",
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
            pawnStructure = "",
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
            pawnStructure = "",
        )
        assertEquals(0, stats.acpl)
        assertEquals(0, stats.blunders)
        assertTrue("accuracy should be high", stats.accuracy > 95f)
    }

    private fun ev(index: Int, cp: Int, delta: Int, motif: String = "mixed") =
        GameEvaluation(gameId = 1L, moveIndex = index, evalCp = cp, evalDelta = delta, motif = motif)

    private fun stats(evals: List<GameEvaluation>, times: List<MoveTimeData> = emptyList()) =
        GameStatsCalculator.compute(
            game = game(), evaluations = evals, moveTimes = times,
            username = "me", analysisDepth = 12, pawnStructure = "",
        )

    @Test
    fun `tags a fork blunder by engine motif`() {
        // White ply3 hangs ~340cp on a fork.
        val s = stats(listOf(ev(1, 30, 30), ev(2, 40, 10), ev(3, -300, -340, motif = "fork")))
        assertEquals(1, s.blunders)
        assertEquals(1, s.forkBlunders)
        assertEquals(0, s.hangingBlunders)
    }

    @Test
    fun `recovery counts a best move right after an inaccuracy`() {
        // White plies 1,3,5: ply3 is an inaccuracy (~40cp), ply5 is best (no loss).
        val s = stats(
            listOf(
                ev(1, 10, 10), ev(2, 10, 0),
                ev(3, -30, -40), ev(4, -30, 0),
                ev(5, -30, 0),
            )
        )
        assertEquals(1, s.oversightCount)
        assertEquals(1, s.oversightRecovered)
    }

    @Test
    fun `sharp positions are bucketed by tactical motif`() {
        // White ply1 best move in a fork (sharp); ply3 best move, quiet, small swing.
        val s = stats(listOf(ev(1, 20, 20, motif = "fork"), ev(2, 20, 0), ev(3, 30, 10)))
        assertEquals(1, s.sharpMoveCount)
        assertEquals(1, s.sharpBestMoves)
        assertEquals(1, s.quietMoveCount)
        assertEquals(1, s.quietBestMoves)
    }

    @Test
    fun `move time splits by winning vs losing eval`() {
        // White ply3: eval before = +250 (winning), spent 5s. White ply5: eval before = -250 (losing), spent 40s.
        val evals = listOf(
            ev(1, 250, 250), ev(2, 250, 0),
            ev(3, 260, 10), ev(4, -250, -510),
            ev(5, -240, 10),
        )
        val times = listOf(
            MoveTimeData(gameId = 1L, moveIndex = 3, timeSpentSeconds = 5, clockRemainingSeconds = 0),
            MoveTimeData(gameId = 1L, moveIndex = 5, timeSpentSeconds = 40, clockRemainingSeconds = 0),
        )
        val s = stats(evals, times)
        assertEquals(5f, s.avgTimeWinningSec, 0.01f)
        assertEquals(40f, s.avgTimeLosingSec, 0.01f)
    }
}
