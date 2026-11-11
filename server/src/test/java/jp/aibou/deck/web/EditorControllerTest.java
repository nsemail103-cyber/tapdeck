package jp.aibou.deck.web;

import jp.aibou.deck.deck.DeckService;
import jp.aibou.deck.model.Action;
import jp.aibou.deck.model.Deck;
import jp.aibou.deck.model.KeyDef;
import jp.aibou.deck.model.PageDef;
import jp.aibou.deck.model.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "app.deck.executor=mock",
        "app.deck.data-dir=${java.io.tmpdir}/deck-editor-test-${random.uuid}"
})
class EditorControllerTest {

    @Autowired
    private EditorController controller;

    @Autowired
    private DeckService deckService;

    private MockHttpServletRequest local() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private MockHttpServletRequest remote() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        return request;
    }

    private Deck minimalDeck(String title) {
        return new Deck(1, List.of(new Profile("p1", "P1", null, List.of(
                new PageDef("pg1", "Main", 2, 2, List.of(
                        new KeyDef("k1", 0, 0, title, KeyDef.Icon.emoji("💾"),
                                new Action.Hotkey(List.of("CTRL"), "S"))))))));
    }

    @Test
    void getAndPutRoundTrip() {
        Deck deck = minimalDeck("保存");
        Deck saved = controller.putDeck(deck, local());
        assertEquals("保存", saved.profiles().get(0).pages().get(0).keys().get(0).title());
        assertEquals(deck, controller.getDeck(local()));
        assertEquals(deck, deckService.getDeck());
    }

    @Test
    void rejectsNonLocalRequests() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.getDeck(remote()));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertThrows(ResponseStatusException.class,
                () -> controller.putDeck(minimalDeck("x"), remote()));
    }

    @Test
    void rejectsInvalidDeck() {
        Deck duplicateCells = new Deck(1, List.of(new Profile("p1", "P1", null, List.of(
                new PageDef("pg1", "Main", 2, 2, List.of(
                        new KeyDef("k1", 0, 0, "a", KeyDef.Icon.emoji("A"),
                                new Action.Hotkey(List.of(), "A")),
                        new KeyDef("k2", 0, 0, "b", KeyDef.Icon.emoji("B"),
                                new Action.Hotkey(List.of(), "B"))))))));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.putDeck(duplicateCells, local()));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertTrue(e.getReason().contains("share cell"), e.getReason());
    }

    @Test
    void iconUploadAndDownload() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my icon.png", "image/png", new byte[]{1, 2, 3});
        Map<String, String> result = controller.uploadIcon(file, local());
        assertTrue(result.get("path").startsWith("icons/"));
        assertTrue(result.get("path").endsWith("my_icon.png"));

        String name = result.get("path").substring("icons/".length());
        assertEquals(HttpStatus.OK, controller.icon(name).getStatusCode());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.icon("../deck.json"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }
}
