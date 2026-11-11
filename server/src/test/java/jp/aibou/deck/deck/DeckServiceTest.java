package jp.aibou.deck.deck;

import jp.aibou.deck.model.Deck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeckServiceTest {

    @Test
    void updatePersistsAndNotifiesListeners(@TempDir Path dir) {
        DeckStore store = new DeckStore(JsonMapper.builder().build(), dir.toString());
        DeckService service = new DeckService(store, new SampleDeckFactory());
        List<Deck> notified = new ArrayList<>();
        service.addListener(notified::add);

        Deck updated = new Deck(99, service.getDeck().profiles());
        service.updateDeck(updated);

        assertEquals(updated, service.getDeck());
        assertEquals(List.of(updated), notified);

        // A fresh service over the same store sees the updated deck.
        DeckService reloaded = new DeckService(store, new SampleDeckFactory());
        assertEquals(updated, reloaded.getDeck());
    }
}
