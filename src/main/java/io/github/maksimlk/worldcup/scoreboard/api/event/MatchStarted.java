package io.github.maksimlk.worldcup.scoreboard.api.event;

import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import java.util.Objects;

/**
 * A match has started.
 *
 * @param match the new match, at 0-0
 */
public record MatchStarted(MatchSnapshot match) implements ScoreboardEvent {

    public MatchStarted {
        Objects.requireNonNull(match, "match");
    }
}
