package io.github.maksimlk.worldcup.scoreboard.exception;

import java.io.Serial;

/**
 * Base type for violations of scoreboard domain rules, so callers can handle them in one place.
 *
 * <p>Invalid arguments ({@code null}, blank names) are reported with standard Java exceptions instead.
 */
public abstract class ScoreboardException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected ScoreboardException(String message) {
        super(message);
    }
}
