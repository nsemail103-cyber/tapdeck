package jp.aibou.deck.deck;

import jp.aibou.deck.model.Deck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckStoreTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    private DeckStore store(Path dir) {
        return new DeckStore(mapper, dir.toString());
    }

    @Test
    void seedsAndPersistsWhenNoFileExists(@TempDir Path dir) {
        DeckStore store = store(dir);
        Deck sample = new SampleDeckFactory().sampleDeck();

        Deck loaded = store.loadOrSeed(() -> sample);

        assertEquals(sample, loaded);
        assertTrue(Files.exists(dir.resolve("deck.json")));
    }

    @Test
    void saveThenLoadRoundTrips(@TempDir Path dir) {
        DeckStore store = store(dir);
        Deck sample = new SampleDeckFactory().sampleDeck();
        store.save(sample);

        Deck loaded = store.loadOrSeed(() -> {
            throw new AssertionError("seed must not be used when a file exists");
        });

        // Golden check: the full model, including polymorphic actions, survives JSON.
        assertEquals(sample, loaded);
    }

    @Test
    void recoversFromBackupWhenMainFileIsCorrupt(@TempDir Path dir) throws Exception {
        DeckStore store = store(dir);
        Deck first = new SampleDeckFactory().sampleDeck();
        store.save(first);
        Deck second = new Deck(2, first.profiles());
        store.save(second); // now deck.json = second, deck.json.bak = first
        Files.writeString(dir.resolve("deck.json"), "{ this is not json");

        Deck loaded = store.loadOrSeed(() -> {
            throw new AssertionError("seed must not be used when the backup is readable");
        });

        assertEquals(first, loaded);
    }

    @Test
    void reseedsWhenBothFilesAreCorrupt(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("deck.json"), "broken");
        Files.writeString(dir.resolve("deck.json.bak"), "also broken");
        DeckStore store = store(dir);
        Deck sample = new SampleDeckFactory().sampleDeck();

        Deck loaded = store.loadOrSeed(() -> sample);

        assertEquals(sample, loaded);
    }
}
