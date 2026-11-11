package jp.aibou.deck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeckApplication {

    public static void main(String[] args) {
        // The server's whole job is to drive the local desktop (Robot,
        // clipboard). If the launch context made the JVM mis-detect a
        // headless environment, AWT would refuse to create a Robot, so
        // state the intent explicitly before AWT initializes. Mock-mode
        // runs never touch AWT, so this is harmless there.
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(DeckApplication.class, args);
    }
}
