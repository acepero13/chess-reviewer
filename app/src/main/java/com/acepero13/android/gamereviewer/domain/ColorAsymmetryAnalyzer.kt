package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame

/**
 * Splits engine-marked critical moments by the color the user played and
 * runs the failure-archetype diagnosis on each side independently.
 *
 * Reveals systematic differences: e.g. the user may handle king safety well
 * as White but neglect it as Black, or vice versa.
 */
data class ColorAsymmetry(
    val asWhite:      List<BehavioralDiagnostic.FailureTrend>,
    val asBlack:      List<BehavioralDiagnostic.FailureTrend>,
    val totalAsWhite: Int,
    val totalAsBlack: Int,
    val hasData:      Boolean,
)

object ColorAsymmetryAnalyzer {

    /**
     * @param username The player's own handle from [SettingsRepository.username].
     *                 An empty username disables the computation.
     */
    fun compute(
        moments:  List<CriticalMoment>,
        games:    List<ReviewGame>,
        username: String,
    ): ColorAsymmetry {
        if (username.isBlank()) {
            return ColorAsymmetry(emptyList(), emptyList(), 0, 0, false)
        }

        val engineMoments = moments.filter {
            it.type == CriticalMoment.Type.ENGINE_MARKED.name
        }

        val userColorByGame: Map<Long, String> = games.associate { game ->
            val color = when {
                game.whitePlayer.equals(username, ignoreCase = true) -> "white"
                game.blackPlayer.equals(username, ignoreCase = true) -> "black"
                else                                                 -> "unknown"
            }
            game.id to color
        }

        // White's mistakes: user played White AND made the move on an odd index.
        val whiteMoments = engineMoments.filter { m ->
            userColorByGame[m.gameId] == "white" && m.moveIndex % 2 == 1
        }
        // Black's mistakes: user played Black AND made the move on an even index.
        val blackMoments = engineMoments.filter { m ->
            userColorByGame[m.gameId] == "black" && m.moveIndex % 2 == 0
        }

        return ColorAsymmetry(
            asWhite      = BehavioralDiagnostic.diagnose(whiteMoments, topN = 2),
            asBlack      = BehavioralDiagnostic.diagnose(blackMoments, topN = 2),
            totalAsWhite = whiteMoments.size,
            totalAsBlack = blackMoments.size,
            hasData      = whiteMoments.isNotEmpty() || blackMoments.isNotEmpty(),
        )
    }
}
