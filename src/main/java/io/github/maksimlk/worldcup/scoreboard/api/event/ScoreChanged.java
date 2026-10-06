package io.github.maksimlk.worldcup.scoreboard.api.event;

import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.api.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.api.Side;
import java.util.Objects;

/**
 * A score event was applied to a live match.
 *
 * @param match  the match with its new score
 * @param side   the side the event applied to
 * @param change what happened
 */
public record ScoreChanged(MatchSnapshot match, Side side, ScoreChange change) implements ScoreboardEvent {

    public ScoreChanged {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(change, "change");
    }
}
