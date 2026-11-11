package jp.aibou.deck.ws;

import jp.aibou.deck.deck.DeckService;
import jp.aibou.deck.model.Deck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Thin WebSocket adapter; all protocol logic lives in DeckSessionService.
 * Also pushes a fresh profileSync to authenticated sessions whenever the
 * deck definition changes (editor updates).
 */
@Component
public class DeckWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeckWebSocketHandler.class);
    private static final String STATE_KEY = "deckSessionState";
    private static final String LISTENER_KEY = "deckChangeListener";

    private final DeckSessionService service;
    private final DeckService deckService;

    public DeckWebSocketHandler(DeckSessionService service, DeckService deckService) {
        this.service = service;
        this.deckService = deckService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        DeckSessionService.SessionState state = new DeckSessionService.SessionState();
        session.getAttributes().put(STATE_KEY, state);
        Consumer<Deck> listener = deck -> {
            if (state.authenticated()) {
                send(session, service.profileSyncJson(deck));
            }
        };
        session.getAttributes().put(LISTENER_KEY, listener);
        deckService.addListener(listener);
        log.info("client connected: {}", session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        DeckSessionService.SessionState state =
                (DeckSessionService.SessionState) session.getAttributes().get(STATE_KEY);
        service.handle(state, message.getPayload(), json -> send(session, json));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object listener = session.getAttributes().get(LISTENER_KEY);
        if (listener instanceof Consumer<?> consumer) {
            @SuppressWarnings("unchecked")
            Consumer<Deck> typed = (Consumer<Deck>) consumer;
            deckService.removeListener(typed);
        }
        log.info("client disconnected: {} {}", session.getRemoteAddress(), status);
    }

    private void send(WebSocketSession session, String json) {
        synchronized (session) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("failed to send to {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
