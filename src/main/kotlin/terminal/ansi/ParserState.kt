package terminal.ansi

enum class ParserState {
    GROUND,
    ESCAPE,
    ESCAPE_INTERMEDIATE,
    CSI_ENTRY,
    CSI_PARAM,
    CSI_INTERMEDIATE,
    OSC_STRING
}
