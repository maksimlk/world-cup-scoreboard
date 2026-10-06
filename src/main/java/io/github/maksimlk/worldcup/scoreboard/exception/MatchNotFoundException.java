package io.github.maksimlk.worldcup.scoreboard.exception;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import java.io.Serial;

/** Thrown when a match id is unknown to the scoreboard or the match has already finished. */
public final class MatchNotFoundException extends ScoreboardException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MatchNotFoundException(MatchId matchId) {
        super("No live match with id " + matchId.value());
    }
}
