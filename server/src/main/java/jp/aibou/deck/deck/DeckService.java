package jp.aibou.deck.deck;

import jp.aibou.deck.model.Deck;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Holds the current deck definition. Updates are persisted through the
 * store and pushed to registered listeners (connected clients re-sync).
 */
@Component
public class DeckService {

    private final DeckStore store;
    private final AtomicReference<Deck> current;
    private final List<Consumer<Deck>> listeners = new CopyOnWriteArrayList<>();

    public DeckService(DeckStore store, SampleDeckFactory sampleFactory) {
        this.store = store;
        this.current = new AtomicReference<>(store.loadOrSeed(sampleFactory::sampleDeck));
    }

    public Deck getDeck() {
        return current.get();
    }

    public synchronized void updateDeck(Deck deck) {
        store.save(deck);
        current.set(deck);
        for (Consumer<Deck> listener : listeners) {
            listener.accept(deck);
        }
    }

    public void addListener(Consumer<Deck> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Deck> listener) {
        listeners.remove(listener);
    }
}
