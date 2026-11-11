package jp.aibou.deck.action;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.AWTException;
import java.awt.Robot;
import java.util.List;

/** KeySender backed by java.awt.Robot. Requires a desktop session. */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "robot", matchIfMissing = true)
public class RobotKeySender implements KeySender {

    private volatile Robot robot;

    @Override
    public void chord(List<Integer> keyCodes) {
        Robot r = robot();
        for (int code : keyCodes) {
            r.keyPress(code);
        }
        for (int i = keyCodes.size() - 1; i >= 0; i--) {
            r.keyRelease(keyCodes.get(i));
        }
    }

    private Robot robot() {
        Robot r = robot;
        if (r == null) {
            synchronized (this) {
                if (robot == null) {
                    try {
                        robot = new Robot();
                        robot.setAutoDelay(10);
                    } catch (AWTException e) {
                        throw new IllegalStateException("Robot is unavailable (headless session?)", e);
                    }
                }
                r = robot;
            }
        }
        return r;
    }
}
