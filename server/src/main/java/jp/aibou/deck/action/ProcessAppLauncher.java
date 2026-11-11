package jp.aibou.deck.action;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AppLauncher for Windows: ProcessBuilder for executables, and the
 * shell's start command for URLs, files and folders.
 */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "robot", matchIfMissing = true)
public class ProcessAppLauncher implements AppLauncher {

    @Override
    public void launch(String path, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(expand(path));
        for (String arg : args) {
            command.add(expand(arg));
        }
        start(command);
    }

    /**
     * Expands %VAR% references from the process environment (for example
     * %LOCALAPPDATA% in an application path). Unknown variables are left
     * as-is. ProcessBuilder does not do this itself, unlike cmd's start.
     */
    static String expand(String value) {
        if (value == null || !value.contains("%")) {
            return value;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int open = value.indexOf('%', i);
            int close = open < 0 ? -1 : value.indexOf('%', open + 1);
            if (open < 0 || close < 0) {
                out.append(value, i, value.length());
                break;
            }
            out.append(value, i, open);
            String name = value.substring(open + 1, close);
            String replacement = name.isEmpty() ? null : System.getenv(name);
            out.append(replacement != null ? replacement : value.substring(open, close + 1));
            i = close + 1;
        }
        return out.toString();
    }

    @Override
    public void open(String target) {
        // `start` resolves URLs, files and folders through shell associations.
        start(List.of("cmd", "/c", "start", "", target));
    }

    private void start(List<String> command) {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new IllegalStateException("failed to start: " + command.get(0), e);
        }
    }
}
