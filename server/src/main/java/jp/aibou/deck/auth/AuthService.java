package jp.aibou.deck.auth;

import jp.aibou.deck.deck.DeckStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Client authentication.
 *
 * Two credential kinds:
 * - long-lived session tokens, issued at pairing time; only their
 *   SHA-256 hashes are stored (data/auth.json), so the file alone
 *   cannot be used to connect.
 * - one-time pairing codes, kept in memory only, valid for 5 minutes;
 *   a successful pairing exchanges the code for a session token.
 */
@Component
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    static final long PAIRING_TTL_SECONDS = 300;

    /** One stored session token (hash only). */
    public record TokenEntry(String id, String label, String hash, String createdAt) {
    }

    /** On-disk shape of auth.json. */
    public record AuthData(List<TokenEntry> tokens) {
        public AuthData {
            tokens = tokens == null ? List.of() : List.copyOf(tokens);
        }
    }

    private final ObjectMapper mapper;
    private final Path authFile;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final List<TokenEntry> tokens = new ArrayList<>();

    // The active pairing code is kept in memory only (never on disk) so
    // the /pair page can re-show the same code while it is still valid.
    private String pairingCode;
    private String pairingHash;
    private Instant pairingExpiresAt;

    @Autowired
    public AuthService(ObjectMapper mapper, DeckStore deckStore) {
        this(mapper, deckStore, Clock.systemDefaultZone());
    }

    AuthService(ObjectMapper mapper, DeckStore deckStore, Clock clock) {
        this.mapper = mapper;
        this.authFile = deckStore.dataDir().resolve("auth.json");
        this.clock = clock;
        load();
    }

    /** Issues a new session token; the plaintext is returned exactly once. */
    public synchronized String issueToken(String label) {
        String token = randomToken(32);
        TokenEntry entry = new TokenEntry(
                randomToken(6), label, sha256Hex(token), clock.instant().toString());
        tokens.add(entry);
        save();
        log.info("issued session token for {}", label);
        return token;
    }

    /** Constant-time check of a session token against the stored hashes. */
    public synchronized boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String hash = sha256Hex(token);
        boolean matched = false;
        for (TokenEntry entry : tokens) {
            matched |= MessageDigest.isEqual(
                    hash.getBytes(StandardCharsets.UTF_8),
                    entry.hash().getBytes(StandardCharsets.UTF_8));
        }
        return matched;
    }

    /**
     * Returns the active pairing code, creating one when none is valid.
     * Reloading the /pair page must not invalidate a QR code that is
     * already on screen, so a still-valid code is reused; pass
     * {@code force} to deliberately replace it.
     */
    public synchronized String createPairingCode() {
        return createPairingCode(false);
    }

    public synchronized String createPairingCode(boolean force) {
        if (!force && pairingCode != null && pairingExpiresAt != null
                && clock.instant().isBefore(pairingExpiresAt)) {
            return pairingCode;
        }
        String code = randomToken(24);
        pairingCode = code;
        pairingHash = sha256Hex(code);
        pairingExpiresAt = clock.instant().plusSeconds(PAIRING_TTL_SECONDS);
        return code;
    }

    /**
     * Exchanges a valid pairing code for a new session token. The code is
     * consumed either way once presented correctly, and expires after
     * PAIRING_TTL_SECONDS.
     */
    public synchronized Optional<String> consumePairingCode(String code) {
        if (code == null || pairingHash == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(pairingExpiresAt)) {
            pairingCode = null;
            pairingHash = null;
            pairingExpiresAt = null;
            return Optional.empty();
        }
        boolean matches = MessageDigest.isEqual(
                sha256Hex(code).getBytes(StandardCharsets.UTF_8),
                pairingHash.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return Optional.empty();
        }
        pairingCode = null;
        pairingHash = null;
        pairingExpiresAt = null;
        return Optional.of(issueToken("paired-device"));
    }

    private String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void load() {
        if (!Files.exists(authFile)) {
            return;
        }
        try {
            AuthData data = mapper.readValue(authFile.toFile(), AuthData.class);
            tokens.addAll(data.tokens());
            log.info("loaded {} session token(s) from {}", tokens.size(), authFile);
        } catch (RuntimeException e) {
            log.warn("could not read {} ({}); starting with no session tokens",
                    authFile, e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(authFile.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(authFile.toFile(), new AuthData(tokens));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to save " + authFile, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("failed to save " + authFile, e);
        }
    }
}
