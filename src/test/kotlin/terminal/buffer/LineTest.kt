package terminal.buffer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class LineTest {

    @Test
    fun `new line has correct width and empty cells`() {
        val line = Line(10)
        line.width shouldBe 10
        line[0] shouldBe Cell()
        line[9] shouldBe Cell()
    }

    @Test
    fun `set and get cell`() {
        val line = Line(10)
        val cell = Cell('A', TextAttributes(foreground = Color.RED))
        line[3] = cell
        line[3] shouldBe cell
    }

    @Test
    fun `getText returns trimmed string`() {
        val line = Line(10)
        line[0] = Cell('H')
        line[1] = Cell('i')
        line.getText() shouldBe "Hi"
    }

    @Test
    fun `getText on empty line returns empty string`() {
        val line = Line(10)
        line.getText() shouldBe ""
    }

    @Test
    fun `clear resets all cells`() {
        val line = Line(5)
        line[0] = Cell('X')
        line[1] = Cell('Y')
        line.clear()
        line[0] shouldBe Cell()
        line[1] shouldBe Cell()
    }

    @Test
    fun `copyOf creates independent copy`() {
        val line = Line(5)
        line[0] = Cell('A')
        val copy = line.copyOf()
        copy[0] shouldBe Cell('A')
        copy[0] = Cell('B')
        line[0] shouldBe Cell('A')
        copy[0] shouldBe Cell('B')
    }

    @Test
    fun `getText with cells in the middle has leading spaces`() {
        val line = Line(10)
        line[3] = Cell('X')
        line.getText() shouldBe "   X"
    }

    @Test
    fun `clear with specific cell fills entire line`() {
        val line = Line(3)
        val cell = Cell('*', TextAttributes(foreground = Color.RED))
        line.clear(cell)
        line[0] shouldBe cell
        line[1] shouldBe cell
        line[2] shouldBe cell
    }
}
