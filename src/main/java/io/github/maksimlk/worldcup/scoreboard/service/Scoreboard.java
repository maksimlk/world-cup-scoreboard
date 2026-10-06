package io.github.maksimlk.worldcup.scoreboard.service;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.api.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.api.Side;
import io.github.maksimlk.worldcup.scoreboard.api.event.MatchFinished;
import io.github.maksimlk.worldcup.scoreboard.api.event.MatchStarted;
import io.github.maksimlk.worldcup.scoreboard.api.event.ScoreChanged;
import io.github.maksimlk.worldcup.scoreboard.api.event.ScoreboardEvent;
import io.github.maksimlk.worldcup.scoreboard.api.event.Subscription;
import io.github.maksimlk.worldcup.scoreboard.exception.ListenerFailedException;
import io.github.maksimlk.worldcup.scoreboard.exception.MatchNotFoundException;
import io.github.maksimlk.worldcup.scoreboard.exception.NoGoalToCancelException;
import io.github.maksimlk.worldcup.scoreboard.exception.TeamAlreadyPlayingException;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.Match;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.TeamName;
import io.github.maksimlk.worldcup.scoreboard.internal.event.EventPublisher;
import io.github.maksimlk.worldcup.scoreboard.internal.repository.InMemoryMatchRepository;
import io.github.maksimlk.worldcup.scoreboard.internal.repository.MatchRepository;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tracks live football World Cup matches.
 *
 * <p>Team names are trimmed and compared ignoring case. A team can be in at most one live match.
 *
 * <p>This class is <strong>not thread-safe</strong>. Callers that share an instance across threads
 * must synchronize access themselves.
 */
public final class Scoreboard {

    private final MatchRepository matches;
    private final EventPublisher events = new EventPublisher();

    /** Creates an empty scoreboard that keeps matches in memory. */
    public Scoreboard() {
        this(new InMemoryMatchRepository());
    }

    /**
     * Creates a scoreboard that keeps matches in the given repository. Package-private: the
     * repository is an internal type, so this injection point is for tests, not part of the API.
     */
    Scoreboard(MatchRepository matches) {
        this.matches = Objects.requireNonNull(matches, "matches");
    }

    /**
     * Starts a new match with a 0-0 score. The match is live immediately.
     *
     * @return the id of the new match
     * @throws NullPointerException if a team name is {@code null}
     * @throws IllegalArgumentException if a team name is blank or both names denote the same team
     * @throws TeamAlreadyPlayingException if either team is already playing in a live match
     * @throws ListenerFailedException if a subscribed listener failed; the match has been started
     */
    public MatchId startMatch(String homeTeam, String awayTeam) {
        TeamName home = new TeamName(homeTeam);
        TeamName away = new TeamName(awayTeam);
        if (home.equals(away)) {
            throw new IllegalArgumentException("A team cannot play itself: " + home.value());
        }
        for (TeamName team : List.of(home, away)) {
            if (matches.findByTeam(team).isPresent()) {
                throw new TeamAlreadyPlayingException(team.value());
            }
        }
        Match match = new Match(matches.nextId(), home, away);
        matches.save(match);
        events.publish(new MatchStarted(match.snapshot()));
        return match.id();
    }

    /**
     * Applies one score event to one side of a live match.
     *
     * @throws NullPointerException if any argument is {@code null}
     * @throws MatchNotFoundException if the match is unknown or already finished
     * @throws NoGoalToCancelException if a goal is cancelled for a side with a score of zero
     * @throws ListenerFailedException if a subscribed listener failed; the score has been updated
     */
    public void updateScore(MatchId matchId, Side side, ScoreChange change) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(change, "change");
        Match match = liveMatch(matchId);
        match.apply(side, change);
        matches.save(match);
        events.publish(new ScoreChanged(match.snapshot(), side, change));
    }

    /**
     * Finishes a live match: removes it from the scoreboard and frees both teams.
     *
     * @throws NullPointerException if the id is {@code null}
     * @throws MatchNotFoundException if the match is unknown or already finished
     * @throws ListenerFailedException if a subscribed listener failed; the match has been finished
     */
    public void finishMatch(MatchId matchId) {
        Match match = liveMatch(matchId);
        matches.remove(matchId);
        events.publish(new MatchFinished(match.snapshot()));
    }

    /**
     * Returns the live matches ordered by total score, highest first. Matches with the same total
     * are ordered by start, most recently started first.
     *
     * @return an unmodifiable snapshot that later changes do not affect
     */
    public List<MatchSnapshot> getSummary() {
        return matches.findAll().stream()
                .sorted(Match.SUMMARY_ORDER)
                .map(Match::snapshot)
                .toList();
    }

    /**
     * Subscribes a listener to changes on the scoreboard. The listener receives a
     * {@link ScoreboardEvent} after each successful start, score update and finish, synchronously
     * and in subscription order. Only changes made after subscribing are delivered; call
     * {@link #getSummary()} first for the current state. Rejected calls publish nothing.
     *
     * <p>If listeners throw, every listener still receives the event and the operation then throws
     * {@link ListenerFailedException}; the change itself has been applied.
     *
     * @return the subscription, used to stop receiving events
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription subscribe(Consumer<ScoreboardEvent> listener) {
        return events.subscribe(listener);
    }

    private Match liveMatch(MatchId matchId) {
        Objects.requireNonNull(matchId, "matchId");
        return matches.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    }
}
