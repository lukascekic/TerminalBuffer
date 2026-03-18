package terminal.buffer

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
        val topLine = screen.removeAt(0)
        scrollback.add(topLine)
        if (scrollback.size > maxScrollbackSize) {
            scrollback.removeAt(0)
        }
        screen.add(Line(width))
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
}
