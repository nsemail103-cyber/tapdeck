package jp.aibou.deck.ws;

import jp.aibou.deck.action.KeySender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boot smoke over a real socket: connect, authenticate, receive the deck,
 * fire a key event and confirm the action reached the (mock) key sender.
 * Disabled by default because the build sandbox blocks raw TCP from the
 * test JVM; run with -Ddeck.smoke.socket=true where sockets are allowed
 * (for example on the user's PC). The same flow is covered in-process by
 * DeckHandlerIntegrationTest.
 */
@EnabledIfSystemProperty(named = "deck.smoke.socket", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.deck.executor=mock",
        "app.deck.token=smoke-token"
})
class WebSocketLiveSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private KeySender keySender;

    @Test
    void fullFlowOverRealSocket() throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                inbox.add(message.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create("ws://127.0.0.1:" + port + "/ws"))
                .get(10, TimeUnit.SECONDS);

        try {
            session.sendMessage(new TextMessage("{\"type\":\"auth\",\"token\":\"smoke-token\"}"));
            String authOk = pollFor(inbox, "\"authOk\"");
            assertNotNull(authOk);
            String sync = pollFor(inbox, "\"profileSync\"");
            assertNotNull(sync);
            assertTrue(sync.contains("key-save"));

            session.sendMessage(new TextMessage(
                    "{\"type\":\"keyEvent\",\"profileId\":\"prof-default\",\"pageId\":\"page-main\",\"keyId\":\"key-save\",\"phase\":\"down\"}"));
            String result = pollFor(inbox, "\"actionResult\"");
            assertNotNull(result);
            assertTrue(result.contains("\"success\":true"));

            jp.aibou.deck.action.MockKeySender mock = (jp.aibou.deck.action.MockKeySender) keySender;
            assertEquals(List.of(List.of(KeyEvent.VK_CONTROL, KeyEvent.VK_S)), mock.sent());
        } finally {
            session.close();
        }
    }

    private String pollFor(BlockingQueue<String> inbox, String needle) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            String msg = inbox.poll(500, TimeUnit.MILLISECONDS);
            if (msg != null && msg.contains(needle)) {
                return msg;
            }
        }
        return null;
    }
}
