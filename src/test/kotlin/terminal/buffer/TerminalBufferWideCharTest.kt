package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TerminalBufferWideCharTest {

    @Nested
    inner class WideCharDetection {

        @Test
        fun `CJK ideograph is wide`() {
            WideCharUtil.isWide('中') shouldBe true
        }

        @Test
        fun `ASCII character is not wide`() {
            WideCharUtil.isWide('A') shouldBe false
        }

        @Test
        fun `Japanese katakana (fullwidth) is wide`() {
            WideCharUtil.isWide('ア') shouldBe true
        }

        @Test
        fun `Latin character is not wide`() {
            WideCharUtil.isWide('é') shouldBe false
        }
    }

    @Nested
    inner class WideCharWriting {

        @Test
        fun `wide char occupies 2 cells`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(0, 0)
            buffer.writeText("中")
            buffer.getCell(0, 0).char shouldBe '中'
            buffer.getCell(0, 0).width shouldBe 2
            buffer.getCell(1, 0).width shouldBe 0
            buffer.getCursorPosition().column shouldBe 2
        }

        @Test
        fun `wide char at end of line with only 1 column left is not written`() {
            val buffer = TerminalBuffer(width = 3, height = 2)
            buffer.setCursorPosition(2, 0)
            buffer.writeText("中")
            buffer.getCell(2, 0) shouldBe Cell()
            buffer.getCursorPosition().column shouldBe 2
        }

        @Test
        fun `multiple wide chars in sequence`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(0, 0)
            buffer.writeText("中文AB")
            buffer.getCell(0, 0).char shouldBe '中'
            buffer.getCell(0, 0).width shouldBe 2
            buffer.getCell(1, 0).width shouldBe 0
            buffer.getCell(2, 0).char shouldBe '文'
            buffer.getCell(2, 0).width shouldBe 2
            buffer.getCell(3, 0).width shouldBe 0
            buffer.getCell(4, 0).char shouldBe 'A'
            buffer.getCell(5, 0).char shouldBe 'B'
            buffer.getCursorPosition().column shouldBe 6
        }

        @Test
        fun `overwriting first half of wide char clears both halves`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(0, 0)
            buffer.writeText("中")
            buffer.setCursorPosition(0, 0)
            buffer.writeText("A")
            buffer.getCell(0, 0).char shouldBe 'A'
            buffer.getCell(0, 0).width shouldBe 1
            buffer.getCell(1, 0) shouldBe Cell()
        }

        @Test
        fun `overwriting second half of wide char clears both halves`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(0, 0)
            buffer.writeText("中")
            buffer.setCursorPosition(1, 0)
            buffer.writeText("B")
            buffer.getCell(0, 0) shouldBe Cell()
            buffer.getCell(1, 0).char shouldBe 'B'
            buffer.getCell(1, 0).width shouldBe 1
        }

        @Test
        fun `mixed wide and narrow characters`() {
            val buffer = TerminalBuffer(width = 10, height = 3)
            buffer.setCursorPosition(0, 0)
            buffer.writeText("A中B")
            buffer.getCell(0, 0).char shouldBe 'A'
            buffer.getCell(1, 0).char shouldBe '中'
            buffer.getCell(1, 0).width shouldBe 2
            buffer.getCell(2, 0).width shouldBe 0
            buffer.getCell(3, 0).char shouldBe 'B'
        }
    }
}
