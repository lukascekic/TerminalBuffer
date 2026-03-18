package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TerminalBufferCursorTest {

    private lateinit var buffer: TerminalBuffer

    @BeforeEach
    fun setUp() {
        buffer = TerminalBuffer(width = 10, height = 5)
    }

    @Nested
    inner class SetCursorPosition {

        @Test
        fun `sets cursor to valid position`() {
            buffer.setCursorPosition(5, 3)
            buffer.getCursorPosition() shouldBe CursorPosition(5, 3)
        }

        @Test
        fun `clamps negative column to 0`() {
            buffer.setCursorPosition(-1, 0)
            buffer.getCursorPosition().column shouldBe 0
        }

        @Test
        fun `clamps column beyond width`() {
            buffer.setCursorPosition(20, 0)
            buffer.getCursorPosition().column shouldBe 9
        }

        @Test
        fun `clamps negative row to 0`() {
            buffer.setCursorPosition(0, -5)
            buffer.getCursorPosition().row shouldBe 0
        }

        @Test
        fun `clamps row beyond height`() {
            buffer.setCursorPosition(0, 10)
            buffer.getCursorPosition().row shouldBe 4
        }

        @ParameterizedTest(name = "setCursor({0},{1}) -> ({2},{3})")
        @CsvSource(
            "0,0,0,0", "9,4,9,4", "-1,-1,0,0", "10,5,9,4", "5,2,5,2"
        )
        fun `clamping boundary conditions`(col: Int, row: Int, expCol: Int, expRow: Int) {
            buffer.setCursorPosition(col, row)
            buffer.getCursorPosition().column shouldBe expCol
            buffer.getCursorPosition().row shouldBe expRow
        }
    }

    @Nested
    inner class MoveCursor {

        @Test
        fun `moveUp from row 3 by 2`() {
            buffer.setCursorPosition(0, 3)
            buffer.moveCursorUp(2)
            buffer.getCursorPosition().row shouldBe 1
        }

        @Test
        fun `moveUp clamps at 0`() {
            buffer.setCursorPosition(0, 1)
            buffer.moveCursorUp(5)
            buffer.getCursorPosition().row shouldBe 0
        }

        @Test
        fun `moveDown from row 2 by 1`() {
            buffer.setCursorPosition(0, 2)
            buffer.moveCursorDown(1)
            buffer.getCursorPosition().row shouldBe 3
        }

        @Test
        fun `moveDown clamps at height-1`() {
            buffer.setCursorPosition(0, 3)
            buffer.moveCursorDown(10)
            buffer.getCursorPosition().row shouldBe 4
        }

        @Test
        fun `moveLeft from col 5 by 3`() {
            buffer.setCursorPosition(5, 0)
            buffer.moveCursorLeft(3)
            buffer.getCursorPosition().column shouldBe 2
        }

        @Test
        fun `moveLeft clamps at 0`() {
            buffer.setCursorPosition(2, 0)
            buffer.moveCursorLeft(10)
            buffer.getCursorPosition().column shouldBe 0
        }

        @Test
        fun `moveRight from col 5 by 2`() {
            buffer.setCursorPosition(5, 0)
            buffer.moveCursorRight(2)
            buffer.getCursorPosition().column shouldBe 7
        }

        @Test
        fun `moveRight clamps at width-1`() {
            buffer.setCursorPosition(8, 0)
            buffer.moveCursorRight(10)
            buffer.getCursorPosition().column shouldBe 9
        }

        @Test
        fun `moveUp by 0 does nothing`() {
            buffer.setCursorPosition(3, 2)
            buffer.moveCursorUp(0)
            buffer.getCursorPosition() shouldBe CursorPosition(3, 2)
        }

        @ParameterizedTest(name = "moveDown({0}) from row 2 -> row {1}")
        @CsvSource("0,2", "1,3", "2,4", "3,4", "100,4")
        fun `moveDown boundary conditions`(n: Int, expectedRow: Int) {
            buffer.setCursorPosition(0, 2)
            buffer.moveCursorDown(n)
            buffer.getCursorPosition().row shouldBe expectedRow
        }
    }
}
