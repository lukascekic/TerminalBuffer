package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TerminalBufferScrollbackTest {

    @Test
    fun `getFullContent includes scrollback then screen`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("ScrolledOff")
        buffer.insertLineAtBottom()
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Visible")
        buffer.getFullContent() shouldBe "ScrolledOf\nVisible"
    }

    @Test
    fun `scrollback preserves attributes`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
        val attrs = TextAttributes(foreground = Color.RED)
        buffer.currentAttributes = attrs
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Red")
        buffer.insertLineAtBottom()
        buffer.getScrollbackCell(0, 0).attributes shouldBe attrs
    }

    @Test
    fun `scrollback limit removes oldest lines`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 2)
        repeat(5) { i ->
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Line$i")
            buffer.insertLineAtBottom()
        }
        buffer.scrollbackSize shouldBe 2
        buffer.getScrollbackLineAsString(0) shouldBe "Line3"
        buffer.getScrollbackLineAsString(1) shouldBe "Line4"
    }

    @Test
    fun `clearScreenAndScrollback empties scrollback`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Data")
        buffer.insertLineAtBottom()
        buffer.clearScreenAndScrollback()
        buffer.scrollbackSize shouldBe 0
        buffer.getScreenContent() shouldBe ""
    }
}
