package io.github.maksimlk.worldcup.scoreboard.exception;

import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.Side;
import java.io.Serial;
import java.util.Locale;

/** Thrown when cancelling a goal for a side whose score is already zero. */
public final class NoGoalToCancelException extends ScoreboardException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoGoalToCancelException(MatchId matchId, Side side) {
        super("No " + side.name().toLowerCase(Locale.ROOT) + " goal to cancel in match " + matchId.value());
    }
}
