package terminal.ansi

import terminal.buffer.Color
import terminal.buffer.Style

/**
 * VT100/ANSI escape sequence parser implemented as a character-by-character state machine.
 *
 * Input bytes are fed via [feed]. For each recognised sequence or printable character, the
 * [dispatch] callback receives the corresponding [TerminalCommand]. Unrecognised sequences are
 * silently discarded, preserving robustness against unknown escape codes.
 *
 * Supported sequences:
 * - Printable ASCII characters → [TerminalCommand.Print]
 * - C0 controls: CR, LF, BS, TAB
 * - CSI sequences: cursor movement (A–D, H/f), erase (J/K), SGR (m), scroll region (r),
 *   insert/delete characters (@/P) and lines (L/M)
 * - OSC strings (terminated by BEL or ESC \): collected and discarded
 * - DEC private mode sequences (`ESC [ ? …`): silently ignored
 *
 * The parser uses seven internal states defined in [ParserState].
 */
class VT100Parser(private val dispatch: (TerminalCommand) -> Unit) {

    private companion object {
        private const val ESC = '\u001B'
        private const val BEL = '\u0007'
        private const val BS = '\u0008'
        private const val TAB = '\t'
        private const val LF = '\n'
        private const val CR = '\r'
    }

    private var state: ParserState = ParserState.GROUND
    private val params: MutableList<Int> = mutableListOf()
    private var currentParam: Int = -1
    private var privateMode: Boolean = false

    /**
     * Process [input] one character at a time, dispatching [TerminalCommand] instances for each
     * complete sequence or printable character encountered.
     *
     * Partial sequences are buffered internally and completed when subsequent calls provide the
     * remaining bytes. The parser state persists across calls, so streaming input is supported.
     */
    fun feed(input: String) {
        for (ch in input) {
            processByte(ch)
        }
    }

    private fun processByte(ch: Char) {
        when (state) {
            ParserState.GROUND -> handleGround(ch)
            ParserState.ESCAPE -> handleEscape(ch)
            ParserState.ESCAPE_INTERMEDIATE -> handleEscapeIntermediate(ch)
            ParserState.CSI_ENTRY -> handleCsiEntry(ch)
            ParserState.CSI_PARAM -> handleCsiParam(ch)
            ParserState.CSI_INTERMEDIATE -> handleCsiIntermediate(ch)
            ParserState.OSC_STRING -> handleOscString(ch)
        }
    }

    private fun handleGround(ch: Char) {
        when {
            ch == ESC -> {
                state = ParserState.ESCAPE
            }
            ch == CR -> dispatch(TerminalCommand.CarriageReturn)
            ch == LF -> dispatch(TerminalCommand.LineFeed)
            ch == BS -> dispatch(TerminalCommand.Backspace)
            ch == TAB -> {
                // Tab — move to next tab stop (every 8 columns)
                // For now, emit spaces
                dispatch(TerminalCommand.Print(' '))
            }
            ch.code >= 0x20 -> dispatch(TerminalCommand.Print(ch))
        }
    }

    private fun handleEscape(ch: Char) {
        when {
            ch == '[' -> {
                state = ParserState.CSI_ENTRY
                params.clear()
                currentParam = -1
                privateMode = false
            }
            ch == ']' -> {
                state = ParserState.OSC_STRING
            }
            ch in ' '..'/' -> {
                state = ParserState.ESCAPE_INTERMEDIATE
            }
            ch in '0'..'~' -> {
                // ESC + final byte — ignore for now
                state = ParserState.GROUND
            }
            else -> state = ParserState.GROUND
        }
    }

    private fun handleEscapeIntermediate(ch: Char) {
        when {
            ch in '0'..'~' -> state = ParserState.GROUND
            ch in ' '..'/' -> { /* collect intermediate */ }
            else -> state = ParserState.GROUND
        }
    }

    private fun handleCsiEntry(ch: Char) {
        when {
            ch in '0'..'9' -> {
                currentParam = ch - '0'
                state = ParserState.CSI_PARAM
            }
            ch == ';' -> {
                params.add(0)
                state = ParserState.CSI_PARAM
            }
            ch in '<'..'?' -> {
                privateMode = true
                state = ParserState.CSI_PARAM
            }
            ch in ' '..'/' -> {
                state = ParserState.CSI_INTERMEDIATE
            }
            ch in '@'..'~' -> {
                dispatchCsi(ch)
                state = ParserState.GROUND
            }
            else -> state = ParserState.GROUND
        }
    }

    private fun handleCsiParam(ch: Char) {
        when {
            ch in '0'..'9' -> {
                currentParam = if (currentParam < 0) (ch - '0') else (currentParam * 10 + (ch - '0')).coerceAtMost(9999)
            }
            ch == ';' -> {
                params.add(if (currentParam < 0) 0 else currentParam)
                currentParam = -1
            }
            ch in ' '..'/' -> {
                params.add(if (currentParam < 0) 0 else currentParam)
                currentParam = -1
                state = ParserState.CSI_INTERMEDIATE
            }
            ch in '@'..'~' -> {
                params.add(if (currentParam < 0) 0 else currentParam)
                currentParam = -1
                dispatchCsi(ch)
                state = ParserState.GROUND
            }
            else -> state = ParserState.GROUND
        }
    }

