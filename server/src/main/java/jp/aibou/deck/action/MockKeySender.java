package jp.aibou.deck.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** KeySender that only records and logs. For tests and headless runs. */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "mock")
public class MockKeySender implements KeySender {

    private static final Logger log = LoggerFactory.getLogger(MockKeySender.class);

    private final List<List<Integer>> sent = new CopyOnWriteArrayList<>();

    @Override
    public void chord(List<Integer> keyCodes) {
        sent.add(List.copyOf(keyCodes));
        log.info("MOCK chord sent: {}", keyCodes);
    }

    public List<List<Integer>> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
