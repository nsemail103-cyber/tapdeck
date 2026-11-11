package jp.aibou.deck.action;

import jp.aibou.deck.model.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Executes an action definition by delegating to the OS-facing ports
 * (KeySender, TextTyper, AppLauncher), which keeps this class testable.
 */
@Component
public class ActionDispatcher {

    private static final Map<String, String> MEDIA_KEYS = Map.of(
            "PLAY_PAUSE", "MEDIA_PLAY_PAUSE",
            "NEXT", "MEDIA_NEXT",
            "PREV", "MEDIA_PREV",
            "STOP", "MEDIA_STOP",
            "VOL_UP", "VOLUME_UP",
            "VOL_DOWN", "VOLUME_DOWN",
            "MUTE", "VOLUME_MUTE"
    );

    private static final Logger log = LoggerFactory.getLogger(ActionDispatcher.class);

    private final KeySender keySender;
    private final TextTyper textTyper;
    private final AppLauncher appLauncher;

    public ActionDispatcher(KeySender keySender, TextTyper textTyper, AppLauncher appLauncher) {
        this.keySender = keySender;
        this.textTyper = textTyper;
        this.appLauncher = appLauncher;
    }

    public ExecutionResult execute(Action action) {
        try {
            return switch (action) {
                case Action.Hotkey hotkey -> executeHotkey(hotkey);
                case Action.Text text -> {
                    textTyper.type(text.text(), text.restore());
                    yield ExecutionResult.ok("text pasted (" + text.text().length() + " chars)");
                }
                case Action.Launch launch -> {
                    appLauncher.launch(launch.path(), launch.args());
                    yield ExecutionResult.ok("launched: " + launch.path());
                }
                case Action.Open open -> {
                    appLauncher.open(open.target());
                    yield ExecutionResult.ok("opened: " + open.target());
                }
                case Action.Media media -> executeMedia(media);
                case Action.Delay delay -> executeDelay(delay);
                case Action.Multi multi -> executeMulti(multi);
            };
        } catch (IllegalArgumentException e) {
            return ExecutionResult.failed(e.getMessage());
        } catch (RuntimeException e) {
            log.warn("action execution failed: {}", action, e);
            return ExecutionResult.failed("execution error: " + describe(e));
        }
    }

    /** Message plus the deepest cause, so the phone toast shows the real reason. */
    private static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder(String.valueOf(e.getMessage()));
        Throwable cause = e.getCause();
        while (cause != null) {
            sb.append(" <- ").append(cause.getClass().getSimpleName())
                    .append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return sb.toString();
    }

    private ExecutionResult executeHotkey(Action.Hotkey hotkey) {
        List<Integer> codes = new ArrayList<>();
        for (String modifier : hotkey.modifiers()) {
            codes.add(KeyCodes.of(modifier));
        }
        codes.add(KeyCodes.of(hotkey.key()));
        keySender.chord(codes);
        return ExecutionResult.ok("hotkey sent: " + String.join("+",
                codes.stream().map(String::valueOf).toList()));
    }

    private ExecutionResult executeMedia(Action.Media media) {
        String command = media.command() == null ? "" : media.command().trim().toUpperCase(Locale.ROOT);
        String keyName = MEDIA_KEYS.get(command);
        if (keyName == null) {
            return ExecutionResult.failed("unknown media command: " + media.command());
        }
        keySender.chord(List.of(KeyCodes.of(keyName)));
        return ExecutionResult.ok("media sent: " + command);
    }

    private ExecutionResult executeDelay(Action.Delay delay) {
        if (delay.millis() > 0) {
            try {
                Thread.sleep(delay.millis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ExecutionResult.failed("delay interrupted");
            }
        }
        return ExecutionResult.ok("delayed " + delay.millis() + "ms");
    }

    private ExecutionResult executeMulti(Action.Multi multi) {
        int failed = 0;
        String firstFailure = null;
        for (Action step : multi.steps()) {
            ExecutionResult stepResult = execute(step);
            if (!stepResult.success()) {
                failed++;
                if (firstFailure == null) {
                    firstFailure = stepResult.message();
                }
            }
        }
        int total = multi.steps().size();
        if (failed == 0) {
            return ExecutionResult.ok("multi done: " + total + " steps");
        }
        return ExecutionResult.failed(
                "multi: " + failed + "/" + total + " steps failed (first: " + firstFailure + ")");
    }
}
