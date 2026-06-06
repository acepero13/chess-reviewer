package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fen: String,
    val sourceGameId: Long? = null,
    val moveIndex: Int = 0,
    val tags: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

fun Snippet.parsedTags(): List<String> =
    tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
