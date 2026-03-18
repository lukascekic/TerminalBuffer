package terminal.ansi

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import terminal.buffer.*

class IntegrationTest {

    private lateinit var buffer: TerminalBuffer
    private lateinit var parser: VT100Parser

    @BeforeEach
    fun setUp() {
        buffer = TerminalBuffer(width = 40, height = 10)
        parser = VT100Parser { buffer.applyCommand(it) }
    }

    @Nested
    inner class TextOutput {

        @Test
        fun `plain text appears on screen`() {
            parser.feed("Hello, World!")
            buffer.getLineAsString(0) shouldBe "Hello, World!"
        }

        @Test
        fun `CR LF moves to next line`() {
            parser.feed("Line1\r\nLine2")
            buffer.getLineAsString(0) shouldBe "Line1"
            buffer.getLineAsString(1) shouldBe "Line2"
        }

        @Test
        fun `CR without LF returns to start of line`() {
            parser.feed("Old text\rNew")
            buffer.getLineAsString(0) shouldBe "New text"
        }
    }

    @Nested
    inner class CursorMovement {

        @Test
        fun `cursor position moves cursor to home`() {
            parser.feed("XXXXX")
            parser.feed("\u001B[1;1H")  // home
            parser.feed("Y")
            buffer.getLineAsString(0) shouldBe "YXXXX"
        }

        @Test
        fun `cursor movement overwrite`() {
            parser.feed("Hello World")
            parser.feed("\u001B[5D")  // back 5
            parser.feed("Brave")
            buffer.getLineAsString(0) shouldBe "Hello Brave"
        }
    }

    @Nested
    inner class SGRColors {

        @Test
        fun `bold red text has correct attributes`() {
            parser.feed("\u001B[1;31mHi\u001B[0m")
            buffer.getCell(0, 0).char shouldBe 'H'
            buffer.getCell(0, 0).attributes.foreground shouldBe Color.RED
            buffer.getCell(0, 0).attributes.styles shouldBe setOf(Style.BOLD)
            // After reset
            buffer.currentAttributes shouldBe TextAttributes.DEFAULT
        }

        @Test
        fun `reset returns to default attributes`() {
            parser.feed("\u001B[32mGreen\u001B[0mNormal")
            buffer.getCell(0, 0).attributes.foreground shouldBe Color.GREEN
            buffer.getCell(5, 0).attributes.foreground shouldBe Color.DEFAULT
        }

        @Test
        fun `bright colors`() {
            parser.feed("\u001B[91mBrightRed\u001B[0m")
            buffer.getCell(0, 0).attributes.foreground shouldBe Color.BRIGHT_RED
        }

        @Test
        fun `background color`() {
            parser.feed("\u001B[44mBlueBG\u001B[0m")
            buffer.getCell(0, 0).attributes.background shouldBe Color.BLUE
        }
    }

    @Nested
    inner class EraseOperations {

        @Test
        fun `erase to end of line`() {
            parser.feed("Hello World")
            parser.feed("\u001B[1;6H")  // cursor to col 5 (1-based col 6)
            parser.feed("\u001B[K")      // erase to end of line
            buffer.getLineAsString(0) shouldBe "Hello"
        }

        @Test
        fun `erase entire display`() {
            parser.feed("Line1\r\nLine2")
            parser.feed("\u001B[2J")
            buffer.getScreenContent() shouldBe ""
        }

        @Test
        fun `erase display mode 3 clears scrollback too`() {
            parser.feed("Data")
            buffer.insertLineAtBottom()
            parser.feed("\u001B[3J")
            buffer.getScreenContent() shouldBe ""
            buffer.scrollbackSize shouldBe 0
        }
    }

    @Nested
    inner class ScrollRegionIntegration {

        @Test
        fun `DECSTBM restricts scrolling`() {
            val b = TerminalBuffer(width = 20, height = 5)
            val p = VT100Parser { b.applyCommand(it) }

            // Write content on all lines
            for (i in 0 until 5) {
                p.feed("\u001B[${i + 1};1H")  // Move to row i+1
                p.feed("Line$i")
            }

            // Set scroll region to rows 2-4 (1-based)
            p.feed("\u001B[2;4r")

            // Move cursor to row 4 (bottom of region) and line feed
            p.feed("\u001B[4;1H")
            p.feed("\n")

            b.getLineAsString(0) shouldBe "Line0"  // outside region, untouched
            b.getLineAsString(1) shouldBe "Line2"  // scrolled up
            b.getLineAsString(2) shouldBe "Line3"  // scrolled up
            b.getLineAsString(3) shouldBe ""        // new empty line
            b.getLineAsString(4) shouldBe "Line4"  // outside region, untouched
        }
    }

    @Nested
    inner class RealisticShellOutput {

        @Test
        fun `simulates colored ls output`() {
            // bold blue "dir/" then reset, space, then green "file.txt" then reset
            parser.feed("\u001B[1;34mdir/\u001B[0m \u001B[32mfile.txt\u001B[0m")
            buffer.getCell(0, 0).attributes.foreground shouldBe Color.BLUE
            buffer.getCell(0, 0).attributes.styles shouldBe setOf(Style.BOLD)
            buffer.getCell(4, 0).attributes shouldBe TextAttributes.DEFAULT  // space
            buffer.getCell(5, 0).attributes.foreground shouldBe Color.GREEN
        }

        @Test
        fun `simulates progress bar with CR`() {
            parser.feed("[####      ] 40%\r[########  ] 80%")
            buffer.getLineAsString(0) shouldBe "[########  ] 80%"
        }
    }
}
