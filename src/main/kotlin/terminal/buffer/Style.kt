package terminal.buffer

/**
 * Text rendering styles that can be applied to terminal cells.
 *
 * Styles are stored as a [Set] in [TextAttributes]. Multiple styles may be active
 * simultaneously. Corresponds to ANSI SGR codes: BOLD=1, ITALIC=3, UNDERLINE=4.
 */
enum class Style {
    BOLD, ITALIC, UNDERLINE
}
