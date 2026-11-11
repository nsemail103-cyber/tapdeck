package jp.aibou.deck.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeckPageControllerTest {

    @Test
    void deckPathForwardsToClientPage() {
        assertEquals("forward:/deck/index.html", new DeckPageController().deckPage());
    }
}
