# Terminal Buffer

A terminal text buffer implementation in Kotlin — the core data structure that terminal emulators
use to store and manipulate displayed text. Includes a VT100/ANSI escape sequence parser and
end-to-end integration tests.

## Overview

This project implements the fundamental components of a terminal emulator's text layer:

- **Cell / Line / TerminalBuffer** — the screen grid with scrollback history
- **WideCharUtil** — East Asian wide character (CJK) detection and two-column rendering
- **VT100Parser** — a character-by-character state machine for ANSI/VT100 escape sequences
- **TerminalBuffer.applyCommand** — bridge that maps parsed commands to buffer operations

## Architecture

```
terminal/
  buffer/
    Cell.kt               — Immutable data class: char + TextAttributes + display width
    TextAttributes.kt     — Immutable data class: foreground Color, background Color, Set<Style>
    Color.kt              — Enum: DEFAULT + 16 standard ANSI colors (normal + bright)
    Style.kt              — Enum: BOLD, ITALIC, UNDERLINE
    CursorPosition.kt     — Immutable data class: (column, row)
    WideCharUtil.kt       — Detects wide characters (CJK, Hangul, Hiragana, Katakana, fullwidth forms)
    Line.kt               — Fixed-width array of Cell with getText, clear, copyOf, resizedTo
    TerminalBuffer.kt     — Main API: screen grid, scrollback, cursor, editing, resize, scroll regions
  ansi/
    ParserState.kt        — Seven-state enum for the VT100 state machine
    TerminalCommand.kt    — Sealed class with 21 command subtypes
    VT100Parser.kt        — State machine: feed(String) → dispatch(TerminalCommand)
```

### TerminalBuffer

`TerminalBuffer(width, height, maxScrollbackSize)` is the central mutable class. Key properties:

| Property | Description |
|---|---|
| `width`, `height` | Screen dimensions (read-only; use `resize()` to change) |
| `maxScrollbackSize` | Max lines in scrollback history (default 1000) |
| `currentAttributes` | `TextAttributes` applied to newly written characters |
| `scrollbackSize` | Number of lines currently in scrollback |

**Screen model:**
- `screen`: fixed-size `MutableList<Line>` of `height` rows
- `scrollback`: lines pushed off the top of a full-screen scroll; oldest lines dropped when `maxScrollbackSize` exceeded

**Scroll regions (DECSTBM):**
- `setScrollRegion(top, bottom)` constrains scrolling to rows `[top..bottom]`
- Full-screen scrolls push lines to scrollback; sub-region scrolls do not
- `resize()` always resets the scroll region to the full screen

### Cell width model

| `width` value | Meaning |
|---|---|
| `1` | Normal character (one column) |
| `2` | Wide character main cell (CJK etc.) — occupies two columns |
| `0` | Continuation cell — right half of a wide character |

When overwriting either half of a wide character, both halves are cleared before writing.

### VT100Parser

A seven-state machine (`GROUND → ESCAPE → CSI_ENTRY → CSI_PARAM → ...`) that processes input
one character at a time via `feed(String)`. Recognised sequences produce `TerminalCommand`
instances dispatched to a callback.

Supported sequences:

| Sequence | Command |
|---|---|
| Printable ASCII | `Print` |
| `\r`, `\n`, `\u0008` | `CarriageReturn`, `LineFeed`, `Backspace` |
| `ESC [ A/B/C/D` | `CursorUp/Down/Forward/Backward` |
| `ESC [ H`, `ESC [ f` | `SetCursorPosition` |
| `ESC [ J` | `EraseDisplay` (modes 0–3) |
| `ESC [ K` | `EraseLine` (modes 0–2) |
| `ESC [ m` | SGR — colors, bold, italic, underline, reset |
| `ESC [ r` | `SetScrollRegion` (DECSTBM) |
| `ESC [ @ / P / L / M` | Insert/Delete characters and lines |
| `ESC [ ? …` | DEC private mode — silently ignored |
| `ESC ] … BEL` | OSC string — silently ignored |

