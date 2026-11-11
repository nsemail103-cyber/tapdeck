package jp.aibou.deck.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * An action bound to a key. Discriminated by the JSON {@code type} field.
 * New action types are added as records plus one JsonSubTypes entry.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Action.Hotkey.class, name = "hotkey"),
        @JsonSubTypes.Type(value = Action.Text.class, name = "text"),
        @JsonSubTypes.Type(value = Action.Launch.class, name = "launch"),
        @JsonSubTypes.Type(value = Action.Open.class, name = "open"),
        @JsonSubTypes.Type(value = Action.Media.class, name = "media"),
        @JsonSubTypes.Type(value = Action.Delay.class, name = "delay"),
        @JsonSubTypes.Type(value = Action.Multi.class, name = "multi")
})
public sealed interface Action {

    /** Send a keyboard shortcut: modifiers in order, then the main key. */
    record Hotkey(List<String> modifiers, String key) implements Action {
        public Hotkey {
            modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        }
    }

    /** Type a string by pasting it through the clipboard. */
    record Text(String text, Boolean restoreClipboard) implements Action {
        public boolean restore() {
            return restoreClipboard == null || restoreClipboard;
        }
    }

    /** Launch an executable with optional arguments. */
    record Launch(String path, List<String> args) implements Action {
        public Launch {
            args = args == null ? List.of() : List.copyOf(args);
        }
    }

    /** Open a URL in the default browser, or a file/folder with its association. */
    record Open(String target) implements Action {
    }

    /** Send a media key. Command: PLAY_PAUSE, NEXT, PREV, STOP, VOL_UP, VOL_DOWN, MUTE. */
    record Media(String command) implements Action {
    }

    /** Wait. Only meaningful as a step inside a Multi action. */
    record Delay(long millis) implements Action {
    }

    /** Run child actions in order; a failing step does not stop the rest. */
    record Multi(List<Action> steps) implements Action {
        public Multi {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
    }
}
