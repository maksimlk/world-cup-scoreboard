package io.github.maksimlk.worldcup.scoreboard.exception;

import java.io.Serial;
import java.util.List;

/**
 * Thrown after an event was delivered to every listener and at least one of them failed.
 *
 * <p>The change that caused the event has already been applied. The cause is the first listener's
 * exception; any further failures are attached as suppressed exceptions. This is not a
 * {@link ScoreboardException}, as no scoreboard rule was broken.
 */
public final class ListenerFailedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** @param failures the listeners' exceptions in delivery order; must not be empty */
    public ListenerFailedException(List<RuntimeException> failures) {
        super(failures.size() + " scoreboard listener(s) failed", failures.getFirst());
        failures.subList(1, failures.size()).forEach(this::addSuppressed);
    }
}
