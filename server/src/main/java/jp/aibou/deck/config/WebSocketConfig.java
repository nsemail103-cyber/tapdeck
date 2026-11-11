package jp.aibou.deck.config;

import jp.aibou.deck.ws.DeckWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeckWebSocketHandler handler;

    public WebSocketConfig(DeckWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // LAN-only personal tool; the token check in the protocol is the gate.
        registry.addHandler(handler, "/ws").setAllowedOrigins("*");
    }
}
