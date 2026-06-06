package com.acepero13.android.gamereviewer.domain

internal object CoachingThresholds {
    const val CANDIDATE_EVAL_THRESHOLD_CP              = 30
    const val WORST_PIECE_MAX_MOBILITY                 = 2
    const val WORST_PIECE_STREAK_NEEDED                = 3
    const val MIDDLEGAME_START                         = 10
    const val MIDDLEGAME_END                           = 50
    const val OPPONENT_PLAN_MIN_CP                     = 50
    const val OPPONENT_PLAN_MAX_CP                     = 120
    const val KING_MIN_ADJACENT_DEFENDERS              = 1
    const val SAFETY_MIN_CP_DROP                       = 100
    const val OPENING_TRIGGER_THRESHOLD_CP             = -75
    const val IMPULSE_TIME_THRESHOLD_SECONDS           = 10
    const val IMPULSE_CP_LOSS_THRESHOLD                = 200
    const val CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS = 30
    const val TACTICAL_OVERSIGHT_MIN_SECONDS           = IMPULSE_TIME_THRESHOLD_SECONDS
    const val TACTICAL_OVERSIGHT_MAX_SECONDS           = CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS
    const val CANDIDATE_SEARCH_MIN_CP                  = 50
    const val CANDIDATE_SEARCH_MAX_CP                  = 300
    const val CCT_CHECK_EVAL_SHIFT_CP                  = 100
    const val TIER4_OPENING_GATE                       = 30
    const val ROOK_ACTIVATION_MIN_HALF_MOVE            = 30
    const val ROOK_ACTIVATION_MIN_DEVELOPED_MINORS     = 2
    const val CANDIDATE_SEARCH_CLARITY_CP              = 150
    const val FORCING_MOVE_MIN_CP_LOSS                 = 150
    const val FORCING_MOVE_MIN_PLAYER_ADVANTAGE_CP     = 150
    const val PRE_MOVE_CHECKLIST_MIN_CP_LOSS           = 100
    const val PRE_MOVE_CHECKLIST_MAX_CP_LOSS           = 200
    const val CONVERSION_ADVANTAGE_THRESHOLD_CP        = 500
    const val KING_ATTACK_FIRE_THRESHOLD               = 3
    const val KING_ATTACK_LOSS_THRESHOLD               = 1
    const val HARMONY_FIRE_THRESHOLD                   = 6
    const val HARMONY_LOSS_THRESHOLD                   = 3
    const val HARMONY_MIN_DELTA                        = 2
    const val PUNISH_BLUNDER_MIN_CP_LOSS               = 120
    const val COORDINATION_EVAL_MIN_ADVANTAGE_CP       = 50
    const val COORDINATION_BLUNDER_SUPPRESS_CP         = 150
    const val CALIBRATION_POST_OPENING_MIN             = 28
    const val CALIBRATION_POST_OPENING_MAX             = 44
    const val CALIBRATION_EVAL_JUMP_FROM_CP            = 50
    const val CALIBRATION_EVAL_JUMP_TO_CP              = 150
    const val CALIBRATION_FREQUENCY_CAP               = 15
    const val CALIBRATION_VOLATILITY_MAX_CP            = 50
    const val CALIBRATION_MAX_EVAL_CP                  = 300
}
