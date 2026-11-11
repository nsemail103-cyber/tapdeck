package jp.aibou.deck.ws;

import jp.aibou.deck.action.ActionDispatcher;
import jp.aibou.deck.action.AppLauncher;
import jp.aibou.deck.action.KeySender;
import jp.aibou.deck.action.TextTyper;
import jp.aibou.deck.deck.DeckService;
import jp.aibou.deck.deck.DeckStore;
import jp.aibou.deck.deck.SampleDeckFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckSessionServiceTest {

    private static final class RecordingKeySender implements KeySender {
        final List<List<Integer>> chords = new ArrayList<>();

        @Override
        public void chord(List<Integer> keyCodes) {
            chords.add(List.copyOf(keyCodes));
        }
    }

    private RecordingKeySender keySender;
    private DeckService deckService;
    private jp.aibou.deck.auth.AuthService authService;
    private DeckSessionService service;
    private DeckSessionService.SessionState state;
    private List<String> sent;

    @BeforeEach
    void setUp(@TempDir Path dataDir) {
        ObjectMapper mapper = JsonMapper.builder().build();
        keySender = new RecordingKeySender();
        TextTyper noText = (text, restore) -> { };
        AppLauncher noLaunch = new AppLauncher() {
            @Override public void launch(String path, List<String> args) { }
            @Override public void open(String target) { }
        };
        deckService = new DeckService(new DeckStore(mapper, dataDir.toString()), new SampleDeckFactory());
        authService = new jp.aibou.deck.auth.AuthService(mapper,
                new DeckStore(mapper, dataDir.toString()));
        service = new DeckSessionService(mapper,
                new ActionDispatcher(keySender, noText, noLaunch), deckService, authService, "test-token");
        state = new DeckSessionService.SessionState();
        sent = new ArrayList<>();
    }

    private void handle(String json) {
        service.handle(state, json, sent::add);
    }

    private void auth() {
        handle("{\"type\":\"auth\",\"token\":\"test-token\"}");
        sent.clear();
    }

    @Test
    void wrongTokenIsRejected() {
        handle("{\"type\":\"auth\",\"token\":\"nope\"}");
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("\"authFailed\""));
        assertFalse(state.authenticated());
    }

    @Test
    void correctTokenReturnsAuthOkAndProfileSync() {
        handle("{\"type\":\"auth\",\"token\":\"test-token\"}");
        assertEquals(2, sent.size());
        assertTrue(sent.get(0).contains("\"authOk\""));
        assertTrue(sent.get(1).contains("\"profileSync\""));
        assertTrue(sent.get(1).contains("key-save"));
        assertTrue(sent.get(1).contains("\"type\":\"hotkey\""));
        assertTrue(state.authenticated());
    }

    @Test
    void keyEventBeforeAuthDoesNotExecute() {
        handle("{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"key-save\",\"phase\":\"down\"}");
        assertTrue(sent.get(0).contains("\"error\""));
        assertTrue(keySender.chords.isEmpty());
    }

    @Test
    void keyEventExecutesBoundHotkey() {
        auth();
        handle("{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"key-save\",\"phase\":\"down\"}");
        assertEquals(List.of(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_S)), keySender.chords);
        assertTrue(sent.get(0).contains("\"actionResult\""));
        assertTrue(sent.get(0).contains("\"success\":true"));
    }

    @Test
    void unknownKeyReportsFailure() {
        auth();
        handle("{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"missing\",\"phase\":\"down\"}");
        assertTrue(sent.get(0).contains("\"success\":false"));
        assertTrue(keySender.chords.isEmpty());
    }

    @Test
    void pingAnswersPong() {
        handle("{\"type\":\"ping\"}");
        assertTrue(sent.get(0).contains("\"pong\""));
    }

    @Test
    void pairingCodeExchangesForSessionToken() {
        String code = authService.createPairingCode();
        handle("{\"type\":\"auth\",\"token\":\"" + code + "\"}");
        assertTrue(sent.get(0).contains("\"authOk\""), sent.get(0));
        assertTrue(sent.get(0).contains("\"sessionToken\""), sent.get(0));
        String sessionToken = sent.get(0).split("\"sessionToken\":\"")[1].split("\"")[0];

        // The same pairing code must not work twice.
        DeckSessionService.SessionState second = new DeckSessionService.SessionState();
        List<String> sent2 = new ArrayList<>();
        service.handle(second, "{\"type\":\"auth\",\"token\":\"" + code + "\"}", sent2::add);
        assertTrue(sent2.get(0).contains("\"authFailed\""), sent2.get(0));

        // The issued session token authenticates a fresh session.
        List<String> sent3 = new ArrayList<>();
        service.handle(new DeckSessionService.SessionState(),
                "{\"type\":\"auth\",\"token\":\"" + sessionToken + "\"}", sent3::add);
        assertTrue(sent3.get(0).contains("\"authOk\""), sent3.get(0));
    }

    @Test
    void deckUpdatesArePickedUpImmediately() {
        auth();
        // Remove every key by swapping in an empty deck: the same keyEvent
        // that worked before must now report "key not found".
        deckService.updateDeck(new jp.aibou.deck.model.Deck(2, List.of()));
        handle("{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"key-save\",\"phase\":\"down\"}");
        assertTrue(sent.get(0).contains("\"success\":false"));
        assertTrue(keySender.chords.isEmpty());
    }
}
