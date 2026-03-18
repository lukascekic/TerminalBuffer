package terminal.buffer

class Line(val width: Int) {

    private val cells: Array<Cell> = Array(width) { Cell() }

    operator fun get(column: Int): Cell = cells[column]

    operator fun set(column: Int, cell: Cell) {
        cells[column] = cell
    }

    fun getText(): String {
        val sb = StringBuilder()
        for (cell in cells) {
            if (cell.width != 0) {
                sb.append(cell.char)
            }
        }
        return sb.toString().trimEnd()
    }

    fun clear(cell: Cell = Cell()) {
        cells.fill(cell)
    }

    fun copyOf(): Line {
        val copy = Line(width)
        cells.copyInto(copy.cells)
        return copy
    }

    fun resizedTo(newWidth: Int): Line {
        val newLine = Line(newWidth)
        val copyWidth = minOf(width, newWidth)
        for (i in 0 until copyWidth) {
            newLine[i] = this[i]
        }
        // Fix: if last cell is first half of a wide char, clear it to avoid dangling wide cell
        if (newWidth < width && newWidth > 0 && newLine[newWidth - 1].width == 2) {
            newLine[newWidth - 1] = Cell()
        }
        return newLine
    }
}
