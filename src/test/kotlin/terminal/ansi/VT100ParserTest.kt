package terminal.ansi

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import terminal.buffer.Color
import terminal.buffer.Style

class VT100ParserTest {

    private val commands = mutableListOf<TerminalCommand>()
    private lateinit var parser: VT100Parser

    @BeforeEach
    fun setUp() {
        commands.clear()
        parser = VT100Parser { commands.add(it) }
    }

    @Nested
    inner class GroundState {

        @Test
        fun `printable characters emit Print commands`() {
            parser.feed("ABC")
            commands shouldContainExactly listOf(
                TerminalCommand.Print('A'),
                TerminalCommand.Print('B'),
                TerminalCommand.Print('C')
            )
        }

        @Test
        fun `carriage return emits CR command`() {
            parser.feed("\r")
            commands shouldContainExactly listOf(TerminalCommand.CarriageReturn)
        }

        @Test
        fun `line feed emits LF command`() {
            parser.feed("\n")
            commands shouldContainExactly listOf(TerminalCommand.LineFeed)
        }

        @Test
        fun `backspace emits BS command`() {
            parser.feed("\u0008")
            commands shouldContainExactly listOf(TerminalCommand.Backspace)
        }
    }

    @Nested
    inner class CsiSequences {

        @Test
        fun `cursor up CSI A defaults to 1`() {
            parser.feed("\u001B[A")
            commands shouldContainExactly listOf(TerminalCommand.CursorUp(1))
        }

        @Test
        fun `cursor up CSI 3A moves 3`() {
            parser.feed("\u001B[3A")
            commands shouldContainExactly listOf(TerminalCommand.CursorUp(3))
        }

        @Test
        fun `cursor down CSI B`() {
            parser.feed("\u001B[2B")
            commands shouldContainExactly listOf(TerminalCommand.CursorDown(2))
        }

        @Test
        fun `cursor forward CSI C`() {
            parser.feed("\u001B[5C")
            commands shouldContainExactly listOf(TerminalCommand.CursorForward(5))
        }

        @Test
        fun `cursor backward CSI D`() {
            parser.feed("\u001B[D")
            commands shouldContainExactly listOf(TerminalCommand.CursorBackward(1))
        }

        @Test
        fun `cursor position CSI row col H`() {
            parser.feed("\u001B[3;5H")
            // VT100 uses 1-based, we convert to 0-based
            commands shouldContainExactly listOf(TerminalCommand.SetCursorPosition(2, 4))
        }

        @Test
        fun `cursor position defaults to 1,1`() {
            parser.feed("\u001B[H")
            commands shouldContainExactly listOf(TerminalCommand.SetCursorPosition(0, 0))
        }

        @Test
        fun `erase display CSI 2J`() {
            parser.feed("\u001B[2J")
            commands shouldContainExactly listOf(TerminalCommand.EraseDisplay(2))
        }

        @Test
        fun `erase line CSI K defaults to 0`() {
            parser.feed("\u001B[K")
            commands shouldContainExactly listOf(TerminalCommand.EraseLine(0))
        }

        @Test
        fun `SGR reset CSI 0m`() {
            parser.feed("\u001B[0m")
            commands shouldContainExactly listOf(TerminalCommand.ResetAttributes)
        }

        @Test
        fun `SGR bold CSI 1m`() {
            parser.feed("\u001B[1m")
            commands shouldContainExactly listOf(TerminalCommand.SetStyleOn(Style.BOLD))
        }

        @Test
        fun `SGR foreground red CSI 31m`() {
            parser.feed("\u001B[31m")
            commands shouldContainExactly listOf(TerminalCommand.SetForeground(Color.RED))
        }

        @Test
        fun `SGR background blue CSI 44m`() {
            parser.feed("\u001B[44m")
            commands shouldContainExactly listOf(TerminalCommand.SetBackground(Color.BLUE))
        }

        @Test
        fun `SGR combined bold and red CSI 1,31m`() {
            parser.feed("\u001B[1;31m")
            commands shouldContainExactly listOf(
                TerminalCommand.SetStyleOn(Style.BOLD),
                TerminalCommand.SetForeground(Color.RED)
            )
        }

        @Test
        fun `DECSTBM scroll region CSI 2,10r`() {
            parser.feed("\u001B[2;10r")
            commands shouldContainExactly listOf(TerminalCommand.SetScrollRegion(1, 9))
        }

        @Test
        fun `insert characters CSI 3 at`() {
            parser.feed("\u001B[3@")
            commands shouldContainExactly listOf(TerminalCommand.InsertCharacters(3))
        }

        @Test
        fun `delete characters CSI 2P`() {
            parser.feed("\u001B[2P")
            commands shouldContainExactly listOf(TerminalCommand.DeleteCharacters(2))
        }

        @Test
        fun `insert lines CSI L`() {
            parser.feed("\u001B[L")
            commands shouldContainExactly listOf(TerminalCommand.InsertLines(1))
        }

        @Test
        fun `delete lines CSI 3M`() {
            parser.feed("\u001B[3M")
            commands shouldContainExactly listOf(TerminalCommand.DeleteLines(3))
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `SGR with no params resets attributes`() {
            parser.feed("\u001B[m")
            commands shouldContainExactly listOf(TerminalCommand.ResetAttributes)
        }

        @Test
        fun `SGR bright foreground CSI 91m`() {
            parser.feed("\u001B[91m")
            commands shouldContainExactly listOf(TerminalCommand.SetForeground(Color.BRIGHT_RED))
        }

        @Test
        fun `SGR bright background CSI 104m`() {
            parser.feed("\u001B[104m")
            commands shouldContainExactly listOf(TerminalCommand.SetBackground(Color.BRIGHT_BLUE))
        }

        @Test
        fun `CSI 0A treats 0 as default 1`() {
            parser.feed("\u001B[0A")
            commands shouldContainExactly listOf(TerminalCommand.CursorUp(1))
        }

        @Test
        fun `cursor position f is alias for H`() {
            parser.feed("\u001B[3;5f")
            commands shouldContainExactly listOf(TerminalCommand.SetCursorPosition(2, 4))
        }

        @Test
        fun `SGR default foreground CSI 39m`() {
            parser.feed("\u001B[39m")
            commands shouldContainExactly listOf(TerminalCommand.SetForeground(Color.DEFAULT))
        }

        @Test
        fun `SGR default background CSI 49m`() {
            parser.feed("\u001B[49m")
            commands shouldContainExactly listOf(TerminalCommand.SetBackground(Color.DEFAULT))
        }

        @Test
        fun `OSC string terminated by BEL emits no commands`() {
            parser.feed("\u001B]0;window title\u0007")
            commands shouldBe emptyList()
        }

        @Test
        fun `DEC private mode sequence is silently consumed`() {
            parser.feed("\u001B[?25h")
            commands shouldBe emptyList()
        }

        @Test
        fun `DEC private mode followed by normal sequence`() {
            parser.feed("\u001B[?25h\u001B[2A")
            commands shouldContainExactly listOf(TerminalCommand.CursorUp(2))
        }

        @Test
        fun `malformed CSI followed by valid sequence`() {
            parser.feed("\u001B[1\u0000\u001B[2A")
            commands shouldContainExactly listOf(TerminalCommand.CursorUp(2))
        }
    }
}
