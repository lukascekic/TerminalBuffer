package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TerminalBufferResizeTest {

    @Test
    fun `increase width pads lines`() {
        val buffer = TerminalBuffer(width = 5, height = 3)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("AB")
        buffer.resize(10, 3)
        buffer.width shouldBe 10
        buffer.getLineAsString(0) shouldBe "AB"
        buffer.getCell(5, 0) shouldBe Cell()
    }

    @Test
    fun `decrease width truncates lines`() {
        val buffer = TerminalBuffer(width = 10, height = 3)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("ABCDEFGHIJ")
        buffer.resize(5, 3)
        buffer.width shouldBe 5
        buffer.getLineAsString(0) shouldBe "ABCDE"
    }

    @Test
    fun `increase height pulls from scrollback`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Scrolled")
        buffer.insertLineAtBottom()
        buffer.scrollbackSize shouldBe 1
        buffer.resize(10, 4)
        buffer.height shouldBe 4
        buffer.scrollbackSize shouldBe 0
        buffer.getLineAsString(0) shouldBe "Scrolled"
    }

    @Test
    fun `decrease height pushes to scrollback`() {
        val buffer = TerminalBuffer(width = 10, height = 4)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Line0")
        buffer.setCursorPosition(0, 1)
        buffer.writeText("Line1")
        buffer.setCursorPosition(0, 2)
        buffer.writeText("Line2")
        buffer.resize(10, 2)
        buffer.height shouldBe 2
        buffer.scrollbackSize shouldBe 2
        buffer.getScrollbackLineAsString(0) shouldBe "Line0"
        buffer.getScrollbackLineAsString(1) shouldBe "Line1"
        buffer.getLineAsString(0) shouldBe "Line2"
    }

    @Test
    fun `resize clamps cursor position`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setCursorPosition(8, 4)
        buffer.resize(5, 3)
        buffer.getCursorPosition().column shouldBe 4
        buffer.getCursorPosition().row shouldBe 2
    }

    @Test
    fun `resize to same size is no-op`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Test")
        buffer.resize(10, 5)
        buffer.getLineAsString(0) shouldBe "Test"
    }

    @Test
    fun `increase height with empty scrollback adds empty lines`() {
        val buffer = TerminalBuffer(width = 10, height = 2)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Hello")
        buffer.resize(10, 5)
        buffer.height shouldBe 5
        buffer.scrollbackSize shouldBe 0
        buffer.getLineAsString(0) shouldBe "Hello"
        buffer.getLineAsString(2) shouldBe ""
        buffer.getLineAsString(3) shouldBe ""
        buffer.getLineAsString(4) shouldBe ""
    }
}
