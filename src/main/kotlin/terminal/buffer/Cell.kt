package terminal.buffer

/**
 * Represents a single cell in the terminal grid.
 *
 * Each cell holds one character along with its visual attributes and display width.
 * Wide characters (e.g. CJK ideographs) occupy two consecutive cells: the first cell has
 * [width] = 2 and contains the character; the second (continuation) cell has [width] = 0.
 *
 * @property char The character displayed in this cell. Defaults to space.
 * @property attributes The visual attributes (colors, styles) applied to this cell.
 * @property width The display width: 1 for normal characters, 2 for wide characters
 *   (CJK ideographs, certain emoji), 0 for continuation cells (right half of a wide character).
 */
data class Cell(
    val char: Char = ' ',
    val attributes: TextAttributes = TextAttributes.DEFAULT,
    val width: Int = 1
)
