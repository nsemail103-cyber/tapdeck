package jp.aibou.deck.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the deck client at /deck and the editor at /editor. */
@Controller
public class DeckPageController {

    @GetMapping({"/deck", "/deck/"})
    public String deckPage() {
        return "forward:/deck/index.html";
    }

    @GetMapping({"/editor", "/editor/"})
    public String editorPage() {
        return "forward:/editor/index.html";
    }

    @GetMapping({"/pair", "/pair/"})
    public String pairPage() {
        return "forward:/pair/index.html";
    }
}
