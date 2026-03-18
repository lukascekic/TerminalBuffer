package terminal.buffer

data class TextAttributes(
    val foreground: Color = Color.DEFAULT,
    val background: Color = Color.DEFAULT,
    val styles: Set<Style> = emptySet()
) {
    companion object {
        val DEFAULT = TextAttributes()
    }
}