    private fun handleCsiIntermediate(ch: Char) {
        when {
            ch in ' '..'/' -> { /* collect */ }
            ch in '@'..'~' -> {
                // Intermediate sequences — ignore for now
                state = ParserState.GROUND
            }
            else -> state = ParserState.GROUND
        }
    }

    private fun handleOscString(ch: Char) {
        when {
            ch == BEL -> state = ParserState.GROUND
            ch == ESC -> state = ParserState.ESCAPE  // ESC \ terminates OSC (ST)
        }
        // Otherwise collect OSC string (ignored for now)
    }

    private fun dispatchCsi(finalByte: Char) {
        if (privateMode) {
            privateMode = false
            return
        }
        val p = params.toList()
        when (finalByte) {
            'A' -> dispatch(TerminalCommand.CursorUp(p.getOrDefault(0, 1)))
            'B' -> dispatch(TerminalCommand.CursorDown(p.getOrDefault(0, 1)))
            'C' -> dispatch(TerminalCommand.CursorForward(p.getOrDefault(0, 1)))
            'D' -> dispatch(TerminalCommand.CursorBackward(p.getOrDefault(0, 1)))
            'H', 'f' -> {
                val row = (p.getOrDefault(0, 1) - 1).coerceAtLeast(0)
                val col = (p.getOrDefault(1, 1) - 1).coerceAtLeast(0)
                dispatch(TerminalCommand.SetCursorPosition(row, col))
            }
            'J' -> dispatch(TerminalCommand.EraseDisplay(p.getOrDefault(0, 0)))
            'K' -> dispatch(TerminalCommand.EraseLine(p.getOrDefault(0, 0)))
            'm' -> dispatchSgr(p)
            'r' -> {
                val top = (p.getOrDefault(0, 1) - 1).coerceAtLeast(0)
                val bottom = (p.getOrDefault(1, 1) - 1).coerceAtLeast(0)
                dispatch(TerminalCommand.SetScrollRegion(top, bottom))
            }
            '@' -> dispatch(TerminalCommand.InsertCharacters(p.getOrDefault(0, 1)))
            'P' -> dispatch(TerminalCommand.DeleteCharacters(p.getOrDefault(0, 1)))
            'L' -> dispatch(TerminalCommand.InsertLines(p.getOrDefault(0, 1)))
            'M' -> dispatch(TerminalCommand.DeleteLines(p.getOrDefault(0, 1)))
        }
    }

    private fun dispatchSgr(params: List<Int>) {
        val p = if (params.isEmpty()) listOf(0) else params
        var i = 0
        while (i < p.size) {
            when (p[i]) {
                0 -> dispatch(TerminalCommand.ResetAttributes)
                1 -> dispatch(TerminalCommand.SetStyleOn(Style.BOLD))
                3 -> dispatch(TerminalCommand.SetStyleOn(Style.ITALIC))
                4 -> dispatch(TerminalCommand.SetStyleOn(Style.UNDERLINE))
                22 -> dispatch(TerminalCommand.SetStyleOff(Style.BOLD))
                23 -> dispatch(TerminalCommand.SetStyleOff(Style.ITALIC))
                24 -> dispatch(TerminalCommand.SetStyleOff(Style.UNDERLINE))
                in 30..37 -> dispatch(TerminalCommand.SetForeground(sgrToColor(p[i] - 30)))
                39 -> dispatch(TerminalCommand.SetForeground(Color.DEFAULT))
                in 40..47 -> dispatch(TerminalCommand.SetBackground(sgrToColor(p[i] - 40)))
                49 -> dispatch(TerminalCommand.SetBackground(Color.DEFAULT))
                in 90..97 -> dispatch(TerminalCommand.SetForeground(sgrToBrightColor(p[i] - 90)))
                in 100..107 -> dispatch(TerminalCommand.SetBackground(sgrToBrightColor(p[i] - 100)))
            }
            i++
        }
    }

    private fun sgrToColor(index: Int): Color = when (index) {
        0 -> Color.BLACK
        1 -> Color.RED
        2 -> Color.GREEN
        3 -> Color.YELLOW
        4 -> Color.BLUE
        5 -> Color.MAGENTA
        6 -> Color.CYAN
        7 -> Color.WHITE
        else -> Color.DEFAULT
    }

    private fun sgrToBrightColor(index: Int): Color = when (index) {
        0 -> Color.BRIGHT_BLACK
        1 -> Color.BRIGHT_RED
        2 -> Color.BRIGHT_GREEN
        3 -> Color.BRIGHT_YELLOW
        4 -> Color.BRIGHT_BLUE
        5 -> Color.BRIGHT_MAGENTA
        6 -> Color.BRIGHT_CYAN
        7 -> Color.BRIGHT_WHITE
        else -> Color.DEFAULT
    }

    // VT100: param value 0 means "use default" for most CSI commands
    private fun List<Int>.getOrDefault(index: Int, default: Int): Int =
        if (index < size && this[index] > 0) this[index] else default
}
