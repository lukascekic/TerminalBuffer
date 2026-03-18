package terminal.buffer

/**
 * The current cursor position within the terminal screen.
 *
 * Both [column] and [row] are 0-based indices. The cursor is always clamped
 * to valid screen bounds: column in [0, width-1], row in [0, height-1].
 *
 * @property column The 0-based column index (horizontal position).
 * @property row The 0-based row index (vertical position).
 */
data class CursorPosition(val column: Int, val row: Int)
