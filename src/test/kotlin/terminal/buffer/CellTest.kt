package terminal.buffer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.EnumSet

class CellTest {

    @Nested
    inner class TextAttributesTest {

        @Test
        fun `DEFAULT has default colors and no styles`() {
            val attrs = TextAttributes.DEFAULT
            attrs.foreground shouldBe Color.DEFAULT
            attrs.background shouldBe Color.DEFAULT
            attrs.styles shouldBe EnumSet.noneOf(Style::class.java)
        }

        @Test
        fun `custom attributes preserve values`() {
            val attrs = TextAttributes(
                foreground = Color.RED,
                background = Color.BLUE,
                styles = EnumSet.of(Style.BOLD, Style.ITALIC)
            )
            attrs.foreground shouldBe Color.RED
            attrs.background shouldBe Color.BLUE
            attrs.styles shouldBe EnumSet.of(Style.BOLD, Style.ITALIC)
        }

        @Test
        fun `data class equals works structurally`() {
            val a = TextAttributes(foreground = Color.RED)
            val b = TextAttributes(foreground = Color.RED)
            a shouldBe b
        }

        @Test
        fun `different attributes are not equal`() {
            val a = TextAttributes(foreground = Color.RED)
            val b = TextAttributes(foreground = Color.GREEN)
            a shouldNotBe b
        }
    }

    @Nested
    inner class CellDataTest {

        @Test
        fun `default cell is space with default attributes and width 1`() {
            val cell = Cell()
            cell.char shouldBe ' '
            cell.attributes shouldBe TextAttributes.DEFAULT
            cell.width shouldBe 1
        }

        @Test
        fun `cell preserves char and attributes`() {
            val attrs = TextAttributes(foreground = Color.GREEN, styles = EnumSet.of(Style.UNDERLINE))
            val cell = Cell(char = 'A', attributes = attrs)
            cell.char shouldBe 'A'
            cell.attributes shouldBe attrs
            cell.width shouldBe 1
        }

        @Test
        fun `wide cell has width 2`() {
            val cell = Cell(char = '中', width = 2)
            cell.width shouldBe 2
        }

        @Test
        fun `continuation cell has width 0`() {
            val cell = Cell(char = ' ', width = 0)
            cell.width shouldBe 0
        }
    }

    @Nested
    inner class CursorPositionTest {

        @Test
        fun `cursor position holds column and row`() {
            val pos = CursorPosition(5, 10)
            pos.column shouldBe 5
            pos.row shouldBe 10
        }
    }
}
