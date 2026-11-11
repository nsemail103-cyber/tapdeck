package jp.aibou.deck.web;

import jakarta.servlet.http.HttpServletRequest;
import jp.aibou.deck.deck.DeckService;
import jp.aibou.deck.deck.DeckStore;
import jp.aibou.deck.deck.DeckValidator;
import jp.aibou.deck.model.Deck;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Editor API. Every endpoint is restricted to requests from the machine
 * running the server: the API exists so the owner can edit their own
 * deck from the PC, and it can change what the PC executes.
 */
@RestController
public class EditorController {

    private final DeckService deckService;
    private final DeckStore deckStore;

    public EditorController(DeckService deckService, DeckStore deckStore) {
        this.deckService = deckService;
        this.deckStore = deckStore;
    }

    static void requireLocal(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!("127.0.0.1".equals(remote) || "::1".equals(remote)
                || "0:0:0:0:0:0:0:1".equals(remote))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "editor API is only available from this PC");
        }
    }

    @GetMapping("/api/deck")
    public Deck getDeck(HttpServletRequest request) {
        requireLocal(request);
        return deckService.getDeck();
    }

    @PutMapping("/api/deck")
    public Deck putDeck(@RequestBody Deck deck, HttpServletRequest request) {
        requireLocal(request);
        return applyUpdate(deck);
    }

    /** Import is a deck replace too; the store keeps the previous version as .bak. */
    @PostMapping("/api/import")
    public Deck importDeck(@RequestBody Deck deck, HttpServletRequest request) {
        requireLocal(request);
        return applyUpdate(deck);
    }

    private Deck applyUpdate(Deck deck) {
        List<String> problems = DeckValidator.validate(deck);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", problems));
        }
        deckService.updateDeck(deck);
        return deckService.getDeck();
    }

    @GetMapping("/api/export")
    public ResponseEntity<Deck> exportDeck(HttpServletRequest request) {
        requireLocal(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deck.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(deckService.getDeck());
    }

    @PostMapping("/api/icons")
    public Map<String, String> uploadIcon(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        requireLocal(request);
        String original = file.getOriginalFilename() == null ? "icon" : file.getOriginalFilename();
        String safe = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String name = UUID.randomUUID().toString().substring(0, 8) + "-" + safe;
        Path dir = deckStore.dataDir().resolve("icons");
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to store icon", e);
        }
        return Map.of("path", "icons/" + name);
    }

    @GetMapping("/icons/{name}")
    public ResponseEntity<Resource> icon(@PathVariable String name) {
        if (!name.matches("[A-Za-z0-9._-]+") || name.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid icon name");
        }
        Path path = deckStore.dataDir().resolve("icons").resolve(name);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "icon not found");
        }
        MediaType type = name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")
                ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(path));
    }
}
