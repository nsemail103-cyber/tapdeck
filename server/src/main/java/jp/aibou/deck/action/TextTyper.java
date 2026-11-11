package jp.aibou.deck.action;

/**
 * Types a string into the focused application by pasting it through the
 * clipboard (the reliable route for Japanese text). When
 * {@code restoreClipboard} is set, the previous clipboard text is put
 * back afterwards.
 */
public interface TextTyper {

    void type(String text, boolean restoreClipboard);
}
