# Add project specific ProGuard rules here.
# For more details see: http://developer.android.com/tools/help/proguard.html

# chess-core: keep Room entities and chesslib enums
-keep class com.acepero13.chess.core.data.model.** { *; }
-keep class com.github.bhlangonijr.chesslib.** { *; }
-keepclassmembers enum com.github.bhlangonijr.chesslib.** { *; }

# Gson: keep data classes used for serialization
-keep class com.acepero13.chess.core.ui.board.Arrow { *; }
-keep class com.acepero13.chess.core.ui.board.MarkedSquare { *; }
-keep class com.acepero13.android.gamereviewer.data.model.** { *; }
