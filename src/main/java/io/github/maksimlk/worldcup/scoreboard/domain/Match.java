package io.github.maksimlk.worldcup.scoreboard.domain;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.api.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.api.Side;
import io.github.maksimlk.worldcup.scoreboard.exception.NoGoalToCancelException;
import java.util.Comparator;
import java.util.Objects;

/** The mutable state of one live match. Scores never drop below zero. */
public final class Match {

    /** Summary order: total score descending; ties go to the most recently started match, i.e. the higher id. */
    public static final Comparator<Match> SUMMARY_ORDER = Comparator
            .comparingInt(Match::totalScore)
            .thenComparingLong(match -> match.id().value())
            .reversed();

    private final MatchId id;
    private final TeamName homeTeam;
    private final TeamName awayTeam;
    private int homeScore;
    private int awayScore;

    public Match(MatchId id, TeamName homeTeam, TeamName awayTeam) {
        this.id = Objects.requireNonNull(id, "id");
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam");
    }

    public MatchId id() {
        return id;
    }

    /**
     * Applies one score event. The score is left unchanged if the event is rejected.
     *
     * @throws NullPointerException if an argument is {@code null}
     * @throws NoGoalToCancelException if a goal is cancelled for a side with a score of zero
     */
    public void apply(Side side, ScoreChange change) {
        Objects.requireNonNull(side, "side");
        int delta = Objects.requireNonNull(change, "change") == ScoreChange.GOAL ? 1 : -1;
        if (side == Side.HOME) {
            homeScore = checked(homeScore + delta, side);
        } else {
            awayScore = checked(awayScore + delta, side);
        }
    }

    /** Whether the team plays in this match, on either side. */
    public boolean involves(TeamName team) {
        return homeTeam.equals(team) || awayTeam.equals(team);
    }

    public int totalScore() {
        return homeScore + awayScore;
    }

    public MatchSnapshot snapshot() {
        return new MatchSnapshot(id, homeTeam.value(), awayTeam.value(), homeScore, awayScore);
    }

    private int checked(int score, Side side) {
        if (score < 0) {
            throw new NoGoalToCancelException(id, side);
        }
        return score;
    }
}
