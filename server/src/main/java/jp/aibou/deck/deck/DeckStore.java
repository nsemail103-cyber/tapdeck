package jp.aibou.deck.deck;

import jp.aibou.deck.model.Deck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/**
 * Persists the deck definition as JSON: data/deck.json, keeping the
 * previous generation as deck.json.bak. If the main file is unreadable
 * the backup is used; if both are unusable the seed deck is saved.
 */
@Component
public class DeckStore {

    private static final Logger log = LoggerFactory.getLogger(DeckStore.class);

    private final ObjectMapper mapper;
    private final Path file;
    private final Path backup;

    public DeckStore(ObjectMapper mapper, @Value("${app.deck.data-dir:./data}") String dataDir) {
        this.mapper = mapper;
        Path dir = Path.of(dataDir);
        this.file = dir.resolve("deck.json");
        this.backup = dir.resolve("deck.json.bak");
    }

    public Path file() {
        return file;
    }

    /** Directory that holds deck.json and the icons/ folder. */
    public Path dataDir() {
        return file.getParent();
    }

    public synchronized Deck loadOrSeed(Supplier<Deck> seed) {
        Deck fromMain = tryRead(file);
        if (fromMain != null) {
            return fromMain;
        }
        Deck fromBackup = tryRead(backup);
        if (fromBackup != null) {
            log.warn("deck.json unreadable; recovered from backup {}", backup);
            return fromBackup;
        }
        Deck seeded = seed.get();
        save(seeded);
        log.info("seeded new deck definition at {}", file);
        return seeded;
    }

    public synchronized void save(Deck deck) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("deck.json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), deck);
            if (Files.exists(file)) {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to save deck to " + file, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("failed to save deck to " + file, e);
        }
    }

    private Deck tryRead(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return mapper.readValue(path.toFile(), Deck.class);
        } catch (RuntimeException e) {
            log.warn("could not read deck from {}: {}", path, e.getMessage());
            return null;
        }
    }
}
