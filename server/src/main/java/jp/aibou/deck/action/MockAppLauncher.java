package jp.aibou.deck.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** AppLauncher that only records. For tests and headless runs. */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "mock")
public class MockAppLauncher implements AppLauncher {

    private static final Logger log = LoggerFactory.getLogger(MockAppLauncher.class);

    public record LaunchCall(String path, List<String> args) {
    }

    private final List<LaunchCall> launches = new CopyOnWriteArrayList<>();
    private final List<String> opens = new CopyOnWriteArrayList<>();

    @Override
    public void launch(String path, List<String> args) {
        launches.add(new LaunchCall(path, List.copyOf(args)));
        log.info("MOCK launch: {} {}", path, args);
    }

    @Override
    public void open(String target) {
        opens.add(target);
        log.info("MOCK open: {}", target);
    }

    public List<LaunchCall> launches() {
        return List.copyOf(launches);
    }

    public List<String> opens() {
        return List.copyOf(opens);
    }

    public void clear() {
        launches.clear();
        opens.clear();
    }
}
