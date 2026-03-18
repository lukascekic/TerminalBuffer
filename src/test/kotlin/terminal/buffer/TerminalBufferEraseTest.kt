package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import terminal.ansi.TerminalCommand

class TerminalBufferEraseTest {

    @Nested
    inner class EraseDisplay {

        @Test
        fun `mode 0 clears from cursor to end of screen`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            for (row in 0 until 3) {
                buffer.setCursorPosition(0, row)
                buffer.writeText("Line$row")
            }
            buffer.setCursorPosition(3, 1)
            buffer.applyCommand(TerminalCommand.EraseDisplay(0))
            buffer.getLineAsString(0) shouldBe "Line0"
            buffer.getLineAsString(1) shouldBe "Lin"
            buffer.getLineAsString(2) shouldBe ""
        }

        @Test
        fun `mode 1 clears from start to cursor`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            for (row in 0 until 3) {
                buffer.setCursorPosition(0, row)
                buffer.writeText("Line$row")
            }
            buffer.setCursorPosition(3, 1)
            buffer.applyCommand(TerminalCommand.EraseDisplay(1))
            buffer.getLineAsString(0) shouldBe ""
            buffer.getLineAsString(1) shouldBe "    1"
            buffer.getLineAsString(2) shouldBe "Line2"
        }

        @Test
        fun `mode 2 clears screen but preserves cursor position`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(5, 2)
            buffer.writeText("Test")
            buffer.setCursorPosition(5, 2)
            buffer.applyCommand(TerminalCommand.EraseDisplay(2))
            buffer.getScreenContent() shouldBe ""
            buffer.getCursorPosition() shouldBe CursorPosition(5, 2)
        }

        @Test
        fun `mode 3 clears screen and scrollback`() {
            val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
            buffer.writeText("Data")
            buffer.insertLineAtBottom()
            buffer.applyCommand(TerminalCommand.EraseDisplay(3))
            buffer.getScreenContent() shouldBe ""
            buffer.scrollbackSize shouldBe 0
        }
    }

    @Nested
    inner class EraseLine {

        @Test
        fun `mode 0 clears from cursor to end of line`() {
            val buffer = TerminalBuffer(width = 10, height = 2)
            buffer.writeText("HelloWorld")
            buffer.setCursorPosition(5, 0)
            buffer.applyCommand(TerminalCommand.EraseLine(0))
            buffer.getLineAsString(0) shouldBe "Hello"
        }

        @Test
        fun `mode 1 clears from start to cursor`() {
            val buffer = TerminalBuffer(width = 10, height = 2)
            buffer.writeText("HelloWorld")
            buffer.setCursorPosition(5, 0)
            buffer.applyCommand(TerminalCommand.EraseLine(1))
            buffer.getLineAsString(0) shouldBe "      orld"
        }

        @Test
        fun `mode 2 clears entire line`() {
            val buffer = TerminalBuffer(width = 10, height = 2)
            buffer.writeText("HelloWorld")
            buffer.applyCommand(TerminalCommand.EraseLine(2))
            buffer.getLineAsString(0) shouldBe ""
        }
    }
}
