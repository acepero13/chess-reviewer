package com.acepero13.android.gamereviewer.data.model

data class PlayerStats(
    val name: String,
    val rating: String,
    val isWhite: Boolean,
    val totalMoves: Int,
    val accuracy: Float,
    val avgClockSeconds: Float?,
    val excellentMovePercent: Float,
    val goodMovePercent: Float,
    val blunderRate: Float,
)
