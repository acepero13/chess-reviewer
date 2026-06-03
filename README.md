# GameReviewer

An Android chess game review app that helps you improve by guiding you through structured
self-analysis **before** showing you what the engine thinks. The core philosophy is
**Human-First, Engine-Second** — you think first, annotate your reasoning, and only then
receive engine feedback.

## Philosophy

Most chess review tools show you the evaluation bar immediately, which short-circuits genuine
reflection. GameReviewer works differently:

1. Import a game — the engine runs silently in the background, building a hidden "Truth Map"
2. You navigate the game freely, marking critical moments and annotating your thinking
3. A coach-like **Insight Reconciliation System** notices when you scroll past a major error
   without reflecting on it, and prompts you with targeted questions — never raw engine lines
4. Only after you've done the work does the engine reveal its findings

## Features

- **PGN import** from Chess.com and Lichess (username or URL)
- **Interactive analysis board** with arrow drawing, square marking, and variation sandboxes
- **Blunder Guard** — intercepts candidate moves that cross a centipawn loss threshold and asks for reflection before proceeding
- **Missed Moment engine** — flags positions where the evaluation swings > 1.5 pawns if you scroll past without annotating
- **Guided discovery panel** — asks conceptual questions matched to the mistake category (`MISSED_TACTIC`, `KING_SAFETY`, `ENDGAME_PRINCIPLE`, …) without spoiling the answer
- **Game Report** — move list with narrative coaching commentary, accuracy breakdown, and opening classification
- **Eval Calibration quiz** — test your positional judgment before the eval bar is revealed
- **Cognitive dashboard** — aggregates annotations across sessions to surface your top psychological failure patterns (e.g. Wishful Thinking, Defensive Tunnel Vision)

## Download

Pre-built APKs are attached to every [GitHub Release](../../releases). Download the latest
`app-release.apk` and install it with:

```
adb install app-release.apk
```

Or enable **Install from unknown sources** on your device and open the file directly.

## Building from source

### Prerequisites

- Android Studio Meerkat or later
- JDK 17
- Android SDK with API 35

### 1. Build and publish chess-core locally

The app depends on a local library module. Run this once (and again after any chess-core change):

```bash
cd chess-core
./gradlew :chess-core:publishToMavenLocal
cd ..
```

### 2. Build the app

```bash
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### 3. Build a signed release APK

Set the following environment variables, then run `assembleRelease`:

```bash
export SIGNING_STORE_FILE=/path/to/release.jks
export SIGNING_STORE_PASSWORD=...
export SIGNING_KEY_ALIAS=...
export SIGNING_KEY_PASSWORD=...

./gradlew assembleRelease
```

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Chess engine | Stockfish (via `chess-core`) |
| Board & PGN | `chess-core` (local AAR) + `bhlangonijr/chesslib` |
| Persistence | Room |
| DI | Koin |
| Async | Kotlin Coroutines |

## CI / Releases

Pushing a tag of the form `v*` triggers the [release workflow](.github/workflows/release.yml),
which builds a signed APK and publishes it as a GitHub Release automatically.

```bash
git tag v2.5.12
git push origin v2.5.12
```

See the workflow file for the required GitHub secrets (`RELEASE_KEYSTORE_BASE64`,
`RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
