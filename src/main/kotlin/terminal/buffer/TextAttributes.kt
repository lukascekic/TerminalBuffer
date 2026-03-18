package terminal.buffer

/**
 * Immutable visual attributes applied to a terminal cell.
 *
 * Uses a [Set] of [Style] values (not EnumSet) to preserve true immutability with
 * data class [copy]. Add a style with `styles + style`, remove with `styles - style`.
 *
 * @property foreground The foreground (text) color. Defaults to the terminal default color.
 * @property background The background color. Defaults to the terminal default color.
 * @property styles The set of active text styles (bold, italic, underline).
 */
data class TextAttributes(
    val foreground: Color = Color.DEFAULT,
    val background: Color = Color.DEFAULT,
    val styles: Set<Style> = emptySet()
) {
    companion object {
        /** Default attributes: terminal default colors, no styles. */
        val DEFAULT = TextAttributes()
    }
}
