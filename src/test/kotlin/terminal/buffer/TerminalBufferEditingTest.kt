package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TerminalBufferEditingTest {

    private lateinit var buffer: TerminalBuffer

    @BeforeEach
    fun setUp() {
        buffer = TerminalBuffer(width = 10, height = 5)
    }

    @Nested
    inner class WriteText {

        @Test
        fun `writes text at cursor and advances cursor`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Hello")
            buffer.getLineAsString(0) shouldBe "Hello"
            buffer.getCursorPosition().column shouldBe 5
        }

        @Test
        fun `writes text with current attributes`() {
            val attrs = TextAttributes(foreground = Color.RED, styles = setOf(Style.BOLD))
            buffer.currentAttributes = attrs
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Hi")
            buffer.getCell(0, 0).attributes shouldBe attrs
            buffer.getCell(1, 0).attributes shouldBe attrs
        }

        @Test
        fun `overwrites existing content`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("AAAA")
            buffer.setCursorPosition(1, 0)
            buffer.writeText("BB")
            buffer.getLineAsString(0) shouldBe "ABBA"
        }

        @Test
        fun `stops at right edge of screen`() {
            buffer.setCursorPosition(8, 0)
            buffer.writeText("ABCDE")
            buffer.getLineAsString(0) shouldBe "        AE"
            buffer.getCursorPosition().column shouldBe 9
        }

        @Test
        fun `empty string does nothing`() {
            buffer.setCursorPosition(3, 2)
            buffer.writeText("")
            buffer.getCursorPosition() shouldBe CursorPosition(3, 2)
        }

        @Test
        fun `writes at mid-line position`() {
            buffer.setCursorPosition(3, 1)
            buffer.writeText("XY")
            buffer.getLineAsString(1) shouldBe "   XY"
            buffer.getCursorPosition().column shouldBe 5
        }
    }

    @Nested
    inner class FillLine {

        @Test
        fun `fills current line with character using current attributes`() {
            val attrs = TextAttributes(foreground = Color.GREEN)
            buffer.currentAttributes = attrs
            buffer.setCursorPosition(0, 2)
            buffer.fillLine('=')
            buffer.getLineAsString(2) shouldBe "=========="
            buffer.getCell(0, 2).attributes shouldBe attrs
            buffer.getCell(9, 2).attributes shouldBe attrs
        }

        @Test
        fun `fills with space effectively clears the line`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Hello")
            buffer.setCursorPosition(0, 0)
            buffer.fillLine(' ')
            buffer.getLineAsString(0) shouldBe ""
        }
    }

    @Nested
    inner class InsertLineAtBottom {

        @Test
        fun `scrolls screen up and adds empty line at bottom`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Line0")
            buffer.setCursorPosition(0, 1)
            buffer.writeText("Line1")
            buffer.insertLineAtBottom()
            buffer.getLineAsString(0) shouldBe "Line1"
            buffer.getLineAsString(4) shouldBe ""
            buffer.scrollbackSize shouldBe 1
            buffer.getScrollbackLineAsString(0) shouldBe "Line0"
        }

        @Test
        fun `respects max scrollback size`() {
            val buf = TerminalBuffer(width = 10, height = 2, maxScrollbackSize = 3)
            repeat(5) { i ->
                buf.setCursorPosition(0, 0)
                buf.writeText("L$i")
                buf.insertLineAtBottom()
            }
            buf.scrollbackSize shouldBe 3
            buf.getScrollbackLineAsString(0) shouldBe "L2"
        }
    }

    @Nested
    inner class ClearScreen {

        @Test
        fun `clearScreen resets all cells but preserves scrollback`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Test")
            buffer.insertLineAtBottom()
            buffer.clearScreen()
            buffer.getScreenContent() shouldBe ""
            buffer.scrollbackSize shouldBe 1
        }

        @Test
        fun `clearScreenAndScrollback clears everything`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("Test")
            buffer.insertLineAtBottom()
            buffer.clearScreenAndScrollback()
            buffer.getScreenContent() shouldBe ""
            buffer.scrollbackSize shouldBe 0
        }
    }

    @Nested
    inner class InsertText {

        @Test
        fun `inserts text shifting existing content right`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("ABCDE")
            buffer.setCursorPosition(2, 0)
            buffer.insertText("XX")
            buffer.getLineAsString(0) shouldBe "ABXXCDE"
        }

        @Test
        fun `insert pushes chars off right edge`() {
            buffer.setCursorPosition(0, 0)
            buffer.writeText("1234567890")
            buffer.setCursorPosition(5, 0)
            buffer.insertText("XX")
            buffer.getLineAsString(0) shouldBe "12345XX678"
        }

        @Test
        fun `insert at end of line behaves like write`() {
            buffer.setCursorPosition(5, 0)
            buffer.insertText("AB")
            buffer.getLineAsString(0) shouldBe "     AB"
        }

        @Test
        fun `insert uses current attributes`() {
            val attrs = TextAttributes(foreground = Color.CYAN)
            buffer.currentAttributes = attrs
            buffer.setCursorPosition(0, 0)
            buffer.insertText("Hi")
            buffer.getCell(0, 0).attributes shouldBe attrs
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `writeText on 1-wide buffer`() {
            val tiny = TerminalBuffer(width = 1, height = 1)
            tiny.writeText("X")
            tiny.getCell(0, 0).char shouldBe 'X'
            tiny.getCursorPosition().column shouldBe 0
        }

        @Test
        fun `fillLine on 1x1 buffer`() {
            val tiny = TerminalBuffer(width = 1, height = 1)
            tiny.fillLine('Z')
            tiny.getCell(0, 0).char shouldBe 'Z'
        }

        @Test
        fun `multiple insertLineAtBottom accumulate scrollback`() {
            val buf = TerminalBuffer(width = 10, height = 3, maxScrollbackSize = 100)
            repeat(5) { i ->
                buf.setCursorPosition(0, 0)
                buf.writeText("Line$i")
                buf.insertLineAtBottom()
            }
            buf.scrollbackSize shouldBe 5
            buf.getScrollbackLineAsString(0) shouldBe "Line0"
        }
    }
}
