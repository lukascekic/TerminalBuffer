package terminal.buffer

/**
 * Terminal colors corresponding to the standard 16-color palette.
 *
 * [DEFAULT] represents the terminal's default foreground or background color (not a specific
 * color value). The 8 standard colors ([BLACK] through [WHITE]) correspond to ANSI SGR 30-37
 * (foreground) and 40-47 (background). The 8 bright variants correspond to SGR 90-97 and 100-107.
 */
enum class Color {
    DEFAULT,
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
    BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
    BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
}
