package jp.aibou.deck.action;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

/** TextTyper backed by the system clipboard and a Ctrl+V chord. */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "robot", matchIfMissing = true)
public class RobotTextTyper implements TextTyper {

    private final KeySender keySender;

    public RobotTextTyper(KeySender keySender) {
        this.keySender = keySender;
    }

    @Override
    public void type(String text, boolean restoreClipboard) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String previous = restoreClipboard ? readText(clipboard) : null;
        clipboard.setContents(new StringSelection(text), null);
        keySender.chord(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_V));
        if (restoreClipboard && previous != null) {
            // Give the target application a moment to consume the paste.
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            clipboard.setContents(new StringSelection(previous), null);
        }
    }

    private String readText(Clipboard clipboard) {
        try {
            Object data = clipboard.getData(DataFlavor.stringFlavor);
            return data instanceof String s ? s : null;
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            return null;
        }
    }
}
