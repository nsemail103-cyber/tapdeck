package jp.aibou.deck.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** TextTyper that only records. For tests and headless runs. */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "mock")
public class MockTextTyper implements TextTyper {

    private static final Logger log = LoggerFactory.getLogger(MockTextTyper.class);

    public record Call(String text, boolean restoreClipboard) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();

    @Override
    public void type(String text, boolean restoreClipboard) {
        calls.add(new Call(text, restoreClipboard));
        log.info("MOCK text typed: {} (restore={})", text, restoreClipboard);
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public void clear() {
        calls.clear();
    }
}
