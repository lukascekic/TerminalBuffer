package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TerminalBufferTest {

    @Test
    fun `constructor creates buffer with correct dimensions`() {
        val buffer = TerminalBuffer(80, 24)
        buffer.width shouldBe 80
        buffer.height shouldBe 24
    }

    @Test
    fun `initial screen is empty`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.getScreenContent() shouldBe ""
    }

    @Test
    fun `initial cursor is at 0 0`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.getCursorPosition() shouldBe CursorPosition(0, 0)
    }

    @Test
    fun `initial currentAttributes is DEFAULT`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.currentAttributes shouldBe TextAttributes.DEFAULT
    }

    @Test
    fun `getCell returns empty cell for unmodified position`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.getCell(0, 0) shouldBe Cell()
    }

    @Test
    fun `getLineAsString returns empty for unmodified line`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.getLineAsString(0) shouldBe ""
    }

    @Test
    fun `scrollbackSize is initially 0`() {
        val buffer = TerminalBuffer(10, 5)
        buffer.scrollbackSize shouldBe 0
    }

    @Test
    fun `getScreenContent with content on multiple lines`() {
        val buffer = TerminalBuffer(10, 3)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("AAA")
        buffer.setCursorPosition(0, 2)
        buffer.writeText("CCC")
        buffer.getScreenContent() shouldBe "AAA\n\nCCC"
    }

    @Test
    fun `getFullContent with no scrollback equals getScreenContent`() {
        val buffer = TerminalBuffer(10, 3)
        buffer.setCursorPosition(0, 0)
        buffer.writeText("Test")
        buffer.getFullContent() shouldBe buffer.getScreenContent()
    }
}
