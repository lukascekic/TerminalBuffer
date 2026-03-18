package terminal.buffer

import terminal.ansi.TerminalCommand

class TerminalBuffer(
    width: Int,
    height: Int,
    val maxScrollbackSize: Int = 1000
) {
    var width: Int = width
        private set
    var height: Int = height
        private set
    private val screen: MutableList<Line> = MutableList(height) { Line(width) }
    private val scrollback: MutableList<Line> = mutableListOf()
    private var cursorColumn: Int = 0
    private var cursorRow: Int = 0
    var currentAttributes: TextAttributes = TextAttributes.DEFAULT
    private var scrollTop: Int = 0
    private var scrollBottom: Int = height - 1

    val scrollbackSize: Int get() = scrollback.size

    // --- Cursor ---

    fun getCursorPosition(): CursorPosition = CursorPosition(cursorColumn, cursorRow)

    fun setCursorPosition(column: Int, row: Int) {
        cursorColumn = column.coerceIn(0, width - 1)
        cursorRow = row.coerceIn(0, height - 1)
    }

    // --- Content Access ---

    fun getCell(column: Int, row: Int): Cell = screen[row][column]

    fun getScrollbackCell(column: Int, row: Int): Cell = scrollback[row][column]

    fun getLineAsString(row: Int): String = screen[row].getText()

    fun getScrollbackLineAsString(row: Int): String = scrollback[row].getText()

    fun getScreenContent(): String {
        val lines = (0 until height).map { getLineAsString(it) }
        return lines.joinToString("\n").trimEnd('\n', ' ')
    }

    fun getFullContent(): String {
        val sbLines = (0 until scrollbackSize).map { getScrollbackLineAsString(it) }
        val scLines = (0 until height).map { getLineAsString(it) }
        val all = sbLines + scLines
        return all.joinToString("\n").trimEnd('\n', ' ')
    }

    // --- Cursor Movement ---

    fun moveCursorUp(n: Int) {
        cursorRow = (cursorRow - n).coerceAtLeast(0)
    }

    fun moveCursorDown(n: Int) {
        cursorRow = (cursorRow + n).coerceAtMost(height - 1)
    }

    fun moveCursorLeft(n: Int) {
        cursorColumn = (cursorColumn - n).coerceAtLeast(0)
    }

    fun moveCursorRight(n: Int) {
        cursorColumn = (cursorColumn + n).coerceAtMost(width - 1)
    }

    // --- Editing ---

    fun writeText(text: String) {
        if (text.isEmpty()) return
        for (ch in text) {
            val isWide = WideCharUtil.isWide(ch)
            val charWidth = if (isWide) 2 else 1

            if (cursorColumn + charWidth > width) break

            clearWideCharAt(cursorColumn, cursorRow)
            if (isWide && cursorColumn + 1 < width) {
                clearWideCharAt(cursorColumn + 1, cursorRow)
            }

            screen[cursorRow][cursorColumn] = Cell(ch, currentAttributes, charWidth)
            if (isWide && cursorColumn + 1 < width) {
                screen[cursorRow][cursorColumn + 1] = Cell(' ', currentAttributes, 0)
            }

            cursorColumn = (cursorColumn + charWidth).coerceAtMost(width - 1)
        }
    }

    private fun clearWideCharAt(column: Int, row: Int) {
        val cell = screen[row][column]
        if (cell.width == 2 && column + 1 < width) {
            screen[row][column + 1] = Cell()
        } else if (cell.width == 0 && column > 0) {
            screen[row][column - 1] = Cell()
        }
    }

    fun fillLine(char: Char = ' ') {
        val cell = Cell(char, currentAttributes)
        screen[cursorRow].clear(cell)
    }

    fun insertLineAtBottom() {
        scrollInRegion()
    }

    fun clearScreen() {
        for (line in screen) {
            line.clear()
        }
        cursorColumn = 0
        cursorRow = 0
    }

    fun clearScreenAndScrollback() {
        clearScreen()
        scrollback.clear()
    }

    // --- Scroll Region ---

    fun setScrollRegion(top: Int, bottom: Int) {
        if (top < 0 || bottom >= height || top > bottom) return
        scrollTop = top
        scrollBottom = bottom
    }

    fun resetScrollRegion() {
        scrollTop = 0
        scrollBottom = height - 1
    }

    fun scrollInRegion() {
        if (scrollTop >= scrollBottom) {
            // Single-line region — just clear it
            screen[scrollTop].clear()
            return
        }
        val removedLine = screen.removeAt(scrollTop)
        if (scrollTop == 0 && scrollBottom == height - 1) {
            // Full screen scroll — add to scrollback
            scrollback.add(removedLine)
            if (scrollback.size > maxScrollbackSize) {
                scrollback.removeAt(0)
            }
        }
        screen.add(scrollBottom, Line(width))
    }

    // --- Resize ---

    fun resize(newWidth: Int, newHeight: Int) {
        // Resize width for all lines
        if (newWidth != width) {
            for (i in screen.indices) {
                screen[i] = screen[i].resizedTo(newWidth)
            }
            for (i in scrollback.indices) {
                scrollback[i] = scrollback[i].resizedTo(newWidth)
            }
        }

        // Resize height
        if (newHeight > height) {
            // Pull lines from scrollback
            val linesToPull = minOf(newHeight - height, scrollback.size)
            val pulled = mutableListOf<Line>()
            repeat(linesToPull) {
                pulled.add(0, scrollback.removeAt(scrollback.size - 1))
            }
            screen.addAll(0, pulled)
            // Fill remaining with empty lines
            while (screen.size < newHeight) {
                screen.add(Line(newWidth))
            }
        } else if (newHeight < height) {
            // Push top lines to scrollback
            val linesToPush = height - newHeight
            repeat(linesToPush) {
                scrollback.add(screen.removeAt(0))
                if (scrollback.size > maxScrollbackSize) {
                    scrollback.removeAt(0)
                }
            }
        }

        width = newWidth
        height = newHeight

        // Clamp cursor
        cursorColumn = cursorColumn.coerceIn(0, width - 1)
        cursorRow = cursorRow.coerceIn(0, height - 1)
        resetScrollRegion()
    }

    fun insertText(text: String) {
        if (text.isEmpty()) return
        val line = screen[cursorRow]
        for (ch in text) {
            if (cursorColumn >= width) break
            for (i in width - 1 downTo cursorColumn + 1) {
                line[i] = line[i - 1]
            }
            line[cursorColumn] = Cell(ch, currentAttributes)
            cursorColumn = (cursorColumn + 1).coerceAtMost(width - 1)
        }
    }

    // --- applyCommand ---

    fun applyCommand(command: TerminalCommand) {
        when (command) {
            is TerminalCommand.Print -> {
                writeText(command.char.toString())
            }
            is TerminalCommand.SetCursorPosition -> {
                setCursorPosition(column = command.column, row = command.row)
            }
            is TerminalCommand.CursorUp -> moveCursorUp(command.n)
            is TerminalCommand.CursorDown -> moveCursorDown(command.n)
            is TerminalCommand.CursorForward -> moveCursorRight(command.n)
            is TerminalCommand.CursorBackward -> moveCursorLeft(command.n)
            is TerminalCommand.EraseDisplay -> eraseDisplay(command.mode)
            is TerminalCommand.EraseLine -> eraseLine(command.mode)
            is TerminalCommand.SetForeground -> {
                currentAttributes = currentAttributes.copy(foreground = command.color)
            }
            is TerminalCommand.SetBackground -> {
                currentAttributes = currentAttributes.copy(background = command.color)
            }
            is TerminalCommand.SetStyleOn -> {
                currentAttributes = currentAttributes.copy(styles = currentAttributes.styles + command.style)
            }
            is TerminalCommand.SetStyleOff -> {
                currentAttributes = currentAttributes.copy(styles = currentAttributes.styles - command.style)
            }
            is TerminalCommand.ResetAttributes -> {
                currentAttributes = TextAttributes.DEFAULT
            }
            is TerminalCommand.CarriageReturn -> {
                cursorColumn = 0
            }
            is TerminalCommand.LineFeed -> {
                if (cursorRow == scrollBottom) {
                    scrollInRegion()
                } else if (cursorRow < height - 1) {
                    cursorRow++
                }
            }
            is TerminalCommand.Backspace -> {
                cursorColumn = (cursorColumn - 1).coerceAtLeast(0)
            }
            is TerminalCommand.InsertCharacters -> insertCharacters(command.n)
            is TerminalCommand.DeleteCharacters -> deleteCharacters(command.n)
            is TerminalCommand.InsertLines -> insertLines(command.n)
            is TerminalCommand.DeleteLines -> deleteLines(command.n)
            is TerminalCommand.SetScrollRegion -> {
                // Bug fix: ESC[r (no params) produces SetScrollRegion(0, 0) — reset to full screen
                if (command.bottom == 0 || command.bottom < command.top) {
                    resetScrollRegion()
                } else {
                    setScrollRegion(command.top, command.bottom)
                }
            }
        }
    }

    // --- Erase Operations ---

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                // Clear from cursor to end of screen
                for (col in cursorColumn until width) {
                    screen[cursorRow][col] = Cell()
                }
                for (row in cursorRow + 1 until height) {
                    screen[row].clear()
                }
            }
            1 -> {
                // Clear from start to cursor
                for (row in 0 until cursorRow) {
                    screen[row].clear()
                }
                for (col in 0..cursorColumn) {
                    screen[cursorRow][col] = Cell()
                }
            }
            2 -> {
                val savedCol = cursorColumn
                val savedRow = cursorRow
                clearScreen()
                cursorColumn = savedCol
                cursorRow = savedRow
            }
            3 -> clearScreenAndScrollback()
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> {
                for (col in cursorColumn until width) {
                    screen[cursorRow][col] = Cell()
                }
            }
            1 -> {
                for (col in 0..cursorColumn) {
                    screen[cursorRow][col] = Cell()
                }
            }
            2 -> screen[cursorRow].clear()
        }
    }

    // --- Insert/Delete Characters and Lines ---

    private fun insertCharacters(n: Int) {
        val line = screen[cursorRow]
        for (i in width - 1 downTo cursorColumn + n) {
            line[i] = line[i - n]
        }
        for (i in cursorColumn until (cursorColumn + n).coerceAtMost(width)) {
            line[i] = Cell(' ', currentAttributes)
        }
    }

    private fun deleteCharacters(n: Int) {
        val line = screen[cursorRow]
        for (i in cursorColumn until (width - n).coerceAtLeast(cursorColumn)) {
            line[i] = line[i + n]
        }
        for (i in (width - n).coerceAtLeast(cursorColumn) until width) {
            line[i] = Cell()
        }
    }

    private fun insertLines(n: Int) {
        val actualN = n.coerceAtMost(scrollBottom - cursorRow + 1)
        repeat(actualN) {
            if (cursorRow in scrollTop..scrollBottom) {
                screen.removeAt(scrollBottom)
                screen.add(cursorRow, Line(width))
            }
        }
    }

    private fun deleteLines(n: Int) {
        val actualN = n.coerceAtMost(scrollBottom - cursorRow + 1)
        repeat(actualN) {
            if (cursorRow in scrollTop..scrollBottom) {
                screen.removeAt(cursorRow)
                screen.add(scrollBottom, Line(width))
            }
        }
    }
}
