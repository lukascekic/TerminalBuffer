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
    fun `resize preserves wide char fitting exactly at new width`() {
        val buffer = TerminalBuffer(width = 10, height = 1)
        buffer.writeText("A中")  // A at col 0, 中 at cols 1-2
        buffer.resize(3, 1)
        buffer.getCell(0, 0).char shouldBe 'A'
        buffer.getCell(1, 0).char shouldBe '中'
        buffer.getCell(1, 0).width shouldBe 2
        buffer.getCell(2, 0).width shouldBe 0  // valid continuation preserved
    }

    @Test
    fun `resize truncates wide char split at boundary`() {
        val buffer = TerminalBuffer(width = 10, height = 1)
        buffer.writeText("AB中")  // A at 0, B at 1, 中 at cols 2-3
        buffer.resize(3, 1)  // col 3 (continuation) cut off
        buffer.getCell(2, 0) shouldBe Cell()  // wide char main cell cleared
    }

    @Test
    fun `resize preserves wide chars in scrollback`() {
        val buffer = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 100)
        buffer.writeText("中文")
        buffer.insertLineAtBottom()
        buffer.scrollbackSize shouldBe 1
        buffer.getScrollbackCell(0, 0).char shouldBe '中'
        buffer.getScrollbackCell(0, 0).width shouldBe 2
        buffer.getScrollbackCell(2, 0).char shouldBe '文'
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
