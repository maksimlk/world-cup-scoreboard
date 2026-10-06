package io.github.maksimlk.worldcup.scoreboard;

import io.github.maksimlk.worldcup.scoreboard.exception.MatchNotFoundException;
import io.github.maksimlk.worldcup.scoreboard.exception.NoGoalToCancelException;
import io.github.maksimlk.worldcup.scoreboard.exception.TeamAlreadyPlayingException;
import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.model.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.model.Side;
import java.util.List;

/**
 * Tracks live football World Cup matches.
 *
 * <p>Team names are trimmed and compared ignoring case. A team can be in at most one live match.
 *
 * <p>This class is <strong>not thread-safe</strong>. Callers that share an instance across threads
 * must synchronize access themselves.
 */
public final class Scoreboard {

    /** Creates an empty scoreboard. */
    public Scoreboard() {
    }

    /**
     * Starts a new match with a 0-0 score. The match is live immediately.
     *
     * @return the id of the new match
     * @throws NullPointerException if a team name is {@code null}
     * @throws IllegalArgumentException if a team name is blank or both names denote the same team
     * @throws TeamAlreadyPlayingException
     *         if either team is already playing in a live match
     */
    public MatchId startMatch(String homeTeam, String awayTeam) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Applies one score event to one side of a live match.
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws MatchNotFoundException
     *         if the match is unknown or already finished
     * @throws NoGoalToCancelException
     *         if a goal is cancelled for a side with a score of zero
     */
    public void updateScore(MatchId matchId, Side side, ScoreChange change) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Finishes a live match: removes it from the scoreboard and frees both teams.
     *
     * @throws NullPointerException if the id is {@code null}
     * @throws MatchNotFoundException
     *         if the match is unknown or already finished
     */
    public void finishMatch(MatchId matchId) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Returns the live matches ordered by total score, highest first. Matches with the same total
     * are ordered by start, most recently started first.
     *
     * @return an unmodifiable snapshot that later changes do not affect
     */
    public List<MatchSnapshot> getSummary() {
        throw new UnsupportedOperationException("not implemented");
    }
}
