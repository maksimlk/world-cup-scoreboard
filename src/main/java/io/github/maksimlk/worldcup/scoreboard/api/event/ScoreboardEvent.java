package io.github.maksimlk.worldcup.scoreboard.api.event;

import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;

/**
 * A change on the scoreboard, published to subscribers after the change has been applied.
 *
 * <p>The hierarchy is sealed, so a {@code switch} over an event is checked for exhaustiveness.
 */
public sealed interface ScoreboardEvent permits MatchStarted, ScoreChanged, MatchFinished {

    /** The match as it is after the change. */
    MatchSnapshot match();
}
