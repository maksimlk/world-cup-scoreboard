package io.github.maksimlk.worldcup.scoreboard;

import io.github.maksimlk.worldcup.scoreboard.exception.MatchNotFoundException;
import io.github.maksimlk.worldcup.scoreboard.exception.NoGoalToCancelException;
import io.github.maksimlk.worldcup.scoreboard.exception.TeamAlreadyPlayingException;
import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.model.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.model.Side;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks live football World Cup matches.
 *
 * <p>Team names are trimmed and compared ignoring case. A team can be in at most one live match.
 *
 * <p>This class is <strong>not thread-safe</strong>. Callers that share an instance across threads
 * must synchronize access themselves.
 */
public final class Scoreboard {

    /** Total score descending; ties go to the most recently started match, i.e. the higher id. */
    private static final Comparator<Match> SUMMARY_ORDER = Comparator
            .comparingInt(Match::totalScore)
            .thenComparingLong(match -> match.id().value())
            .reversed();

    private final Map<MatchId, Match> liveMatches = new HashMap<>();
    private final Set<Team> playingTeams = new HashSet<>();
    private long lastMatchId;

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
        Team home = Team.of(homeTeam, "homeTeam");
        Team away = Team.of(awayTeam, "awayTeam");
        if (home.equals(away)) {
            throw new IllegalArgumentException("A team cannot play itself: " + home);
        }
        requireNotPlaying(home);
        requireNotPlaying(away);

        MatchId id = new MatchId(++lastMatchId);
        liveMatches.put(id, new Match(id, home, away));
        playingTeams.add(home);
        playingTeams.add(away);
        return id;
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
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(change, "change");
        liveMatch(matchId).apply(side, change);
    }

    /**
     * Finishes a live match: removes it from the scoreboard and frees both teams.
     *
     * @throws NullPointerException if the id is {@code null}
     * @throws MatchNotFoundException
     *         if the match is unknown or already finished
     */
    public void finishMatch(MatchId matchId) {
        Objects.requireNonNull(matchId, "matchId");
        Match match = liveMatch(matchId);
        liveMatches.remove(matchId);
        playingTeams.remove(match.homeTeam());
        playingTeams.remove(match.awayTeam());
    }

    /**
     * Returns the live matches ordered by total score, highest first. Matches with the same total
     * are ordered by start, most recently started first.
     *
     * @return an unmodifiable snapshot that later changes do not affect
     */
    public List<MatchSnapshot> getSummary() {
        return liveMatches.values().stream()
                .sorted(SUMMARY_ORDER)
                .map(Match::snapshot)
                .toList();
    }

    private Match liveMatch(MatchId matchId) {
        Match match = liveMatches.get(matchId);
        if (match == null) {
            throw new MatchNotFoundException(matchId);
        }
        return match;
    }

    private void requireNotPlaying(Team team) {
        if (playingTeams.contains(team)) {
            throw new TeamAlreadyPlayingException(team.name());
        }
    }
}
