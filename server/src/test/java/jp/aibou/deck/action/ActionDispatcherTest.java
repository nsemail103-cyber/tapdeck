package jp.aibou.deck.action;

import jp.aibou.deck.model.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionDispatcherTest {

    private static final class FakeKeySender implements KeySender {
        final List<List<Integer>> chords = new ArrayList<>();

        @Override
        public void chord(List<Integer> keyCodes) {
            chords.add(List.copyOf(keyCodes));
        }
    }

    private static final class FakeTextTyper implements TextTyper {
        final List<String> texts = new ArrayList<>();

        @Override
        public void type(String text, boolean restoreClipboard) {
            texts.add(text + "|restore=" + restoreClipboard);
        }
    }

    private static final class FakeAppLauncher implements AppLauncher {
        final List<String> calls = new ArrayList<>();

        @Override
        public void launch(String path, List<String> args) {
            calls.add("launch:" + path + ":" + args);
        }

        @Override
        public void open(String target) {
            calls.add("open:" + target);
        }
    }

    private FakeKeySender keySender;
    private FakeTextTyper textTyper;
    private FakeAppLauncher appLauncher;
    private ActionDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        keySender = new FakeKeySender();
        textTyper = new FakeTextTyper();
        appLauncher = new FakeAppLauncher();
        dispatcher = new ActionDispatcher(keySender, textTyper, appLauncher);
    }

    @Test
    void hotkeySendsModifiersThenKey() {
        ExecutionResult result = dispatcher.execute(new Action.Hotkey(List.of("CTRL"), "S"));
        assertTrue(result.success());
        assertEquals(List.of(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_S)), keySender.chords);
    }

    @Test
    void unknownKeyNameFailsWithoutSending() {
        ExecutionResult result = dispatcher.execute(new Action.Hotkey(List.of("CTRL"), "NOPE"));
        assertFalse(result.success());
        assertTrue(keySender.chords.isEmpty());
    }

    @Test
    void textIsTypedWithRestoreDefaultingOn() {
        ExecutionResult result = dispatcher.execute(new Action.Text("お疲れ様です。", null));
        assertTrue(result.success());
        assertEquals(List.of("お疲れ様です。|restore=true"), textTyper.texts);
    }

    @Test
    void launchAndOpenReachTheLauncher() {
        assertTrue(dispatcher.execute(new Action.Launch("notepad.exe", List.of("a.txt"))).success());
        assertTrue(dispatcher.execute(new Action.Open("https://example.com")).success());
        assertEquals(List.of("launch:notepad.exe:[a.txt]", "open:https://example.com"), appLauncher.calls);
    }

    @Test
    void mediaCommandsMapToWindowsVkValues() {
        assertTrue(dispatcher.execute(new Action.Media("PLAY_PAUSE")).success());
        assertTrue(dispatcher.execute(new Action.Media("VOL_UP")).success());
        assertEquals(List.of(List.of(0xB3), List.of(0xAF)), keySender.chords);
    }

    @Test
    void unknownMediaCommandFails() {
        ExecutionResult result = dispatcher.execute(new Action.Media("SHUFFLE"));
        assertFalse(result.success());
        assertTrue(keySender.chords.isEmpty());
    }

    @Test
    void delayActuallyWaits() {
        long start = System.nanoTime();
        ExecutionResult result = dispatcher.execute(new Action.Delay(60));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(result.success());
        assertTrue(elapsedMs >= 50, "elapsed was " + elapsedMs + "ms");
    }

    @Test
    void multiRunsStepsInOrderAndContinuesAfterFailure() {
        Action.Multi multi = new Action.Multi(List.of(
                new Action.Hotkey(List.of("CTRL"), "C"),
                new Action.Hotkey(List.of("CTRL"), "NOPE"),
                new Action.Text("done", null)
        ));
        ExecutionResult result = dispatcher.execute(multi);
        assertFalse(result.success());
        assertTrue(result.message().contains("1/3"), result.message());
        // The failing middle step did not stop the final text step.
        assertEquals(List.of(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_C)), keySender.chords);
        assertEquals(List.of("done|restore=true"), textTyper.texts);
    }

    @Test
    void multiSucceedsWhenAllStepsSucceed() {
        Action.Multi multi = new Action.Multi(List.of(
                new Action.Launch("notepad.exe", List.of()),
                new Action.Delay(1),
                new Action.Hotkey(List.of("CTRL"), "S")
        ));
        ExecutionResult result = dispatcher.execute(multi);
        assertTrue(result.success());
        assertTrue(result.message().contains("3 steps"), result.message());
    }
}
