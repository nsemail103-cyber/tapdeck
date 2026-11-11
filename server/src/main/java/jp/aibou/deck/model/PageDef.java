package jp.aibou.deck.model;

import java.util.List;

/** One grid of keys inside a profile. */
public record PageDef(String id, String name, int rows, int cols, List<KeyDef> keys) {

    public PageDef {
        keys = keys == null ? List.of() : List.copyOf(keys);
    }
}
