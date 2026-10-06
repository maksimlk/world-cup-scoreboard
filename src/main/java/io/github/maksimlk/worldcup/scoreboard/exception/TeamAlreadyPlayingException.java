package io.github.maksimlk.worldcup.scoreboard.exception;

import java.io.Serial;

/** Thrown when starting a match for a team that is already playing in a live match. */
public final class TeamAlreadyPlayingException extends ScoreboardException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeamAlreadyPlayingException(String team) {
        super("Team '" + team + "' is already playing in a live match");
    }
}
