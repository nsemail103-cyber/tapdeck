package jp.aibou.deck.action;

import java.util.List;

/**
 * Sends a chord of keys to the OS: presses the codes in list order and
 * releases them in reverse order. Implementations hide the OS dependency
 * (java.awt.Robot on the user's PC, a recording mock in tests).
 */
public interface KeySender {

    void chord(List<Integer> keyCodes);
}
