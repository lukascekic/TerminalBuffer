package terminal.buffer

object WideCharUtil {

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
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
        Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS,
        Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS,
        Character.UnicodeBlock.BOPOMOFO,
        Character.UnicodeBlock.BOPOMOFO_EXTENDED,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
    )

    private val WIDE_RANGES = listOf(
        0x1100..0x115F,   // Hangul Jamo
        0x2E80..0x303E,   // CJK misc
        0x3041..0x33BF,   // Hiragana, Katakana, etc.
        0xFE30..0xFE6B,   // CJK compatibility forms
        0xFF01..0xFF60,   // Fullwidth forms
        0xFFE0..0xFFE6    // Fullwidth signs
    )

    private operator fun List<IntRange>.contains(value: Int): Boolean =
        any { value in it }
}
