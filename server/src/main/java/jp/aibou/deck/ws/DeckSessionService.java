package jp.aibou.deck.ws;

import jp.aibou.deck.action.ActionDispatcher;
import jp.aibou.deck.action.ExecutionResult;
import jp.aibou.deck.auth.AuthService;
import jp.aibou.deck.deck.DeckService;
import jp.aibou.deck.model.Deck;
import jp.aibou.deck.model.KeyDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Protocol logic for one client session, kept free of WebSocket types so
 * it can be unit-tested directly. Message shapes follow DESIGN.md section 3.
 * The deck is read from DeckService on every message, so edits made
 * through the editor are picked up immediately.
 */
@Component
public class DeckSessionService {

    private static final Logger log = LoggerFactory.getLogger(DeckSessionService.class);

    /** Per-connection state. */
    public static final class SessionState {
        private boolean authenticated;

        public boolean authenticated() {
            return authenticated;
        }
    }

    @FunctionalInterface
    public interface Sender {
        void send(String json);
    }

    private final ObjectMapper mapper;
    private final ActionDispatcher dispatcher;
    private final DeckService deckService;
    private final AuthService authService;
    private final String expectedToken;

    public DeckSessionService(ObjectMapper mapper,
                              ActionDispatcher dispatcher,
                              DeckService deckService,
                              AuthService authService,
                              @Value("${app.deck.token}") String expectedToken) {
        this.mapper = mapper;
        this.dispatcher = dispatcher;
        this.deckService = deckService;
        this.authService = authService;
        this.expectedToken = expectedToken;
    }

    /** Serializes a profileSync message for the given deck. */
    public String profileSyncJson(Deck deck) {
        Map<String, Object> sync = message("profileSync");
        sync.put("deck", deck);
        return writeJson(sync);
    }

    public void handle(SessionState state, String payload, Sender sender) {
        JsonNode node;
        try {
            node = mapper.readTree(payload);
        } catch (RuntimeException e) {
            send(sender, message("error", "message", "invalid JSON"));
            return;
        }
        String type = node.path("type").asString("");
        switch (type) {
            case "auth" -> handleAuth(state, node, sender);
            case "ping" -> send(sender, message("pong"));
            case "keyEvent" -> handleKeyEvent(state, node, sender);
            default -> send(sender, message("error", "message", "unknown message type: " + type));
        }
    }

    private void handleAuth(SessionState state, JsonNode node, Sender sender) {
        String token = node.path("token").asString("");
        // A pairing code is exchanged for a long-lived session token.
        Optional<String> issued = authService.consumePairingCode(token);
        if (issued.isPresent()) {
            state.authenticated = true;
            Map<String, Object> ok = message("authOk", "serverVersion", "0.3.0");
            ok.put("sessionToken", issued.get());
            send(sender, ok);
            sender.send(profileSyncJson(deckService.getDeck()));
            return;
        }
        if (expectedToken.equals(token) || authService.verify(token)) {
            state.authenticated = true;
            send(sender, message("authOk", "serverVersion", "0.3.0"));
            sender.send(profileSyncJson(deckService.getDeck()));
        } else {
            send(sender, message("authFailed", "message", "invalid token"));
        }
    }

    private void handleKeyEvent(SessionState state, JsonNode node, Sender sender) {
        if (!state.authenticated) {
            send(sender, message("error", "message", "not authenticated"));
            return;
        }
        String profileId = node.path("profileId").asString("");
        String pageId = node.path("pageId").asString("");
        String keyId = node.path("keyId").asString("");
        String phase = node.path("phase").asString("down");
        if (!"down".equals(phase)) {
            return; // Actions execute on key down only.
        }
        Optional<KeyDef> key = deckService.getDeck().findKey(profileId, pageId, keyId);
        if (key.isEmpty()) {
            send(sender, result(keyId, ExecutionResult.failed("key not found")));
            return;
        }
        ExecutionResult outcome = dispatcher.execute(key.get().action());
        log.info("keyEvent {} -> success={} {}", keyId, outcome.success(), outcome.message());
        send(sender, result(keyId, outcome));
    }

    private Map<String, Object> result(String keyId, ExecutionResult outcome) {
        Map<String, Object> m = message("actionResult");
        m.put("keyId", keyId);
        m.put("success", outcome.success());
        m.put("message", outcome.message());
        return m;
    }

    private Map<String, Object> message(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        return m;
    }

    private Map<String, Object> message(String type, String field, Object value) {
        Map<String, Object> m = message(type);
        m.put(field, value);
        return m;
    }

    private void send(Sender sender, Map<String, Object> message) {
        sender.send(writeJson(message));
    }

    private String writeJson(Map<String, Object> message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (RuntimeException e) {
            log.warn("failed to serialize outbound message: {}", e.getMessage());
            return "{\"type\":\"error\",\"message\":\"serialization failed\"}";
        }
    }
}
