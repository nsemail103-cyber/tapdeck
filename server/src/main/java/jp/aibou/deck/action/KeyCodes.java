package jp.aibou.deck.action;

import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Map;

/**
 * Maps the names used in deck definitions to java.awt VK codes.
 * Names are case-insensitive. Unknown names raise IllegalArgumentException.
 */
public final class KeyCodes {

    private static final Map<String, Integer> CODES = build();

    private KeyCodes() {
    }

    public static int of(String name) {
        if (name == null) {
            throw new IllegalArgumentException("key name is null");
        }
        Integer code = CODES.get(name.trim().toUpperCase(Locale.ROOT));
        if (code == null) {
            throw new IllegalArgumentException("unknown key name: " + name);
        }
        return code;
    }

    private static Map<String, Integer> build() {
        Map<String, Integer> m = new java.util.HashMap<>();
        // Modifiers (aliases included)
        m.put("CTRL", KeyEvent.VK_CONTROL);
        m.put("CONTROL", KeyEvent.VK_CONTROL);
        m.put("ALT", KeyEvent.VK_ALT);
        m.put("SHIFT", KeyEvent.VK_SHIFT);
        m.put("WIN", KeyEvent.VK_WINDOWS);
        m.put("META", KeyEvent.VK_META);
        // Letters and digits resolve to their VK constants, which equal the ASCII codes.
        for (char c = 'A'; c <= 'Z'; c++) {
            m.put(String.valueOf(c), (int) c);
        }
        for (char c = '0'; c <= '9'; c++) {
            m.put(String.valueOf(c), (int) c);
        }
        // Function keys
        int[] fkeys = {
                KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
                KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
                KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12
        };
        for (int i = 0; i < fkeys.length; i++) {
            m.put("F" + (i + 1), fkeys[i]);
        }
        // Editing and navigation
        m.put("ENTER", KeyEvent.VK_ENTER);
        m.put("ESC", KeyEvent.VK_ESCAPE);
        m.put("ESCAPE", KeyEvent.VK_ESCAPE);
        m.put("TAB", KeyEvent.VK_TAB);
        m.put("SPACE", KeyEvent.VK_SPACE);
        m.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        m.put("DELETE", KeyEvent.VK_DELETE);
        m.put("HOME", KeyEvent.VK_HOME);
        m.put("END", KeyEvent.VK_END);
        m.put("PAGE_UP", KeyEvent.VK_PAGE_UP);
        m.put("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN);
        m.put("UP", KeyEvent.VK_UP);
        m.put("DOWN", KeyEvent.VK_DOWN);
        m.put("LEFT", KeyEvent.VK_LEFT);
        m.put("RIGHT", KeyEvent.VK_RIGHT);
        // Media keys: java.awt has no VK constants for these, so use the
        // Windows virtual-key values, which Robot passes through on Windows.
        // (Used by the media action from Sprint 3, mapped here once.)
        m.put("MEDIA_PLAY_PAUSE", 0xB3);
        m.put("MEDIA_NEXT", 0xB0);
        m.put("MEDIA_PREV", 0xB1);
        m.put("MEDIA_STOP", 0xB2);
        m.put("VOLUME_UP", 0xAF);
        m.put("VOLUME_DOWN", 0xAE);
        m.put("VOLUME_MUTE", 0xAD);
        return Map.copyOf(m);
    }
}
