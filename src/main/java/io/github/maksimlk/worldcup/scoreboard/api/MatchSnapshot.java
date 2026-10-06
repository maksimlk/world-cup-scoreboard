package io.github.maksimlk.worldcup.scoreboard.api;

import java.util.Objects;

/**
 * An immutable view of a live match at the moment it was taken.
 *
 * @param id        the match id
 * @param homeTeam  the home team name
 * @param awayTeam  the away team name
 * @param homeScore the home team score, never negative
 * @param awayScore the away team score, never negative
 */
public record MatchSnapshot(MatchId id, String homeTeam, String awayTeam, int homeScore, int awayScore) {

    public MatchSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(homeTeam, "homeTeam");
        Objects.requireNonNull(awayTeam, "awayTeam");
        requireNonNegative(homeScore, "homeScore");
        requireNonNegative(awayScore, "awayScore");
    }

    private static void requireNonNegative(int score, String name) {
        if (score < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + score);
        }
    }
}
