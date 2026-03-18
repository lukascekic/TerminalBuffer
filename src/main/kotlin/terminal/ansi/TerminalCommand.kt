package terminal.ansi

import terminal.buffer.Color
import terminal.buffer.Style

sealed class TerminalCommand {
    data class Print(val char: Char) : TerminalCommand()
    data class SetCursorPosition(val row: Int, val column: Int) : TerminalCommand()
    data class CursorUp(val n: Int) : TerminalCommand()
    data class CursorDown(val n: Int) : TerminalCommand()
    data class CursorForward(val n: Int) : TerminalCommand()
    data class CursorBackward(val n: Int) : TerminalCommand()
    data class EraseDisplay(val mode: Int) : TerminalCommand()
    data class EraseLine(val mode: Int) : TerminalCommand()
    data class SetForeground(val color: Color) : TerminalCommand()
    data class SetBackground(val color: Color) : TerminalCommand()
    data class SetStyleOn(val style: Style) : TerminalCommand()
    data class SetStyleOff(val style: Style) : TerminalCommand()
    object ResetAttributes : TerminalCommand()
    object CarriageReturn : TerminalCommand()
    object LineFeed : TerminalCommand()
    object Backspace : TerminalCommand()
    data class InsertCharacters(val n: Int) : TerminalCommand()
    data class DeleteCharacters(val n: Int) : TerminalCommand()
    data class InsertLines(val n: Int) : TerminalCommand()
    data class DeleteLines(val n: Int) : TerminalCommand()
    data class SetScrollRegion(val top: Int, val bottom: Int) : TerminalCommand()
}
