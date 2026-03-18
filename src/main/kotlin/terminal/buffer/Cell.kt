package terminal.buffer

data class Cell(
    val char: Char = ' ',
    val attributes: TextAttributes = TextAttributes.DEFAULT,
    val width: Int = 1
)
