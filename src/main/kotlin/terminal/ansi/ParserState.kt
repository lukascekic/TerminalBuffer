package terminal.ansi

/**
 * Internal states of the [VT100Parser] state machine.
 *
 * Transitions follow the ECMA-48 / XTerm VT100 parsing model:
 *
 * - [GROUND] — normal text output; ESC transitions to [ESCAPE].
 * - [ESCAPE] — ESC received; `[` → [CSI_ENTRY], `]` → [OSC_STRING], intermediates → [ESCAPE_INTERMEDIATE].
 * - [ESCAPE_INTERMEDIATE] — collecting intermediate bytes of a two-byte ESC sequence.
 * - [CSI_ENTRY] — `ESC [` received; first byte determines parameter or final byte.
 * - [CSI_PARAM] — collecting numeric parameters separated by `;`.
 * - [CSI_INTERMEDIATE] — collecting intermediate bytes within a CSI sequence.
 * - [OSC_STRING] — inside an Operating System Command; terminated by BEL or ESC `\`.
 */
enum class ParserState {
    GROUND,
    ESCAPE,
    ESCAPE_INTERMEDIATE,
    CSI_ENTRY,
    CSI_PARAM,
    CSI_INTERMEDIATE,
    OSC_STRING
}
