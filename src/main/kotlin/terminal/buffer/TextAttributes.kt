package terminal.buffer

import java.util.EnumSet

data class TextAttributes(
    val foreground: Color = Color.DEFAULT,
    val background: Color = Color.DEFAULT,
    val styles: EnumSet<Style> = EnumSet.noneOf(Style::class.java)
) {
    companion object {
        val DEFAULT = TextAttributes()
    }
}
