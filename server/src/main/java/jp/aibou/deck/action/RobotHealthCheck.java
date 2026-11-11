package jp.aibou.deck.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;

/**
 * At startup in robot mode, prove that a Robot can actually be created
 * in this session and log the verdict loudly. Desktop automation is the
 * server's core function; failing silently here would surface later as
 * a vague per-key error on the phone.
 */
@Component
@ConditionalOnProperty(name = "app.deck.executor", havingValue = "robot", matchIfMissing = true)
public class RobotHealthCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RobotHealthCheck.class);

    @Override
    public void run(ApplicationArguments args) {
        boolean headless = GraphicsEnvironment.isHeadless();
        try {
            new Robot();
            log.info("Robot check: OK (java.awt.headless={}, isHeadless={})",
                    System.getProperty("java.awt.headless"), headless);
        } catch (Throwable e) {
            log.warn("Robot check: FAILED (java.awt.headless={}, isHeadless={}) - "
                    + "key and text actions will not work in this session", 
                    System.getProperty("java.awt.headless"), headless, e);
        }
    }
}
