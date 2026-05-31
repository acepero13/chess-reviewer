# CLAUDE.md — Chess Self-Analysis App Specification

## 📋 Project Overview

An Android chess game review tool that helps users improve by guiding them through structured
self-analysis before exposing engine-based evaluation. The core philosophy is **Human-First,
Engine-Second**: users must think first, annotate their thinking, and only then receive engine
feedback. The app supports PGN imports from Chess.com and Lichess using `chess-core` utilities and
simulates a human coach by noticing blind spots in real time via an **Insight Reconciliation System
**.

**IMPORTANT**: Every time you make a change in chess-core, you need to run a publishMaven task,
otherwise, changes are not seen by the game reviwer app

---

## 🛠️ Tech Stack & Library Conventions

* **Core Foundation:** Powered by the local library `com.acepero13.chess:chess-core:1.0.0`.
  Re-exports `bhlangonijr:chesslib` for core mutations (`Board`, `Move`, `Square`, `Piece`, etc.).
* **UI Architecture:** Jetpack Compose wrapped inside the bundled `ChessTheme`. State transitions
  must drive the library's immutable `BoardState` object.
* **Persistence:** Room Database leveraging `PositionAnnotation` and `PositionAnnotationDao` from
  `chess-core` to store comments, variation structures, and psychological metadata.
* **Asynchrony:** Kotlin Coroutines using `Dispatchers.Default` to prevent background
  `StockfishEngine` tasks from stalling the Jetpack Compose drawing cycle.
* **UI Board look and feel**: Use the board thmes and pieces from chess-core

---

## 🚀 Implementation Milestones

### Milestone 1: PGN Ingestion & Silent Background Tracking

Implement a non-judgmental ingestion workflow that maps out game metrics and engine truths
completely out of the user's sight.

* **Task 1.1:** Build an import stream using `ChessComFetcher.fetchPgn` and
  `LichessFetcher.fetchPgn` piped through `PgnImporter` to split and isolate headers from move text
  blocks.
* **Task 1.2:** Implement an automated, silent background worker utilizing
  `StockfishEngine.analyzePosition` (or `analyzeMultiPV` for alternate candidates) running at
  `ChessConstants.DEFAULT_ANALYSIS_DEPTH`. Build a hidden local **"Truth Map"** indexing move
  numbers to evaluation swings and tactical motifs via `MotifClassifier.classify`.
* **Task 1.3:** Design the **First Screen UI** using a lightweight, non-judgmental summary:
  highlight structural metrics like opening development quality using `OpeningClassifier`,
  middlegame complexity indicators, and endgame transitions. **Strict Rule:** Hide the `EvalBar` and
  omit any primary accuracy scores.

### Milestone 2: Free Navigation & Guided Self-Analysis Mode

Construct an interactive self-review interface that captures user markers, handles custom drawings,
and isolates user variation trees.

* **Task 2.1:** Hook up `ChessBoard` in exploration mode (`BoardState.isEditorMode = true`). Capture
  user drawings via `onArrowDrawn` and `onSquareMarked`, writing changes to the database using
  `PositionAnnotationDao.upsert`.
* **Task 2.2:** Build an interactive sandbox system using `SolutionTreeBuilder`. When a player flags
  a position as critical, compile a separate `SandboxTree` branching path. If "Automatic Response"
  is active, look up the current layout inside the local engine instance, introduce a brief pacing
  delay (`ChessConstants.OPPONENT_REPLY_DELAY_MS`), and play the engine's counter-move on the board.
* **Task 2.3:** Implement a structured Material3 `BottomSheet` questionnaire that intercepts users
  when marking a move as critical. Prompt the user for qualitative data (plans, threats, candidate
  options) and store these choices directly in `PositionAnnotation.moveComment` alongside targeted
  theme classifications.

### Milestone 3: Insight Reconciliation & Intervention Engine

Build the signature reconciliation layer that matches user definitions against engine truths to
capture psychological blind spots.

