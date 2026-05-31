package com.acepero13.android.gamereviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.navigation.AppNavHost
import com.acepero13.chess.core.ui.theme.ChessTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val boardTheme by settings.boardTheme.collectAsState("Classic")
            val pieceStyle by settings.pieceStyle.collectAsState("Classic")
            val themeMode  by settings.themeMode.collectAsState("dark")

            ChessTheme(
                themeMode  = themeMode,
                boardTheme = boardTheme,
                pieceStyle = pieceStyle,
            ) {
                AppNavHost()
            }
        }
    }
}