Parameter value `0` is treated as "use default" for all CSI commands (standard VT100 behavior).

## Design Decisions

### Cell as Immutable Data Class

`Cell` and `TextAttributes` are immutable `data class` objects rather than packed integers or
mutable classes. Writing a character creates a new `Cell` instead of mutating an existing one.
This trades some memory overhead for readability and
correctness — for a typical 80x24 screen the difference is negligible, and immutability eliminates
an entire class of aliasing bugs. If optimizing for production scale, the packed representation
could be adopted without changing the public API.

`Set<Style>` is used for `TextAttributes.styles` instead of `EnumSet` because `EnumSet` is
mutable — `data class copy()` would share the same underlying set, causing silent aliasing bugs
when modifying styles on a copied instance.

### Color as Enum

`Color` is an enum with 17 values (DEFAULT + 16 standard ANSI colors) rather than a sealed class
hierarchy. The task specifies 16 colors, so an enum gives exhaustive `when` matching and
compile-time safety with no complexity overhead. Extending to 256-color or truecolor would call
for a sealed class (`Standard(index)`, `Extended(index)`, `TrueColor(r, g, b)`), but that's not
needed here.

### Wide Character Handling

`WideCharUtil` classifies characters using both Unicode block membership and explicit code point
ranges. Problematic blocks that contain a mixture of wide and halfwidth characters
(`HALFWIDTH_AND_FULLWIDTH_FORMS`, `HANGUL_JAMO`) are excluded from the block set; the correct
narrow sub-ranges are covered by explicit `WIDE_RANGES` entries instead.

### Resize Strategy

`resize(newWidth, newHeight)`:
- Width — each `Line` is rebuilt via `Line.resizedTo()`, which pads with spaces or truncates,
  and clears wide characters that would be split at the new boundary.
- Height increase — pulls the most recent scrollback lines back onto the screen before adding
  new blank lines.
- Height decrease — pushes top screen lines to scrollback (subject to `maxScrollbackSize`).

### Parser Robustness

- CSI parameter values are capped at 9999 to prevent integer overflow from malformed input.
- DEC private mode sequences (`ESC [ ? …`) set an internal `privateMode` flag; the dispatch is
  silently skipped so they produce no garbage commands.
- `ESC [ r` with no parameters (which the parser converts to `SetScrollRegion(0, 0)`) is handled
  in `applyCommand` as a scroll region reset.

## Building & Testing

Requires JDK 11+ and the Gradle wrapper included in the repository.

```bash
# Compile + run all tests
./gradlew build

# Run tests only
./gradlew test

# Run a specific test class
./gradlew test --tests "terminal.buffer.TerminalBufferWideCharTest"
```

### Test structure

```
test/kotlin/terminal/
  buffer/
    CellTest.kt                        — Cell, TextAttributes, CursorPosition
    LineTest.kt                        — Line operations and resizedTo edge cases
    TerminalBufferTest.kt              — Construction and content access
    TerminalBufferCursorTest.kt        — Cursor movement and clamping
    TerminalBufferEditingTest.kt       — writeText, insertText, fillLine, clearScreen
    TerminalBufferScrollbackTest.kt    — Scrollback limits and access
    TerminalBufferWideCharTest.kt      — Wide character writing and overwriting
    TerminalBufferResizeTest.kt        — Width/height resize with scrollback interaction
    TerminalBufferScrollRegionTest.kt  — DECSTBM scroll region behavior
    TerminalBufferEraseTest.kt         — EraseDisplay and EraseLine modes
  ansi/
    VT100ParserTest.kt                 — Parser state machine unit tests
    IntegrationTest.kt                 — End-to-end: ANSI sequences → buffer state
```

## Tech Stack

- **Language**: Kotlin
- **Build**: Gradle 8 with Kotlin DSL
- **Testing**: JUnit 5 + kotest-assertions
- No runtime dependencies beyond the Kotlin standard library
