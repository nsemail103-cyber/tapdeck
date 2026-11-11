package jp.aibou.deck.deck;

import jp.aibou.deck.model.Deck;
import jp.aibou.deck.model.KeyDef;
import jp.aibou.deck.model.PageDef;
import jp.aibou.deck.model.Profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural validation for deck definitions received through the
 * editor API. Returns human-readable problems; empty means valid.
 */
public final class DeckValidator {

    private DeckValidator() {
    }

    public static List<String> validate(Deck deck) {
        List<String> problems = new ArrayList<>();
        if (deck == null) {
            problems.add("deck is null");
            return problems;
        }
        if (deck.profiles() == null || deck.profiles().isEmpty()) {
            problems.add("at least one profile is required");
            return problems;
        }
        Set<String> profileIds = new HashSet<>();
        for (Profile profile : deck.profiles()) {
            if (profile.id() == null || profile.id().isBlank()) {
                problems.add("profile id must not be blank");
                continue;
            }
            if (!profileIds.add(profile.id())) {
                problems.add("duplicate profile id: " + profile.id());
            }
            if (profile.pages() == null || profile.pages().isEmpty()) {
                problems.add("profile " + profile.id() + " needs at least one page");
                continue;
            }
            Set<String> pageIds = new HashSet<>();
            for (PageDef page : profile.pages()) {
                validatePage(profile, page, pageIds, problems);
            }
        }
        return problems;
    }

    private static void validatePage(Profile profile, PageDef page,
                                     Set<String> pageIds, List<String> problems) {
        String where = "page " + page.id() + " of profile " + profile.id();
        if (page.id() == null || page.id().isBlank()) {
            problems.add("page id must not be blank (profile " + profile.id() + ")");
            return;
        }
        if (!pageIds.add(page.id())) {
            problems.add("duplicate page id: " + page.id());
        }
        if (page.rows() < 1 || page.rows() > 8 || page.cols() < 1 || page.cols() > 8) {
            problems.add(where + ": rows/cols must be between 1 and 8");
            return;
        }
        Set<String> keyIds = new HashSet<>();
        Set<String> cells = new HashSet<>();
        for (KeyDef key : page.keys() == null ? List.<KeyDef>of() : page.keys()) {
            if (key.id() == null || key.id().isBlank()) {
                problems.add(where + ": key id must not be blank");
                continue;
            }
            if (!keyIds.add(key.id())) {
                problems.add(where + ": duplicate key id: " + key.id());
            }
            if (key.row() < 0 || key.row() >= page.rows() || key.col() < 0 || key.col() >= page.cols()) {
                problems.add(where + ": key " + key.id() + " is outside the grid");
            }
            if (!cells.add(key.row() + ":" + key.col())) {
                problems.add(where + ": two keys share cell " + key.row() + "," + key.col());
            }
            if (key.action() == null) {
                problems.add(where + ": key " + key.id() + " has no action");
            }
        }
    }
}
