package jp.aibou.deck.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessAppLauncherTest {

    @Test
    void expandsKnownEnvironmentVariable() {
        String home = System.getenv("HOME");
        if (home == null) {
            return; // environment without HOME; nothing to assert against
        }
        assertEquals(home + "/apps/Code.exe",
                ProcessAppLauncher.expand("%HOME%/apps/Code.exe"));
    }

    @Test
    void leavesUnknownVariableAndPlainTextUntouched() {
        assertEquals("%NO_SUCH_VAR_XYZ%\\app.exe",
                ProcessAppLauncher.expand("%NO_SUCH_VAR_XYZ%\\app.exe"));
        assertEquals("C:\\plain\\path.exe",
                ProcessAppLauncher.expand("C:\\plain\\path.exe"));
        assertEquals("100% sure", ProcessAppLauncher.expand("100% sure"));
    }
}
