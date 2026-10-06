package io.github.maksimlk.worldcup.scoreboard;

import io.github.maksimlk.worldcup.scoreboard.exception.NoGoalToCancelException;
import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.model.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.model.Side;

/** The mutable state of one live match. Scores never drop below zero. */
final class Match {

    private final MatchId id;
    private final Team homeTeam;
    private final Team awayTeam;
    private int homeScore;
    private int awayScore;

    Match(MatchId id, Team homeTeam, Team awayTeam) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    MatchId id() {
        return id;
    }

    Team homeTeam() {
        return homeTeam;
    }

    Team awayTeam() {
        return awayTeam;
    }

    /**
     * Applies one score event. The score is left unchanged if the event is rejected.
     *
     * @throws NoGoalToCancelException if a goal is cancelled for a side with a score of zero
     */
    void apply(Side side, ScoreChange change) {
        int newScore = scoreOf(side) + switch (change) {
            case GOAL -> 1;
            case GOAL_CANCELLED -> -1;
        };
        if (newScore < 0) {
            throw new NoGoalToCancelException(id, side);
        }
        switch (side) {
            case HOME -> homeScore = newScore;
            case AWAY -> awayScore = newScore;
        }
    }

    int totalScore() {
        return homeScore + awayScore;
    }

    private int scoreOf(Side side) {
        return switch (side) {
            case HOME -> homeScore;
            case AWAY -> awayScore;
        };
    }

    MatchSnapshot snapshot() {
        return new MatchSnapshot(id, homeTeam.name(), awayTeam.name(), homeScore, awayScore);
    }
}
