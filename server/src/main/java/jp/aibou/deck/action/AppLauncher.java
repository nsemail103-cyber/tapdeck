package jp.aibou.deck.action;

import java.util.List;

/** Launches applications and opens URLs, files and folders on the PC. */
public interface AppLauncher {

    void launch(String path, List<String> args);

    void open(String target);
}
