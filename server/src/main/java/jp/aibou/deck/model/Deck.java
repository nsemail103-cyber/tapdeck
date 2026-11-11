package jp.aibou.deck.model;

import java.util.List;
import java.util.Optional;

/** Root of the deck definition. The server is the single source of truth. */
public record Deck(int version, List<Profile> profiles) {

    public Deck {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }

    public Optional<KeyDef> findKey(String profileId, String pageId, String keyId) {
        return profiles.stream()
                .filter(p -> p.id().equals(profileId))
                .flatMap(p -> p.pages().stream())
                .filter(pg -> pg.id().equals(pageId))
                .flatMap(pg -> pg.keys().stream())
                .filter(k -> k.id().equals(keyId))
                .findFirst();
    }
}
