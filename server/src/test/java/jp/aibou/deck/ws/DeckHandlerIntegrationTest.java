package jp.aibou.deck.ws;

import jp.aibou.deck.action.MockKeySender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end protocol flow through the real handler, service and
 * dispatcher beans, using a stub session (no socket). Complements
 * WebSocketLiveSmokeTest, which repeats this over a real socket where
 * the environment allows it.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "app.deck.executor=mock",
        "app.deck.token=it-token"
})
class DeckHandlerIntegrationTest {

    @Autowired
    private DeckWebSocketHandler handler;

    @Autowired
    private MockKeySender mockKeySender;

    @Autowired
    private jp.aibou.deck.deck.DeckService deckService;

    @Autowired
    private jp.aibou.deck.deck.SampleDeckFactory sampleDeckFactory;

    @Test
    void authSyncAndKeyEventFlow() throws Exception {
        StubSession session = new StubSession();
        handler.afterConnectionEstablished(session);

        handler.handleMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"it-token\"}"));
        assertEquals(2, session.sent.size());
        assertTrue(session.sent.get(0).contains("\"authOk\""));
        assertTrue(session.sent.get(1).contains("\"profileSync\""));
        assertTrue(session.sent.get(1).contains("key-save"));

        handler.handleMessage(session, new TextMessage(
                "{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"key-save\",\"phase\":\"down\"}"));
        assertEquals(3, session.sent.size());
        assertTrue(session.sent.get(2).contains("\"actionResult\""));
        assertTrue(session.sent.get(2).contains("\"success\":true"));
        assertEquals(List.of(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_S)), mockKeySender.sent());

        // A deck change is pushed to the authenticated session as a fresh sync.
        deckService.updateDeck(sampleDeckFactory.sampleDeck());
        assertEquals(4, session.sent.size());
        assertTrue(session.sent.get(3).contains("\"profileSync\""));
    }

    private static final class StubSession implements WebSocketSession {
        final List<String> sent = new ArrayList<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean open = true;

        @Override public String getId() { return "stub"; }
        @Override public URI getUri() { return URI.create("ws://localhost/ws"); }
        @Override public HttpHeaders getHandshakeHeaders() { return new HttpHeaders(); }
        @Override public Map<String, Object> getAttributes() { return attributes; }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return new InetSocketAddress(8081); }
        @Override public InetSocketAddress getRemoteAddress() { return new InetSocketAddress(50000); }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int limit) { }
        @Override public int getTextMessageSizeLimit() { return 65536; }
        @Override public void setBinaryMessageSizeLimit(int limit) { }
        @Override public int getBinaryMessageSizeLimit() { return 65536; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
            sent.add(message.getPayload().toString());
        }

        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; }
        @Override public void close(CloseStatus status) { open = false; }
    }
}
