package jp.aibou.deck.auth;

import jp.aibou.deck.deck.DeckStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    private AuthService auth(Path dir) {
        return new AuthService(mapper, new DeckStore(mapper, dir.toString()));
    }

    private AuthService auth(Path dir, Clock clock) {
        return new AuthService(mapper, new DeckStore(mapper, dir.toString()), clock);
    }

    @Test
    void issuedTokenVerifiesAndWrongTokenFails(@TempDir Path dir) {
        AuthService auth = auth(dir);
        String token = auth.issueToken("test");
        assertTrue(auth.verify(token));
        assertFalse(auth.verify(token + "x"));
        assertFalse(auth.verify(""));
    }

    @Test
    void onlyTheHashIsStored(@TempDir Path dir) throws Exception {
        AuthService auth = auth(dir);
        String token = auth.issueToken("test");
        String stored = Files.readString(dir.resolve("auth.json"));
        assertFalse(stored.contains(token), "plaintext token must never be persisted");
        assertTrue(stored.contains(AuthService.sha256Hex(token)));
    }

    @Test
    void tokensSurviveRestart(@TempDir Path dir) {
        String token = auth(dir).issueToken("test");
        assertTrue(auth(dir).verify(token));
    }

    @Test
    void pairingCodeIsOneTime(@TempDir Path dir) {
        AuthService auth = auth(dir);
        String code = auth.createPairingCode();
        Optional<String> session = auth.consumePairingCode(code);
        assertTrue(session.isPresent());
        assertTrue(auth.verify(session.get()));
        assertTrue(auth.consumePairingCode(code).isEmpty());
    }

    @Test
    void pairingCodeExpires(@TempDir Path dir) {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-11T12:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        AuthService auth = auth(dir, clock);
        String code = auth.createPairingCode();
        now.set(Instant.parse("2026-11-11T12:06:00Z")); // 6 minutes later
        assertTrue(auth.consumePairingCode(code).isEmpty());
    }

    @Test
    void validPairingCodeIsReusedAcrossRequests(@TempDir Path dir) {
        AuthService auth = auth(dir);
        String first = auth.createPairingCode();
        // Reloading the /pair page must not invalidate the shown QR code.
        assertEquals(first, auth.createPairingCode());
        assertTrue(auth.consumePairingCode(first).isPresent());
        // After consumption, a fresh code is issued.
        String next = auth.createPairingCode();
        assertFalse(next.equals(first));
    }

    @Test
    void forcedPairingCodeInvalidatesPrevious(@TempDir Path dir) {
        AuthService auth = auth(dir);
        String first = auth.createPairingCode();
        auth.createPairingCode(true);
        assertTrue(auth.consumePairingCode(first).isEmpty());
    }
}