* **Task 3.1:** Implement the **"Blunder Guard"** filter layer. When a user tests an alternative
  candidate move in the manual sandbox, compare its resulting positional evaluation against the
  background truth data. If the centipawn loss drops past the default limits, pause the piece drop,
  flash the board border via `BoardState.showingFlash`, and request cognitive reflection.
* **Task 3.2:** Build the **"Missed Moment" Intervention Engine**. Track the active move index as
  the user scrolls through the `MoveTree`. If the user passes an engine-marked critical position (
  evaluation delta > 1.5 pawns or missed tactical motif) without entering sandbox mode, saving an
  annotation, or applying a tag, display a non-intrusive "Review suggestion available" indicator.
* **Task 3.3:** Build a guided discovery panel. When the user taps the intervention indicator,
  freeze standard navigation, hide the move list, and present targeted conceptual questions matching
  the `Reason Category` (e.g., `MISSED_TACTIC`, `KING_SAFETY`, `ENDGAME_PRINCIPLE`) to guide them to
  the correct move without revealing raw lines.

### Milestone 4: Progressive Engine Reveal & Cognitive Diagnostic Dashboard

Aggregate transactional metadata across all processed reviews to display overarching behavioral
metrics.

* **Task 4.1:** Build a time-allocation engine. Extract timestamp entries (`[%clk ...]`) from the
  imported PGN data. Map the exact time spent per move directly against the changes in the hidden
  evaluation map.
* **Task 4.2:** Design a visualization layout charting **Decision Velocity vs. Tactical Complexity
  **. Highlight precise points where rushing in tactical states or over-calculating in equal
  structures leads directly to downstream execution blunders.
* **Task 4.3:** Aggregate stored `PositionAnnotation` entries and custom tags across historical
  sessions. Generate an aggregated behavioral diagnosis profile that exposes their top 3
  psychological failure trends (e.g., *Wishful Thinking* vs. *Defensive Tunnel Vision*).

## 🔄 Core Review App Workflow

[1. DATA INGESTION]
Fetch PGN text via ChessComFetcher
or LichessFetcher.
│
▼
[2. HIDDEN ANALYSIS RUN]
StockfishEngine populates a hidden
Truth Map with evaluations & motifs.
│
▼
[3. RECALL MODE ENTRY]
User reviews game via MoveTree.
EvalBar remains locked and hidden.
│
▼
[4. CANDIDATE INTERPLAY]
Sandbox mode spawns deep variation paths.
Blunder Guard blocks major mistakes.
│
▼
[5. COACH MODE INTERVENTION]
Navigation flags if user scrolls
past an un-annotated major error.
│
▼
[6. PERFORMANCE SYNTHESIS]
Progressive Engine Reveal phase.
Compile psychological tags to dashboard.

---

## 🗃️ Extended Core Data Model Mapping

All local entities translate directly into or build on top of the `chess-core` entities:

* **`Game`**: Wraps structural elements returned by `PgnImporter.parseGame` (Metadata headers, raw
  move strings, and indexed FEN vectors mapped to custom timeline tracks).
* **`PositionAnnotation` (From `chess-core`)**: Re-used natively to persist `arrowsJson`,
  `markedSquaresJson`, and `moveComment`. The text field holds serialized JSON representing the
  user's responses to guided questionnaires.
* **`CriticalMoment` Entity**:
    * `moveIndex`: Int
    * `type`: Enum (`USER_MARKED` vs. `ENGINE_MARKED`)
    * `severity`: Int (Centipawn delta values)
    * `reasonCategory`: Enum (`MISSED_TACTIC`, `OPENING_DEVIATION`, `HANGING_PIECE`, `KING_SAFETY`,
      `ENDGAME_PRINCIPLE`, `STRATEGIC_MISTAKE`, `TIME_PRESSURE`, `MISSED_WIN`)
    * `explanationState`: Enum (`HIDDEN`, `HINTED`, `REVEALED`)

---

## 🔄 Core Review App Workflow