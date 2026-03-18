package terminal.buffer

/**
 * A single row in the terminal screen grid, wrapping a fixed-width array of [Cell] values.
 *
 * @property width The number of columns in this line.
 */
class Line(val width: Int) {

    private val cells: Array<Cell> = Array(width) { Cell() }

    /** Returns the cell at the given [column] index. */
    operator fun get(column: Int): Cell = cells[column]

    /** Sets the cell at the given [column] index. */
    operator fun set(column: Int, cell: Cell) {
        cells[column] = cell
    }

    /**
     * Returns the text content of this line as a string.
     *
     * Continuation cells (width == 0, i.e. the right half of a wide character) are skipped.
     * Trailing spaces are trimmed.
     */
    fun getText(): String {
        val sb = StringBuilder()
        for (cell in cells) {
            if (cell.width != 0) {
                sb.append(cell.char)
            }
        }
        return sb.toString().trimEnd()
    }

    /**
     * Fills all cells in this line with [cell].
     *
     * Defaults to an empty cell (space with default attributes).
     */
    fun clear(cell: Cell = Cell()) {
        cells.fill(cell)
    }

    /** Returns a deep copy of this line. */
    fun copyOf(): Line {
        val copy = Line(width)
        cells.copyInto(copy.cells)
        return copy
    }

    /**
     * Returns a new [Line] resized to [newWidth], preserving as much content as possible.
     *
     * When shrinking, wide characters that would be split at the boundary are cleared to avoid
     * orphaned main cells (width=2 with no continuation) or orphaned continuation cells
     * (width=0 with no preceding main cell).
     */
    fun resizedTo(newWidth: Int): Line {
        val newLine = Line(newWidth)
        val copyWidth = minOf(width, newWidth)
        for (i in 0 until copyWidth) {
            newLine[i] = this[i]
        }
        // Fix: clear orphaned continuation cell — only if predecessor is NOT a valid wide char
        if (newWidth > 0 && newLine[newWidth - 1].width == 0
            && (newWidth < 2 || newLine[newWidth - 2].width != 2)) {
            newLine[newWidth - 1] = Cell()
        }
        // Fix: clear wide char whose second half would fall outside the new boundary
        if (newWidth > 0 && newLine[newWidth - 1].width == 2) {
            newLine[newWidth - 1] = Cell()
        }
        return newLine
    }
}
