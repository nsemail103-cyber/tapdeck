package jp.aibou.deck.action;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyCodesTest {

    @Test
    void resolvesModifiersAndKeys() {
        assertEquals(KeyEvent.VK_CONTROL, KeyCodes.of("CTRL"));
        assertEquals(KeyEvent.VK_CONTROL, KeyCodes.of("ctrl"));
        assertEquals(KeyEvent.VK_ALT, KeyCodes.of("ALT"));
        assertEquals(KeyEvent.VK_SHIFT, KeyCodes.of("SHIFT"));
        assertEquals(KeyEvent.VK_WINDOWS, KeyCodes.of("WIN"));
        assertEquals(KeyEvent.VK_S, KeyCodes.of("S"));
        assertEquals(KeyEvent.VK_F5, KeyCodes.of("F5"));
        assertEquals(KeyEvent.VK_ENTER, KeyCodes.of("ENTER"));
        assertEquals(KeyEvent.VK_ESCAPE, KeyCodes.of("ESC"));
    }

    @Test
    void resolvesMediaKeys() {
        // Windows virtual-key values; java.awt defines no constants for them.
        assertEquals(0xB3, KeyCodes.of("MEDIA_PLAY_PAUSE"));
        assertEquals(0xAF, KeyCodes.of("VOLUME_UP"));
    }

    @Test
    void rejectsUnknownNames() {
        assertThrows(IllegalArgumentException.class, () -> KeyCodes.of("NOPE"));
        assertThrows(IllegalArgumentException.class, () -> KeyCodes.of(null));
    }
}
