package com.acepero13.android.gamereviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.acepero13.android.gamereviewer.navigation.AppNavHost
import com.acepero13.chess.core.ui.theme.ChessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessTheme {
                AppNavHost()
            }
        }
    }
}
