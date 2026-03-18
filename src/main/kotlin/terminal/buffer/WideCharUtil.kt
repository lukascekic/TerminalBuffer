package terminal.buffer

/**
 * Utility for detecting East Asian wide characters that occupy two terminal columns.
 *
 * Wide characters include CJK ideographs, Hiragana, Katakana, Hangul syllables, and
 * certain fullwidth forms. Detection uses Unicode block membership and specific code point
 * ranges to correctly exclude halfwidth forms within otherwise wide-classified blocks.
 */
object WideCharUtil {

    /**
     * Returns `true` if [char] is a wide character that occupies two terminal columns.
     *
     * Wide characters must be written as a pair of cells: the first cell contains the
     * character with [Cell.width] = 2, and the second is a continuation cell with
     * [Cell.width] = 0.
     */
    fun isWide(char: Char): Boolean {
        val block = Character.UnicodeBlock.of(char) ?: return false
        return block in WIDE_BLOCKS || char.code in WIDE_RANGES
    }

    private val WIDE_BLOCKS = setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_COMPATIBILITY,
        Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
        Character.UnicodeBlock.KANGXI_RADICALS,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
        Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS,
        Character.UnicodeBlock.BOPOMOFO,
        Character.UnicodeBlock.BOPOMOFO_EXTENDED,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
    )

    private val WIDE_RANGES = listOf(
        0x1100..0x115F,   // Hangul Jamo (leading consonants only)
        0x2E80..0x303E,   // CJK misc
        0x3041..0x33BF,   // Hiragana, Katakana, etc.
        0xFE30..0xFE6B,   // CJK compatibility forms
        0xFF01..0xFF60,   // Fullwidth Latin/punctuation (excludes halfwidth Katakana U+FF61-U+FF9F)
        0xFFE0..0xFFE6    // Fullwidth currency signs
    )

    private operator fun List<IntRange>.contains(value: Int): Boolean =
        any { value in it }
}
