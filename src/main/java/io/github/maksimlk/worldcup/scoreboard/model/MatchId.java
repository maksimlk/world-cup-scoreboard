package io.github.maksimlk.worldcup.scoreboard.model;

/**
 * Identifies a match on a {@code Scoreboard}.
 *
 * <p>Ids are issued by {@code Scoreboard.startMatch} in increasing order, so a higher value means a
 * more recently started match. An id created by hand is simply unknown to the scoreboard.
 *
 * @param value the numeric id
 */
public record MatchId(long value) {
}
