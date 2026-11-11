package jp.aibou.deck.model;

import java.util.List;

/** A named set of pages. {@code appPattern} is reserved for Smart Profiles. */
public record Profile(String id, String name, String appPattern, List<PageDef> pages) {

    public Profile {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
