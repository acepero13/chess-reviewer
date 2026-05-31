package com.acepero13.android.gamereviewer.domain

import com.acepero13.chess.core.middlegame.MiddlegamePlan

data class MiddlegamePlanClassification(
    val moveIndex: Int,
    val plans: List<MiddlegamePlan>,
    val fen: String,
)
