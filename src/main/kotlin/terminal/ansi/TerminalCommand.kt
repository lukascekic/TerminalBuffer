package terminal.ansi

import terminal.buffer.Color
import terminal.buffer.Style

/**
 * Sealed class representing all terminal commands that [VT100Parser] can produce.
 *
 * Instances are dispatched to a handler (typically [terminal.buffer.TerminalBuffer.applyCommand])
 * which translates each command into a corresponding buffer operation. Commands map directly to
 * VT100/ANSI escape sequence semantics.
 */
sealed class TerminalCommand {
    /** Print a single printable character at the current cursor position. */
    data class Print(val char: Char) : TerminalCommand()

    /**
     * Move the cursor to an absolute position (CSI H / CSI f).
     *
     * Coordinates are 0-based; the parser converts from VT100's 1-based values.
     */
    data class SetCursorPosition(val row: Int, val column: Int) : TerminalCommand()

    /** Move the cursor up by [n] rows (CSI A). */
    data class CursorUp(val n: Int) : TerminalCommand()

    /** Move the cursor down by [n] rows (CSI B). */
    data class CursorDown(val n: Int) : TerminalCommand()

    /** Move the cursor right by [n] columns (CSI C). */
    data class CursorForward(val n: Int) : TerminalCommand()

    /** Move the cursor left by [n] columns (CSI D). */
    data class CursorBackward(val n: Int) : TerminalCommand()

    /**
     * Erase part of the display (CSI J).
     *
     * [mode]: 0 = cursor to end, 1 = start to cursor, 2 = whole screen (cursor preserved),
     * 3 = whole screen + scrollback.
     */
    data class EraseDisplay(val mode: Int) : TerminalCommand()

    /**
     * Erase part of the current line (CSI K).
     *
     * [mode]: 0 = cursor to end of line, 1 = start to cursor, 2 = whole line.
     */
    data class EraseLine(val mode: Int) : TerminalCommand()

    /** Set the foreground color via SGR 30–37, 39, or 90–97. */
    data class SetForeground(val color: Color) : TerminalCommand()

    /** Set the background color via SGR 40–47, 49, or 100–107. */
    data class SetBackground(val color: Color) : TerminalCommand()

    /** Enable a text style via SGR (1=bold, 3=italic, 4=underline). */
    data class SetStyleOn(val style: Style) : TerminalCommand()

    /** Disable a text style via SGR (22=bold off, 23=italic off, 24=underline off). */
    data class SetStyleOff(val style: Style) : TerminalCommand()

    /** Reset all SGR attributes to defaults (SGR 0). */
    object ResetAttributes : TerminalCommand()

    /** Move the cursor to column 0 (ASCII CR, `\r`). */
    object CarriageReturn : TerminalCommand()

    /** Advance the cursor one row, scrolling if at the bottom of the scroll region (ASCII LF, `\n`). */
    object LineFeed : TerminalCommand()

    /** Move the cursor one column to the left (ASCII BS, `\u0008`). */
    object Backspace : TerminalCommand()

    /** Insert [n] blank characters at the cursor position, shifting existing content right (CSI @). */
    data class InsertCharacters(val n: Int) : TerminalCommand()

    /** Delete [n] characters at the cursor position, shifting remaining content left (CSI P). */
    data class DeleteCharacters(val n: Int) : TerminalCommand()

    /** Insert [n] blank lines at the cursor row, pushing lines down within the scroll region (CSI L). */
    data class InsertLines(val n: Int) : TerminalCommand()

    /** Delete [n] lines starting at the cursor row, pulling lines up within the scroll region (CSI M). */
    data class DeleteLines(val n: Int) : TerminalCommand()

    /**
     * Set the scroll region (DECSTBM, CSI r).
     *
     * [top] and [bottom] are 0-based row indices. When [bottom] is 0 or less than [top],
     * [terminal.buffer.TerminalBuffer.applyCommand] resets the region to the full screen.
     */
    data class SetScrollRegion(val top: Int, val bottom: Int) : TerminalCommand()
}
