package terminal.buffer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import terminal.ansi.TerminalCommand

class TerminalBufferScrollRegionTest {

    @Test
    fun `setScrollRegion defines scroll boundaries`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setScrollRegion(1, 3)
        // Functional verification through scrolling behavior
        buffer.setCursorPosition(0, 0); buffer.writeText("Fixed0")
        buffer.setCursorPosition(0, 1); buffer.writeText("Scroll1")
        buffer.setCursorPosition(0, 2); buffer.writeText("Scroll2")
        buffer.setCursorPosition(0, 3); buffer.writeText("Scroll3")
        buffer.setCursorPosition(0, 4); buffer.writeText("Fixed4")

        buffer.scrollInRegion()

        buffer.getLineAsString(0) shouldBe "Fixed0"
        buffer.getLineAsString(1) shouldBe "Scroll2"
        buffer.getLineAsString(2) shouldBe "Scroll3"
        buffer.getLineAsString(3) shouldBe ""
        buffer.getLineAsString(4) shouldBe "Fixed4"
    }

    @Test
    fun `resetScrollRegion covers full screen`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setScrollRegion(1, 3)
        buffer.resetScrollRegion()
        // After reset, insertLineAtBottom scrolls the whole screen
        buffer.setCursorPosition(0, 0); buffer.writeText("Line0")
        buffer.insertLineAtBottom()
        buffer.scrollbackSize shouldBe 1
    }

    @Test
    fun `scroll region does not affect lines outside region`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setCursorPosition(0, 0); buffer.writeText("Top")
        buffer.setCursorPosition(0, 4); buffer.writeText("Bottom")
        buffer.setScrollRegion(1, 3)
        buffer.scrollInRegion()
        buffer.getLineAsString(0) shouldBe "Top"
        buffer.getLineAsString(4) shouldBe "Bottom"
    }

    @Test
    fun `scroll region of height 1`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setCursorPosition(0, 2); buffer.writeText("Middle")
        buffer.setScrollRegion(2, 2)
        buffer.scrollInRegion()
        buffer.getLineAsString(2) shouldBe ""
    }

    @Test
    fun `invalid scroll region is ignored`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setScrollRegion(3, 1)  // top > bottom — invalid
        // Should behave as full screen
        buffer.setCursorPosition(0, 0); buffer.writeText("Test")
        buffer.insertLineAtBottom()
        buffer.scrollbackSize shouldBe 1
    }

    @Test
    fun `full-screen scroll region behaves like normal scrolling`() {
        val buffer = TerminalBuffer(width = 10, height = 3, maxScrollbackSize = 100)
        buffer.setScrollRegion(0, 2)
        buffer.setCursorPosition(0, 0); buffer.writeText("Line0")
        buffer.scrollInRegion()
        buffer.scrollbackSize shouldBe 1
        buffer.getScrollbackLineAsString(0) shouldBe "Line0"
    }

    @Test
    fun `LineFeed at scroll bottom triggers scroll`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setScrollRegion(1, 3)
        buffer.setCursorPosition(0, 1); buffer.writeText("R1")
        buffer.setCursorPosition(0, 2); buffer.writeText("R2")
        buffer.setCursorPosition(0, 3); buffer.writeText("R3")
        buffer.applyCommand(TerminalCommand.LineFeed)
        buffer.getLineAsString(1) shouldBe "R2"
        buffer.getLineAsString(2) shouldBe "R3"
        buffer.getLineAsString(3) shouldBe ""
    }

    @Test
    fun `LineFeed below scroll region does not scroll`() {
        val buffer = TerminalBuffer(width = 10, height = 5)
        buffer.setScrollRegion(0, 2)
        buffer.setCursorPosition(0, 0); buffer.writeText("Top")
        buffer.setCursorPosition(0, 4)  // Below scroll region
        buffer.applyCommand(TerminalCommand.LineFeed)
        buffer.getLineAsString(0) shouldBe "Top"  // Region was NOT scrolled
        buffer.getCursorPosition().row shouldBe 4  // Cursor stays (already at bottom)
    }
}
