package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object AnnotationParser {

    fun parseArrows(gson: Gson, json: String): List<Arrow> = runCatching {
        gson.fromJson<List<Arrow>>(json, object : TypeToken<List<Arrow>>() {}.type)
    }.getOrDefault(emptyList())

    fun parseMarks(gson: Gson, json: String): List<MarkedSquare> = runCatching {
        gson.fromJson<List<MarkedSquare>>(json, object : TypeToken<List<MarkedSquare>>() {}.type)
    }.getOrDefault(emptyList())
}
