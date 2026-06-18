package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProfileBuilderTest {

    private fun stats(
        gameId: Long,
        attack: Float = 70f,
        defense: Float = 70f,
        opening: Float = 80f,
        endgame: Float = 75f,
        mg: Float = 70f,
    ) = GameStats(
        gameId = gameId,
        playerIsWhite = true,
        accuracy = 80f,
        acpl = 20,
        blunders = 0,
        mistakes = 1,
        inaccuracies = 2,
        openingAccuracy = opening,
        middlegameAccuracy = mg,
        endgameAccuracy = endgame,
        middlegameAttackAccuracy = attack,
        middlegameDefenseAccuracy = defense,
        conversionAccuracy = 75f,
        accuracyStdDev = 10f,
        rushedBlunderRate = 0.05f,
        analysisDepth = 12,
    )

    @Test
    fun `null profile when no games`() {
        assertNull(PlayerProfileBuilder.build(emptyList(), emptyList()))
    }

    @Test
    fun `builds nine axes`() {
        val profile = PlayerProfileBuilder.build(listOf(stats(1)), emptyList())
        assertNotNull(profile)
        assertEquals(9, profile!!.axes.size)
        assertEquals(1, profile.gamesAnalyzed)
    }

    @Test
    fun `attacker archetype when attack dominates defense`() {
        val games = (1..3L).map { stats(it, attack = 85f, defense = 55f) }
        val profile = PlayerProfileBuilder.build(games, emptyList())!!
        assertEquals("Aggressive Attacker", profile.archetype)
    }

    @Test
    fun `deltas are zero with fewer than two windows`() {
        val profile = PlayerProfileBuilder.build(listOf(stats(1), stats(2)), emptyList())!!
        assertTrue(profile.axes.all { it.delta == 0f })
    }
}
