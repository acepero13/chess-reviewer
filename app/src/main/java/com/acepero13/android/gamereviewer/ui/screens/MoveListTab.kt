package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark

private val COL_NUM      = 36.dp
private val COL_MOVE     = 64.dp
private val COL_EVAL     = 56.dp
private val COL_CPL      = 48.dp
private val COL_ASSESS   = 88.dp
private val COL_BEST     = 64.dp
private val COL_TOP3     = 56.dp
private val COL_DEPTH    = 52.dp

private val HEADER_BG    = Color(0xFF1A1A2E)
private val ROW_ODD_BG   = Color(0xFF16213E)
private val ROW_EVEN_BG  = WCDark
private val BLUNDER_BG   = Color(0x33EF5350)
private val MISTAKE_BG   = Color(0x22FF9800)
private val SELECTED_BG  = Color(0x33448AFF)

private val ASSESS_BLUNDER    = Color(0xFFEF5350)
private val ASSESS_MISTAKE    = Color(0xFFFF9800)
private val ASSESS_INACCURACY = Color(0xFFFFEB3B)
private val ASSESS_GOOD       = Color(0xFF4CAF50)
private val ASSESS_BEST       = Color(0xFF81C784)
private val ASSESS_BOOK       = Color(0xFF90CAF9)
private val ASSESS_DEFAULT    = Color(0xFFB0BEC5)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoveListTab(
    entries: List<MoveListEntry>,
    modifier: Modifier = Modifier,
) {
    val hScroll = rememberScrollState()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        // Sticky header
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HEADER_BG)
                    .horizontalScroll(hScroll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell("#",          COL_NUM)
                HeaderCell("White",      COL_MOVE)
                HeaderCell("Black",      COL_MOVE)
                HeaderCell("Eval W",     COL_EVAL)
                HeaderCell("Eval B",     COL_EVAL)
                HeaderCell("CPL W",      COL_CPL)
                HeaderCell("CPL B",      COL_CPL)
                HeaderCell("Assess W",   COL_ASSESS)
                HeaderCell("Assess B",   COL_ASSESS)
                HeaderCell("Best W",     COL_BEST)
                HeaderCell("Best B",     COL_BEST)
                HeaderCell("Top 3 W",    COL_TOP3)
                HeaderCell("Top 3 B",    COL_TOP3)
                HeaderCell("Depth W",    COL_DEPTH)
                HeaderCell("Depth B",    COL_DEPTH)
            }
        }

        itemsIndexed(entries) { index, entry ->
            val worstCpl = maxOf(entry.cplWhite, entry.cplBlack ?: 0)
            val rowBg = when {
                worstCpl > 100 -> BLUNDER_BG
                worstCpl > 50  -> MISTAKE_BG
                else           -> if (index % 2 == 0) ROW_EVEN_BG else ROW_ODD_BG
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .horizontalScroll(hScroll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DataCell("${entry.moveNumber}", COL_NUM, color = ChessGold, bold = true)
                DataCell(entry.whiteSan,         COL_MOVE)
                DataCell(entry.blackSan ?: "",   COL_MOVE)
                DataCell(formatEval(entry.evalWhiteCp),  COL_EVAL)
                DataCell(entry.evalBlackCp?.let { formatEval(it) } ?: "", COL_EVAL)
                DataCell(if (entry.assessWhite == "Book Move") "" else "${entry.cplWhite}", COL_CPL)
                DataCell(entry.cplBlack?.let { if (entry.assessBlack == "Book Move") "" else "$it" } ?: "", COL_CPL)
                AssessCell(entry.assessWhite,          COL_ASSESS)
                AssessCell(entry.assessBlack ?: "",    COL_ASSESS)
                DataCell(entry.bestWhite ?: "",  COL_BEST, color = Color(0xFFFFEB3B))
                DataCell(entry.bestBlack ?: "",  COL_BEST, color = Color(0xFFFFEB3B))
                DataCell(if (entry.whiteIsTop3) "✓" else "", COL_TOP3, color = ASSESS_GOOD)
                DataCell(if (entry.blackIsTop3) "✓" else "", COL_TOP3, color = ASSESS_GOOD)
                DataCell("${entry.whiteDepth}", COL_DEPTH, color = Color(0xFF78909C))
                DataCell("${entry.blackDepth}", COL_DEPTH, color = Color(0xFF78909C))
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier          = Modifier.width(width).padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment  = Alignment.Center,
    ) {
        Text(
            text       = text,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = ChessGold,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Box(
        modifier         = Modifier.width(width).padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color      = color,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssessCell(text: String, width: Dp) {
    val color = when (text) {
        "Blunder"    -> ASSESS_BLUNDER
        "Mistake"    -> ASSESS_MISTAKE
        "Inaccuracy" -> ASSESS_INACCURACY
        "Good Move"  -> ASSESS_GOOD
        "Excellent"  -> ASSESS_BEST
        "Best Move"  -> ASSESS_BEST
        "Book Move"  -> ASSESS_BOOK
        else         -> ASSESS_DEFAULT
    }
    DataCell(text, width, color = color)
}

private fun formatEval(cp: Int): String {
    val pawns = cp / 100.0
    return if (pawns >= 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
}
