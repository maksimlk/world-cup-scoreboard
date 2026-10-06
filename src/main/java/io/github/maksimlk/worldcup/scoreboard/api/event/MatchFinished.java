package io.github.maksimlk.worldcup.scoreboard.api.event;

import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import java.util.Objects;

/**
 * A match has finished and left the scoreboard.
 *
 * @param match the match with its final score
 */
public record MatchFinished(MatchSnapshot match) implements ScoreboardEvent {

    public MatchFinished {
        Objects.requireNonNull(match, "match");
    }
}
