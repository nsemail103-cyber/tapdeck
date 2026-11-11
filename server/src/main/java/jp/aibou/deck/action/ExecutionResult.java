package jp.aibou.deck.action;

/** Outcome of executing one action, reported back to the client. */
public record ExecutionResult(boolean success, String message) {

    public static ExecutionResult ok(String message) {
        return new ExecutionResult(true, message);
    }

    public static ExecutionResult failed(String message) {
        return new ExecutionResult(false, message);
    }
}
